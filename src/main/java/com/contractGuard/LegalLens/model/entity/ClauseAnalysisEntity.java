package com.contractGuard.LegalLens.model.entity;


import com.contractGuard.LegalLens.model.enums.OverallAssessment;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "clause_analyses", indexes = {
        @Index(name = "idx_clause_analysis", columnList = "clause_id"),
        @Index(name = "idx_risk_score", columnList = "risk_score"),
        @Index(name = "idx_assessment", columnList = "overall_assessment")
})
@Data
public class ClauseAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clause_id", nullable = false)
    @ToString.Exclude
    private ClauseEntity clause;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_profile_id", nullable = false)
    @ToString.Exclude
    private PartyProfileEntity partyProfile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> benefits;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> risks;

    @ElementCollection
    @CollectionTable(name = "clause_red_flags")
    @Column(name = "red_flag")
    private List<String> redFlags = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> suggestions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> questions;

    private Double riskScore;

    @Enumerated(EnumType.STRING)
    private OverallAssessment overallAssessment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> rawAnalysis;

    @Column(name = "ai_model_used")
    private String aiModelUsed;

    private Integer tokenCount;
    private Double aiCost;

    @CreationTimestamp
    private LocalDateTime analyzedAt;
}