package com.contractGuard.LegalLens.model.entity;


import com.contractGuard.LegalLens.model.enums.ChangeImpact;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "clause_changes", indexes = {
        @Index(name = "idx_contract_change", columnList = "contract_id, clause_number"),
        @Index(name = "idx_change_type", columnList = "change_type"),
        @Index(name = "idx_change_impact", columnList = "impact")
})
@Data
public class ClauseChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clause_id")
    private ClauseEntity clause;

    private Integer clauseNumber;
    private String clauseType;

    @Column(columnDefinition = "TEXT")
    private String oldText;

    @Column(columnDefinition = "TEXT")
    private String newText;

    private String changeType; // ADDED, MODIFIED, DELETED, UNCHANGED

    @Enumerated(EnumType.STRING)
    private ChangeImpact impact; // IMPROVEMENT, REGRESSION, NEUTRAL

    private String suggestionSource; // Which AI suggestion was applied
    private Boolean userAcceptedSuggestion = false;

    private Double oldRiskScore;
    private Double newRiskScore;

    @Column(columnDefinition = "TEXT")
    private String improvementSummary;
}
