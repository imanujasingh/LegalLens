package com.contractGuard.LegalLens.controller;

import com.contractGuard.LegalLens.model.dto.ContractAnalysisResponse;
import com.contractGuard.LegalLens.model.dto.ContractUploadDTO;
import com.contractGuard.LegalLens.model.dto.ContractAnalysisResponse.ClauseChangeDTO;
import com.contractGuard.LegalLens.service.ContractService;
import com.contractGuard.LegalLens.service.parser.DocumentParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Contract Management", description = "APIs for contract upload, analysis, and management")
public class ContractController {

    private final ContractService contractService;
    private final DocumentParserService documentParserService;

    @PostMapping("/upload")
    @Operation(summary = "Upload and analyze a contract")
    public ResponseEntity<ContractAnalysisResponse> uploadAndAnalyzeContract(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value="partyName", required=true) String partyName,
            @RequestParam(value = "contractName", required = false) String contractName) throws IOException {

        log.info("Contract upload request: {}, party: {}",
                file.getOriginalFilename(), partyName);

        ContractAnalysisResponse response = contractService.processContractUpload(file, partyName, contractName);
        log.info("Contract upload done: {}, analysis status: {}", response.getFilename(), response.getAnalysisStatus());
        return ResponseEntity.ok(response);
    }

//    @PostMapping("/analyze")
//    @Operation(summary = "Analyze contract with custom parameters")
//    public ResponseEntity<ContractAnalysisResponse> analyzeContract(
//            @Valid @RequestBody ContractUploadDTO uploadDTO) {
//
//        log.info("Contract analysis request: {}", uploadDTO.getContractName());
//
//        ContractAnalysisResponse response = contractService.analyzeContract(uploadDTO);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/{contractId}")
//    @Operation(summary = "Get contract by ID")
//    public ResponseEntity<ContractAnalysisResponse> getContract(
//            @PathVariable Long contractId) {
//
//        ContractAnalysisResponse contract = contractService.getContractAnalysis(contractId);
//        return ResponseEntity.ok(contract);
//    }
//
//    @GetMapping("/{contractId}/changes")
//    @Operation(summary = "Get all clause changes for a contract")
//    public ResponseEntity<List<ClauseChangeDTO>> getContractChanges(
//            @PathVariable Long contractId) {
//
//        List<ClauseChangeDTO> changes = contractService.getClauseChanges(contractId);
//        return ResponseEntity.ok(changes);
//    }
//
//    @DeleteMapping("/{contractId}")
//    @Operation(summary = "Delete a contract")
//    public ResponseEntity<Void> deleteContract(@PathVariable Long contractId) {
//
//        log.info("Deleting contract: {}", contractId);
//        contractService.deleteContract(contractId);
//        return ResponseEntity.noContent().build();
//    }
//
//    @GetMapping("/party/{partyId}")
//    @Operation(summary = "Get all contracts for a party")
//    public ResponseEntity<List<ContractAnalysisResponse>> getContractsByParty(
//            @PathVariable Long partyId) {
//
//        List<ContractAnalysisResponse> contracts = contractService.getContractsByParty(partyId);
//        return ResponseEntity.ok(contracts);
//    }
//
//    @PostMapping("/{contractId}/compare")
//    @Operation(summary = "Compare with another contract version")
//    public ResponseEntity<ContractAnalysisResponse> compareContract(
//            @PathVariable Long contractId,
//            @RequestParam("comparisonFile") MultipartFile comparisonFile) {
//
//        ContractAnalysisResponse comparison = contractService.compareWithVersion(
//                contractId, comparisonFile);
//        return ResponseEntity.ok(comparison);
//    }
}