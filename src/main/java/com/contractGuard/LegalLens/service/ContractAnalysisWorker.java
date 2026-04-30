package com.contractGuard.LegalLens.service;

import com.contractGuard.LegalLens.model.entity.ContractEntity;
import com.contractGuard.LegalLens.model.entity.PartyProfileEntity;
import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import com.contractGuard.LegalLens.repository.ContractRepository;
import com.contractGuard.LegalLens.repository.PartyProfileRepository;
import com.contractGuard.LegalLens.service.ai.AIService;
import com.contractGuard.LegalLens.service.parser.DocumentParserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractAnalysisWorker {

    private final DocumentParserService documentParserService;
    private final AIService aiService;
    private final RiskEvaluationService riskEvaluationService;
    private final ContractRepository contractRepository;
    private final PartyProfileRepository partyProfileRepository;
    private final ContractStatusService contractStatusService;

    @Async("contractAnalysisExecutor")
    @Transactional
    public void analyzeContractAsync(Long contractId, byte[] fileBytes, Long partyProfileId) throws IOException {
        log.info("Background analysis for contract={} started", contractId);

        ContractEntity contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalStateException("Contract not found"));

        PartyProfileEntity profile = partyProfileRepository.findById(partyProfileId)
                .orElseThrow(() -> new IllegalStateException("Party profile not found"));

        try {
            contractStatusService.updateStatus(contract, AnalysisStatus.PARSING, 10);

            String parsedText = documentParserService.parseBytes(
                    fileBytes,
                    contract.getOriginalFilename(),
                    contract.getContentType()
            );

            contract.setParsedText(parsedText);
            contractRepository.save(contract);

            contractStatusService.updateStatus(contract, AnalysisStatus.ANALYZING, 40);

            String aiResult = aiService.analyzeContract(parsedText, profile);

            contractStatusService.updateStatus(contract, AnalysisStatus.ANALYZING, 80);

            var riskResult = riskEvaluationService.evaluateRisk(aiResult);

            contract.setRiskScore(riskResult.getRiskScore());
            contract.setAiSummary(aiResult);

            contractStatusService.updateStatus(contract, AnalysisStatus.COMPLETED, 100);

            log.info("Analysis completed for contractId={}, risk={}",
                    contractId, riskResult.getRiskLevel());

        } catch (Exception e) {
            log.error("Analysis failed for contractId={}: {}", contractId, e.getMessage(), e);

            contract.setFailureReason(e.getMessage());
            contractStatusService.updateStatus(contract, AnalysisStatus.FAILED, 0);
        }
    }
}
