package com.contractGuard.LegalLens.service;


import com.contractGuard.LegalLens.model.dto.ContractAnalysisResponse;
import com.contractGuard.LegalLens.model.dto.ContractStatusResponse;
import com.contractGuard.LegalLens.model.dto.ContractUploadResponse;
import com.contractGuard.LegalLens.model.entity.ContractEntity;
import com.contractGuard.LegalLens.model.entity.PartyProfileEntity;
import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import com.contractGuard.LegalLens.model.enums.PartyRole;
import com.contractGuard.LegalLens.exception.AnalysisNotReadyException;
import com.contractGuard.LegalLens.exception.ContractNotFoundException;
import com.contractGuard.LegalLens.exception.DuplicateContractException;
import com.contractGuard.LegalLens.repository.ContractRepository;
import com.contractGuard.LegalLens.repository.PartyProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractService {

    private final ContractAnalysisWorker contractAnalysisWorker;
    private final ContractRepository contractRepository;
    private final PartyProfileRepository partyProfileRepository;

    @Transactional
    public ContractUploadResponse uploadContract(MultipartFile file,
                                                 String partyName,
                                                 String partyRole,
                                                 String jurisdiction,
                                                 String parentContractUuid) throws IOException {
        log.info("Upload request - file={}, party={}", file.getOriginalFilename(), partyName);

        // Step 1: Validate
        validateUpload(file, partyName);

        // Step 2: Compute SHA-256 and reject exact duplicates
        String fileHash = computeHash(file.getBytes());
        contractRepository.findByFileHash(fileHash).ifPresent(existing -> {
            throw new DuplicateContractException(
                    "This file was already uploaded. Contract UUID: " + existing.getContractUuid()
            );
        });

        ContractEntity parentContract = findParentContract(parentContractUuid);

        // Step 3: Find or create party profile
        PartyProfileEntity profile = findOrCreateProfile(partyName, partyRole, jurisdiction);

        // Step 4: Persist contract with UPLOADING status
        ContractEntity contract = new ContractEntity();
        contract.setOriginalFilename(file.getOriginalFilename());
        contract.setFileSize(file.getSize());
        contract.setContentType(file.getContentType());
        contract.setFileHash(fileHash);
        contract.setPartyProfile(profile);
        contract.setAnalysisStatus(AnalysisStatus.UPLOADING);
        contract.setProgressPercentage(0);
        if (parentContract != null) {
            parentContract.setIsLatestVersion(false);
            contractRepository.save(parentContract);
            contract.setParentContract(parentContract);
            contract.setVersion(parentContract.getVersion() + 1);
        }
        contract.setIsLatestVersion(true);

        ContractEntity saved = contractRepository.save(contract);

        // Step 5: Trigger background analysis AFTER commit so the worker
        // can safely reload the persisted row (avoids READ COMMITTED race).
        byte[] fileBytes = file.getBytes();
        Long profileId = profile.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                contractAnalysisWorker.analyzeContractAsync(saved.getId(), fileBytes, profileId);
            }
        });

        // Step 6: Return 202 immediately
        log.info("Contract saved id={}, uuid={} — analysis queued", saved.getId(), saved.getContractUuid());
        return new ContractUploadResponse(
                saved.getContractUuid(),
                parentContract != null ? parentContract.getContractUuid() : null,
                saved.getVersion(),
                saved.getIsLatestVersion(),
                AnalysisStatus.UPLOADING,
                "Contract uploaded successfully. Analysis in progress."
        );
    }

    // ─── STATUS POLLING ────────────────────────────────────────────────────────

    /**
     * Cache strategy for status:
     *
     * Only terminal states (COMPLETED, FAILED) are cached.
     * In-progress states (UPLOADING, PARSING, ANALYZING, etc.) always hit the DB
     * so polling clients always see live progress.
     *
     * This prevents a 30-minute stale cache window if the worker fails silently
     * and the status never transitions out of an in-progress state.
     *
     * Terminal states are safe to cache because ContractStatusService evicts
     * both caches on every status transition — but once COMPLETED or FAILED,
     * no further transitions occur, so the cached value stays correct.
     */
    @Cacheable(
            value = "contractStatus",
            key = "#contractUuid",
            unless = "!#result.analysisStatus.name().equals('COMPLETED')"
                    + " && !#result.analysisStatus.name().equals('FAILED')"
    )
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

    /**
     * Cache strategy for result:
     *
     * Only caches when analysis is COMPLETED. If the method throws
     * AnalysisNotReadyException (status != COMPLETED), Spring Cache never
     * sees a return value, so nothing is cached — no NPE risk.
     *
     * Using `unless` with an explicit null guard and enum name comparison
     * (rather than != 'COMPLETED') avoids fragile double-negative logic
     * and is safe if result is ever null due to a framework edge case.
     */
    @Cacheable(
            value = "contractResult",
            key = "#contractUuid",
            unless = "#result == null"
                    + " || !#result.analysisStatus.name().equals('COMPLETED')"
    )
    @Transactional
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

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    private void validateUpload(MultipartFile file, String partyName) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Contract file is empty");
        if (partyName == null || partyName.isBlank())
            throw new IllegalArgumentException("Party name is required");
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

    private ContractEntity findParentContract(String parentContractUuid) {
        if (parentContractUuid == null || parentContractUuid.isBlank()) {
            return null;
        }
        return contractRepository.findByContractUuid(parentContractUuid)
                .orElseThrow(() -> new ContractNotFoundException(parentContractUuid));
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
