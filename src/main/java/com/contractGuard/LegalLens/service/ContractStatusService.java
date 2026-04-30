package com.contractGuard.LegalLens.service;


import com.contractGuard.LegalLens.model.entity.ContractEntity;
import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import com.contractGuard.LegalLens.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractStatusService {
    private final ContractRepository contractRepository;

    public void updateStatus(ContractEntity contract, AnalysisStatus status, int progress) {
        contract.setAnalysisStatus(status);
        contract.setProgressPercentage(progress);
        contractRepository.save(contract);

        log.debug("contractId={} -> status={}, progress={}%",
                contract.getId(), status, progress);
    }
}
