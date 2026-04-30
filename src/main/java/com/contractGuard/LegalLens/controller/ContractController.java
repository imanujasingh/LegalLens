package com.contractGuard.LegalLens.controller;

import com.contractGuard.LegalLens.model.dto.ContractUploadResponse;
import com.contractGuard.LegalLens.model.dto.ContractStatusResponse;
import com.contractGuard.LegalLens.model.dto.ContractAnalysisResponse;
import com.contractGuard.LegalLens.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Contract Management")
public class ContractController {

    private final ContractService contractService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a contract for analysis")
    public ResponseEntity<ContractUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam @NotBlank String partyName,
            @RequestParam @NotBlank String partyRole,
            @RequestParam(required = false, defaultValue = "India") String jurisdiction,
            @RequestParam(required = false) String parentContractUuid)
            throws IOException {

        ContractUploadResponse response = contractService.uploadContract(
                file, partyName, partyRole, jurisdiction, parentContractUuid
        );
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{uuid}/status")
    @Operation(summary = "Poll analysis status")
    public ResponseEntity<ContractStatusResponse> getStatus(
            @PathVariable String uuid) {

        return ResponseEntity.ok(contractService.getStatus(uuid));
    }

    @GetMapping("/{uuid}/result")
    @Operation(summary = "Get full analysis result")
    public ResponseEntity<ContractAnalysisResponse> getResult(
            @PathVariable String uuid) {

        return ResponseEntity.ok(contractService.getResult(uuid));
    }
}
