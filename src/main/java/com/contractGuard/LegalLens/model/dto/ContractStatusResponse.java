package com.contractGuard.LegalLens.model.dto;

import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContractStatusResponse {
    String contractUuid;
    AnalysisStatus analysisStatus;
    Integer progressPercentage;
    String failureReason;
}
