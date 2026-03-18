package com.contractGuard.LegalLens.model.dto;


import lombok.Data;

import java.util.List;

@Data
public class ComparisonResultDTO {

    private Double similarityScore;
    private Integer totalChanges;
    private Double riskImprovement;

    private List<ClauseChangeDTO> clauseChanges;
    private List<String> improvements;
    private List<String> regressions;

    private Double suggestionAdoptionRate;
    private Integer suggestionsAdopted;
    private Integer suggestionsRejected;

    @Data
    public static class ClauseChangeDTO {
        private Integer clauseNumber;
        private String clauseType;
        private String changeType;
        private String oldText;
        private String newText;
        private String impact;
        private Double riskChange;
        private Boolean suggestionApplied;
        private String suggestionSource;
    }
}
