package com.contractGuard.LegalLens.model.dto;

import com.contractGuard.LegalLens.model.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskEvaluationResult {

    /**
     * Final evaluated risk level (NONE, LOW, MEDIUM, HIGH, CRITICAL)
     */
    private RiskLevel riskLevel;

    /**
     * Numeric risk score on a 0–10 scale
     */
    private Double riskScore;

    /**
     * Negative findings that increase risk
     * (e.g. "Uncapped liability exposure")
     */
    private List<String> redFlags;

    /**
     * Positive findings that mitigate risk
     * (e.g. "Mutual obligations present")
     */
    private List<String> greenFlags;
}
