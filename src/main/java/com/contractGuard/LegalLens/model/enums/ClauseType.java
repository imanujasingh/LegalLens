package com.contractGuard.LegalLens.model.enums;


import lombok.Getter;

@Getter
public enum ClauseType {
    DEFINITIONS("Definitions"),
    INDEMNIFICATION("Indemnification"),
    LIMITATION_OF_LIABILITY("Limitation of Liability"),
    TERMINATION("Termination"),
    CONFIDENTIALITY("Confidentiality"),
    GOVERNING_LAW("Governing Law"),
    PAYMENT_TERMS("Payment Terms"),
    WARRANTIES("Warranties"),
    INTELLECTUAL_PROPERTY("Intellectual Property"),
    FORCE_MAJEURE("Force Majeure"),
    DISPUTE_RESOLUTION("Dispute Resolution"),
    DATA_PRIVACY("Data Privacy"),
    SERVICE_LEVEL_AGREEMENT("Service Level Agreement"),
    SCOPE_OF_WORK("Scope of Work"),
    PRICING("Pricing"),
    DELIVERY("Delivery"),
    ACCEPTANCE("Acceptance"),
    INSURANCE("Insurance"),
    ASSIGNMENT("Assignment"),
    NOTICES("Notices"),
    SEVERABILITY("Severability"),
    ENTIRE_AGREEMENT("Entire Agreement"),
    AMENDMENT("Amendment"),
    WAIVER("Waiver"),
    RELATIONSHIP_OF_PARTIES("Relationship of Parties"),
    SURVIVAL("Survival"),
    COUNTERPARTS("Counterparts"),
    HEADINGS("Headings"),
    GENERAL("General");

    private final String displayName;

    ClauseType(String displayName) {
        this.displayName = displayName;
    }
}