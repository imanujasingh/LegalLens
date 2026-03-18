package com.contractGuard.LegalLens.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ContractUploadDTO {

    @NotNull(message = "File is required")
    private MultipartFile file;

    @NotBlank(message = "Party name is required")
    private String partyName;

    @NotBlank(message = "Party role is required")
    private String partyRole;

    private String industry;
    private String jurisdiction;

    private Boolean isUpdate = false;
    private Long parentContractId;
    private String changeSummary;

    private String contractName;
}