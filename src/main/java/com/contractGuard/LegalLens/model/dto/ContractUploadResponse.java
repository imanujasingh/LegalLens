package com.contractGuard.LegalLens.model.dto;

import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Data
public class ContractUploadResponse {
    String contractUuid;
    AnalysisStatus analysisStatus;
    String message;

}
