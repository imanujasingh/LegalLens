package com.contractGuard.LegalLens.service;


import com.contractGuard.LegalLens.model.dto.ContractAnalysisResponse;
import com.contractGuard.LegalLens.model.dto.ContractStatusResponse;
import com.contractGuard.LegalLens.model.dto.ContractUploadResponse;
import com.contractGuard.LegalLens.model.dto.RiskEvaluationResult;
import com.contractGuard.LegalLens.model.entity.ContractEntity;
import com.contractGuard.LegalLens.model.entity.PartyProfileEntity;
import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import com.contractGuard.LegalLens.model.enums.PartyRole;
import com.contractGuard.LegalLens.repository.ContractRepository;
import com.contractGuard.LegalLens.repository.PartyProfileRepository;
import com.contractGuard.LegalLens.service.ai.AIService;
import com.contractGuard.LegalLens.service.parser.DocumentParserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.security.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractService {

    private final DocumentParserService documentParserService;
    private final AIService aiService;
    private final RiskEvaluationService riskEvaluationService;
    private final ContractRepository contractRepository;
    private final PartyProfileRepository partyProfileRepository;

    @Transactional
    public ContractUploadResponse uploadContract(MultipartFile file, String partyName, String partyRole, String jurisdiction) throws IOException {
        log.info("Upload request - file={}, party={}", file.getOriginalFilename(), partyName);
        //Step 1: Validate
        validateUpload(file,partyName);

        //Step 2: Compute SHA-256
        String fileHash=computeHash(file.getBytes());
        contractRepository.findByFileHash(fileHash).ifPresent(existing -> {
            throw new DuplicateContractException(
                    "This file was already uploaded. Contract UUID: "+existing.getContractUuid()
            );
        });

        //Step 3: Find or create party profile
        PartyProfileEntity profile=findOrCreateProfile(partyName, partyRole,jurisdiction);

        // 4. Save contract with UPLOADING status
        ContractEntity contract = new ContractEntity();
        contract.setOriginalFilename(file.getOriginalFilename());
        contract.setFileSize(file.getSize());
        contract.setContentType(file.getContentType());
        contract.setFileHash(fileHash);
        contract.setPartyProfile(profile);
        contract.setAnalysisStatus(AnalysisStatus.UPLOADING);
        contract.setProgressPercentage(0);

        ContractEntity saved = contractRepository.save(contract);

        // 5. Trigger background analysis — non-blocking
        analyzeContractAsync(saved.getId(), file.getBytes(), profile);

        // 6. Return 202 immediately
        log.info("Contract saved id={}, uuid={} — analysis queued",
                saved.getId(), saved.getContractUuid());
        return new ContractUploadResponse(
                saved.getContractUuid(),
                AnalysisStatus.UPLOADING,
                "Contract uploaded successfully. Analysis in progress."
        );

    }

    // ─── BACKGROUND THREAD — does the heavy lifting ───────────────────────────
    @Async("contractAnalysisExecutor")
    @Transactional
    public void analyzeContractAsync(Long contractUuid, byte[] fileBytes, PartyProfileEntity profile) throws IOException {
        log.info("background analysis for contract={} started", contractUuid);
        ContractEntity contract = contractRepository.findById(contractUuid).
                orElseThrow(() -> new IllegalStateException("Contract not found"));

        try{
            updateStatus(contract,AnalysisStatus.PARSING,10);
            String parsedText=documentParserService.parseBytes(
                    fileBytes,
                    contract.getOriginalFilename(),
                    contract.getContentType()
            );
            contract.setParsedText(parsedText);
            contractRepository.save(contract);

            // Step 2: AI analysis
            updateStatus(contract, AnalysisStatus.ANALYZING, 40);
            String aiResult = aiService.analyzeContract(parsedText, profile);

            // Step 3: Risk evaluation
            updateStatus(contract, AnalysisStatus.ANALYZING, 80);
            var riskResult = riskEvaluationService.evaluateRisk(aiResult);

            // Step 4: Save results and mark COMPLETED
            contract.setRiskScore(riskResult.getRiskScore());
            contract.setAiSummary(aiResult);
            updateStatus(contract, AnalysisStatus.COMPLETED, 100);
            contractRepository.save(contract);

            log.info("Analysis completed for contractId={}, risk={}",
                    contractId, riskResult.getRiskLevel());

        } catch (Exception e) {
            log.error("Analysis failed for contractId={}: {}", contractId, e.getMessage(), e);
            contract.setFailureReason(e.getMessage());
            updateStatus(contract, AnalysisStatus.FAILED, 0);
            contractRepository.save(contract);
        }

    }

    // ─── STATUS POLLING ────────────────────────────────────────────────────────

    public ContractStatusResponse getStatus(String contractUuid) {
        ContractEntity contract = contractRepository.findByContractUuid(contractUuid)
                .orElseThrow(() -> new ContractNotFoundException(contractUuid));

        return new ContractStatusResponse(
                contract.getContractUuid(),
                contract.getAnalysisStatus(),
                contract.getProgressPercentage(),
                contract.getFailureReason()
        );
    }

    public ContractAnalysisResponse getResult(String contractUuid) {
        ContractEntity contract = contractRepository.findByContractUuid(contractUuid)
                .orElseThrow(() -> new ContractNotFoundException(contractUuid));

        if (contract.getAnalysisStatus() != AnalysisStatus.COMPLETED) {
            throw new AnalysisNotReadyException(
                    "Analysis not complete. Current status: " + contract.getAnalysisStatus()
            );
        }

        return ContractAnalysisResponse.from(contract);
    }
    private void validateUpload(MultipartFile file,String partyName){
        if(file==null || file.isEmpty())
            throw new IllegalArgumentException("Contract File is empty");
        if(partyName==null || partyName.isEmpty())
            throw new IllegalArgumentException("Party Name is empty");
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    private void updateStatus(ContractEntity contract, AnalysisStatus status, int progress) {
        contract.setAnalysisStatus(status);
        contract.setProgressPercentage(progress);
        contractRepository.save(contract);
        log.debug("contractId={} → status={}, progress={}%",
                contract.getId(), status, progress);
    }

    private PartyProfileEntity findOrCreateProfile(String partyName,
                                                   String partyRole,
                                                   String jurisdiction) {
        return partyProfileRepository.findByPartyName(partyName)
                .orElseGet(() -> {
                    PartyProfileEntity profile = new PartyProfileEntity();
                    profile.setPartyName(partyName);
                    profile.setPartyRole(PartyRole.fromString(partyRole));
                    profile.setJurisdiction(jurisdiction);
                    return partyProfileRepository.save(profile);
                });
    }

    private String computeHash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}


}
