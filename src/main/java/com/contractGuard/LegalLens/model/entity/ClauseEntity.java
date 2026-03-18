package com.contractGuard.LegalLens.model.entity;


import com.contractGuard.LegalLens.model.enums.ClauseType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "clauses", indexes = {
        @Index(name = "idx_contract_clause", columnList = "contract_id, clause_number"),
        @Index(name = "idx_clause_type", columnList = "clause_type")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClauseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    @ToString.Exclude
    private ContractEntity contract;

    @Column(nullable = false)
    private Integer clauseNumber;

    @Enumerated(EnumType.STRING)
    private ClauseType clauseType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalText;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    private String textHash; // For change detection

    private Integer pageNumber;
    private Integer startPosition;
    private Integer endPosition;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "clause", cascade = CascadeType.ALL, orphanRemoval = true)
    private ClauseAnalysisEntity analysis;

    @OneToOne(mappedBy = "clause", cascade = CascadeType.ALL)
    private ClauseChange clauseChange;

    // Helper method to set analysis
    public void setAnalysis(ClauseAnalysisEntity analysis) {
        if (analysis == null) {
            if (this.analysis != null) {
                this.analysis.setClause(null);
            }
        } else {
            analysis.setClause(this);
        }
        this.analysis = analysis;
    }
}