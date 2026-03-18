package com.contractGuard.LegalLens.model.enums;


public enum OverallAssessment {
    VERY_FAVORABLE,
    FAVORABLE,
    NEUTRAL,
    UNFAVORABLE,
    VERY_UNFAVORABLE;

    public static OverallAssessment fromString(String value) {
        try {
            return OverallAssessment.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEUTRAL;
        }
    }
}