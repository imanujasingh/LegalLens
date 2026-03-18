package com.contractGuard.LegalLens.model.entity;


import com.contractGuard.LegalLens.model.entity.ClauseChange;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ComparisonResult {
    private Double similarityScore;
    private Integer totalChanges;
    private Double riskImprovement;

    private List<ClauseChange> clauseChanges = new ArrayList<>();
    private List<String> improvements = new ArrayList<>();
    private List<String> regressions = new ArrayList<>();

    private Double suggestionAdoptionRate;
    private Integer suggestionsAdopted;
    private Integer suggestionsRejected;

    // Helper methods
    public boolean hasChanges() {
        return totalChanges != null && totalChanges > 0;
    }

    public boolean isImproved() {
        return riskImprovement != null && riskImprovement > 0;
    }

    public Double getImprovementPercentage() {
        if (riskImprovement == null || riskImprovement == 0) return 0.0;
        // Assuming risk scores are 0-10 scale
        return (riskImprovement / 10.0) * 100;
    }
}
