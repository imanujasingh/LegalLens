package com.contractGuard.LegalLens.service;


import com.contractGuard.LegalLens.model.dto.ContractAnalysisResponse;
import com.contractGuard.LegalLens.model.dto.RiskEvaluationResult;
import com.contractGuard.LegalLens.model.entity.PartyProfileEntity;
import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import com.contractGuard.LegalLens.model.enums.PartyRole;
import com.contractGuard.LegalLens.service.ai.AIService;
import com.contractGuard.LegalLens.service.parser.DocumentParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractService {

    private final DocumentParserService documentParserService;
    private final AIService aiService;
    private final RiskEvaluationService riskEvaluationService;
    private final ContractResponseBuilder responseBuilder;

    public ContractAnalysisResponse processContractUpload(MultipartFile file,String partyName, String contractName) throws IOException {
        log.info("Processing contract upload for party={}, file={}", partyName, file.getOriginalFilename());
        //Step 1: Parse the document
        String ParsedDocument=documentParserService.parseDocument(file);

        //Build Party Profile (In real scenario, fetch from DB)
        PartyProfileEntity partyProfile=new PartyProfileEntity();
        partyProfile.setPartyName(partyName);
        partyProfile.setPartyRole(PartyRole.CLIENT);
        partyProfile.setJurisdiction("India");

        //Step 2: Analyze the contract (Placeholder for actual AI analysis)
        String result= aiService.analyzeContract(ParsedDocument, partyProfile);
        log.info("AI Analysis Result: {}", result);

        //Step3: Risk Analysis
        RiskEvaluationResult riskResult=riskEvaluationService.evaluateRisk(result);
        return buildResponse(resolveContractName(contractName, file), file.getOriginalFilename(), ParsedDocument,result, riskResult);
    }

    private void validate(MultipartFile file, String partyName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Contract file is required");
        }
        if (partyName == null || partyName.isBlank()) {
            throw new IllegalArgumentException("Party name is required");
        }
    }

    private String resolveContractName(String contractName, MultipartFile file) {
        return (contractName != null && !contractName.isBlank())
                ? contractName
                : file.getOriginalFilename();
    }


    public static ContractAnalysisResponse buildResponse(String contractName, String filename, String contractText, String aiResult, RiskEvaluationResult riskResult) {

        ContractAnalysisResponse response = new ContractAnalysisResponse();

        int clauses = contractText.split("\n\\s*\\d+\\.").length - 1;

        response.setContractUuid(UUID.randomUUID().toString());
        response.setFilename(filename);
        response.setAnalysisDate(LocalDateTime.now());
        response.setAnalysisStatus(AnalysisStatus.COMPLETED);

        response.setTotalClauses(clauses);
        response.setAnalyzedClauses(clauses);

        response.setOverallRisk(riskResult.getRiskLevel());
        response.setRiskScore(riskResult.getRiskScore());
        response.setRedFlags(riskResult.getRedFlags());
        response.setGreenFlags(riskResult.getGreenFlags());

        // concise summary = clause bullets only
        if (aiResult.contains("Clause-level risk summary:")) {
            response.setSummary(
                    aiResult.substring(
                            aiResult.indexOf("Clause-level risk summary:")
                    ).trim()
            );
        } else {
            response.setSummary(aiResult);
        }

        response.setRiskDistribution(
                Map.of(response.getOverallRisk().name(), clauses)
        );

        return response;
    }


}
