package com.contractGuard.LegalLens.model.enums;


public enum AnalysisStatus {
    // Upload & Initial Processing
    UPLOADED("Uploaded", "Contract file uploaded successfully"),
    PARSING("Parsing", "Parsing document content"),
    PARSED("Parsed", "Document parsed successfully"),

    // Segmentation
    SEGMENTING("Segmenting", "Identifying clauses in document"),
    SEGMENTED("Segmented", "Document segmented into clauses"),

    // Analysis Phase
    ANALYZING("Analyzing", "AI analysis in progress"),
    CLAUSE_ANALYSIS_COMPLETE("Clause Analysis Complete", "All clauses analyzed"),

    // Completion
    COMPLETED("Completed", "Analysis completed successfully"),
    FAILED("Failed", "Analysis failed"),

    // User Interaction
    UNDER_REVIEW("Under Review", "User reviewing analysis"),
    NEGOTIATING("Negotiating", "Contract under negotiation"),
    AMENDED("Amended", "Contract amended after negotiation"),
    SIGNED("Signed", "Contract signed"),
    ARCHIVED("Archived", "Contract archived"),

    // Partial States
    PARTIALLY_ANALYZED("Partially Analyzed", "Some clauses analyzed, some failed");

    private final String displayName;
    private final String description;

    AnalysisStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    // Helper methods for status flow
    public boolean isProcessing() {
        return this == PARSING ||
                this == SEGMENTING ||
                this == ANALYZING;
    }

    public boolean isCompleted() {
        return this == COMPLETED ||
                this == SIGNED ||
                this == ARCHIVED;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public boolean canRetry() {
        return this == FAILED ||
                this == PARTIALLY_ANALYZED;
    }
}