package com.contractGuard.LegalLens.service;

import com.contractGuard.LegalLens.model.dto.ContractAnalysisResponse;
import com.contractGuard.LegalLens.model.dto.RiskEvaluationResult;
import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service

public class ContractResponseBuilder {

    public ContractAnalysisResponse buildResponse(String contractName, String filename, String aiResult, RiskEvaluationResult riskResult) {

        ContractAnalysisResponse response = new ContractAnalysisResponse();

        response.setContractUuid(UUID.randomUUID().toString());
        response.setFilename(filename);
        response.setAnalysisDate(LocalDateTime.now());
        response.setAnalysisStatus(AnalysisStatus.COMPLETED);

        response.setOverallRisk(riskResult.getRiskLevel());
        response.setRiskScore(riskResult.getRiskScore());
        response.setSummary("AI-generated summary placeholder");

        return response;
    }
}
