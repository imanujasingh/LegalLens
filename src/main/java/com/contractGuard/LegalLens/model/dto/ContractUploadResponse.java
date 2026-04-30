package com.contractGuard.LegalLens.model.dto;

import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import lombok.Data;

@Data
public class ContractUploadResponse {
    private String contractUuid;
    private String parentContractUuid;
    private Integer version;
    private Boolean isLatestVersion;
    private AnalysisStatus analysisStatus;
    private String message;

    public ContractUploadResponse(String contractUuid, AnalysisStatus analysisStatus, String message) {
        this(contractUuid, null, 1, true, analysisStatus, message);
    }

    public ContractUploadResponse(String contractUuid,
                                  String parentContractUuid,
                                  Integer version,
                                  Boolean isLatestVersion,
                                  AnalysisStatus analysisStatus,
                                  String message) {
        this.contractUuid = contractUuid;
        this.parentContractUuid = parentContractUuid;
        this.version = version;
        this.isLatestVersion = isLatestVersion;
        this.analysisStatus = analysisStatus;
        this.message = message;
    }
}
