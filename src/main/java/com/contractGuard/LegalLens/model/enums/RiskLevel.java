package com.contractGuard.LegalLens.model.enums;


import lombok.Getter;

@Getter
public enum RiskLevel {
    CRITICAL("Critical", 10, "#dc2626"),
    HIGH("High", 7, "#f97316"),
    MEDIUM("Medium", 4, "#eab308"),
    LOW("Low", 2, "#22c55e"),
    NONE("None", 0, "#6b7280");

    private final String displayName;
    private final int score;
    private final String color;

    RiskLevel(String displayName, int score, String color) {
        this.displayName = displayName;
        this.score = score;
        this.color = color;
    }

    public static RiskLevel fromScore(double score) {
        if (score >= 8.0) return CRITICAL;
        if (score >= 6.0) return HIGH;
        if (score >= 4.0) return MEDIUM;
        if (score >= 2.0) return LOW;
        return NONE;
    }
}