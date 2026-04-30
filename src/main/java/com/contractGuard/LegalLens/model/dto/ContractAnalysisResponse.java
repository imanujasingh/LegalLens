package com.contractGuard.LegalLens.model.dto;


import com.contractGuard.LegalLens.model.entity.ContractEntity;
import com.contractGuard.LegalLens.model.entity.ClauseAnalysisEntity;
import com.contractGuard.LegalLens.model.entity.ClauseChange;
import com.contractGuard.LegalLens.model.entity.ClauseEntity;
import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import com.contractGuard.LegalLens.model.enums.RiskLevel;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class ContractAnalysisResponse {

    private Long contractId;
    private String contractUuid;
    private String parentContractUuid;
    private String filename;
    private LocalDateTime analysisDate;
    private AnalysisStatus analysisStatus;
    private RiskLevel overallRisk;
    private Double riskScore;
    private Integer totalClauses;
    private Integer analyzedClauses;
    private Integer version;
    private Boolean isLatestVersion;

    private Map<String, Integer> riskDistribution;
    private List<ClauseAnalysisDTO> clauseAnalyses;
    private List<String> redFlags;
    private List<String> greenFlags;

    private NegotiationStrategyDTO negotiationStrategy;
    private String summary;
    private String changeSummary;
    private Double improvementScore; // For updates

    private ContractComparisonDTO comparison; // If this is an update
    private List<SuggestionAdoptionDTO> suggestionsAdopted;

    public static ContractAnalysisResponse from(ContractEntity contract) {
        ContractAnalysisResponse response = new ContractAnalysisResponse();
        response.setContractId(contract.getId());
        response.setContractUuid(contract.getContractUuid());
        response.setParentContractUuid(contract.getParentContract() != null
                ? contract.getParentContract().getContractUuid()
                : null);
        response.setFilename(contract.getOriginalFilename());
        response.setAnalysisDate(contract.getUpdatedAt());
        response.setAnalysisStatus(contract.getAnalysisStatus());
        response.setRiskScore(contract.getRiskScore());
        response.setOverallRisk(resolveRiskLevel(contract.getRiskScore()));
        response.setTotalClauses(contract.getClauses() != null ? contract.getClauses().size() : 0);
        response.setAnalyzedClauses((int) contract.getClauses().stream()
                .filter(clause -> clause.getAnalysis() != null)
                .count());
        response.setVersion(contract.getVersion());
        response.setIsLatestVersion(contract.getIsLatestVersion());
        response.setRiskDistribution(buildRiskDistribution(response.getOverallRisk()));
        response.setClauseAnalyses(buildClauseAnalyses(contract));
        response.setRedFlags(buildRedFlags(contract));
        response.setGreenFlags(Collections.emptyList());
        response.setSummary(contract.getAiSummary());
        response.setChangeSummary(contract.getChangeSummary());
        response.setComparison(buildComparison(contract));
        response.setSuggestionsAdopted(Collections.emptyList());
        return response;
    }

    private static RiskLevel resolveRiskLevel(Double riskScore) {
        return riskScore == null ? RiskLevel.NONE : RiskLevel.fromScore(riskScore);
    }

    private static Map<String, Integer> buildRiskDistribution(RiskLevel riskLevel) {
        return Map.of(riskLevel.name(), 1);
    }

    private static List<ClauseAnalysisDTO> buildClauseAnalyses(ContractEntity contract) {
        if (contract.getClauses() == null || contract.getClauses().isEmpty()) {
            return Collections.emptyList();
        }
        return contract.getClauses().stream()
                .sorted((left, right) -> left.getClauseNumber().compareTo(right.getClauseNumber()))
                .map(ContractAnalysisResponse::toClauseAnalysis)
                .toList();
    }

    private static ClauseAnalysisDTO toClauseAnalysis(ClauseEntity clause) {
        ClauseAnalysisDTO dto = new ClauseAnalysisDTO();
        dto.setClauseId(clause.getId());
        dto.setClauseNumber(clause.getClauseNumber());
        dto.setClauseType(clause.getClauseType() != null ? clause.getClauseType().name() : null);
        dto.setOriginalText(clause.getOriginalText());

        ClauseAnalysisEntity analysis = clause.getAnalysis();
        if (analysis != null) {
            dto.setRiskLevel(resolveRiskLevel(analysis.getRiskScore()));
            dto.setRiskScore(analysis.getRiskScore());
            dto.setBenefits(Collections.emptyList());
            dto.setRisks(Collections.emptyList());
            dto.setSuggestions(Collections.emptyList());
            Object summary = analysis.getRawAnalysis() != null ? analysis.getRawAnalysis().get("summary") : null;
            dto.setAnalysis(summary != null ? summary.toString() : null);
        }
        return dto;
    }

    private static List<String> buildRedFlags(ContractEntity contract) {
        if (contract.getClauses() == null) {
            return Collections.emptyList();
        }
        return contract.getClauses().stream()
                .map(ClauseEntity::getAnalysis)
                .filter(Objects::nonNull)
                .flatMap(analysis -> analysis.getRedFlags().stream())
                .distinct()
                .toList();
    }

    private static ContractComparisonDTO buildComparison(ContractEntity contract) {
        if (contract.getParentContract() == null || contract.getClauseChanges() == null) {
            return null;
        }

        ContractComparisonDTO comparison = new ContractComparisonDTO();
        comparison.setClausesChanged((int) contract.getClauseChanges().stream()
                .filter(change -> !"UNCHANGED".equals(change.getChangeType()))
                .count());
        comparison.setSuggestionsAdopted(0);
        comparison.setRiskImprovement(null);
        comparison.setImprovementSummary(contract.getChangeSummary());
        comparison.setChanges(contract.getClauseChanges().stream()
                .map(ContractAnalysisResponse::toClauseChange)
                .toList());
        return comparison;
    }

    private static ClauseChangeDTO toClauseChange(ClauseChange change) {
        ClauseChangeDTO dto = new ClauseChangeDTO();
        dto.setClauseNumber(change.getClauseNumber());
        dto.setClauseType(change.getClauseType());
        dto.setChangeType(change.getChangeType());
        dto.setImpact(change.getImpact() != null ? change.getImpact().name() : null);
        dto.setOldText(change.getOldText());
        dto.setNewText(change.getNewText());
        dto.setSuggestionApplied(change.getUserAcceptedSuggestion());
        dto.setRiskChange(null);
        return dto;
    }

    @Data
    public static class ClauseAnalysisDTO {
        private Long clauseId;
        private Integer clauseNumber;
        private String clauseType;
        private String originalText;
        private RiskLevel riskLevel;
        private List<String> benefits;
        private List<RiskDTO> risks;
        private List<SuggestionDTO> suggestions;
        private String analysis;
        private Double riskScore;
    }

    @Data
    public static class RiskDTO {
        private String description;
        private RiskLevel severity;
        private String impact;
        private String mitigation;
        private Double estimatedCost;
    }

    @Data
    public static class SuggestionDTO {
        private String originalText;
        private String suggestedText;
        private String rationale;
        private String priority;
    }

    @Data
    public static class NegotiationStrategyDTO {
        private List<String> mustChange;
        private List<String> shouldChange;
        private List<String> couldChange;
        private List<String> talkingPoints;
        private Map<String, String> alternativeClauses;
    }

    @Data
    public static class ContractComparisonDTO {
        private Double riskImprovement;
        private Integer clausesChanged;
        private Integer suggestionsAdopted;
        private List<ClauseChangeDTO> changes;
        private String improvementSummary;
    }

    @Data
    public static class ClauseChangeDTO {
        private Integer clauseNumber;
        private String clauseType;
        private String changeType;
        private String impact;
        private String oldText;
        private String newText;
        private Boolean suggestionApplied;
        private Double riskChange;
    }

    @Data
    public static class SuggestionAdoptionDTO {
        private String suggestionHash;
        private String suggestionText;
        private String status;
        private String adoptedInVersion;
        private LocalDateTime adoptedAt;
    }

    @Data
    public static class NegotiationHistoryDTO {
        private Integer clauseNumber;
        private String clauseType;
        private String originalText;
        private String negotiatedText;
        private String outcome;
        private Double riskBefore;
        private Double riskAfter;
        private LocalDateTime negotiatedAt;
    }
}
