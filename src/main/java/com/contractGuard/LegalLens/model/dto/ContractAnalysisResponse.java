package com.contractGuard.LegalLens.model.dto;


import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import com.contractGuard.LegalLens.model.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ContractAnalysisResponse {

    private Long contractId;
    private String contractUuid;
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
    private Double improvementScore; // For updates

    private ContractComparisonDTO comparison; // If this is an update
    private List<SuggestionAdoptionDTO> suggestionsAdopted;

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