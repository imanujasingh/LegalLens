package com.contractGuard.LegalLens.model.entity;



import com.contractGuard.LegalLens.model.enums.PartyRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "party_profiles", indexes = {
        @Index(name = "idx_party_name", columnList = "party_name"),
        @Index(name = "idx_party_role", columnList = "party_role")
})
@Data
public class PartyProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String partyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyRole partyRole;

    private String industry;
    private String jurisdiction;

    @ElementCollection
    @CollectionTable(name = "party_risk_tolerance")
    @Column(name = "tolerance")
    private List<String> riskTolerance = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Integer> negotiationPriorities = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "party_deal_breakers")
    @Column(name = "deal_breaker")
    private List<String> dealBreakers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "party_preferred_terms")
    @Column(name = "preferred_term")
    private List<String> preferredTerms = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "partyProfile", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<ContractEntity> contracts = new ArrayList<>();

    @OneToMany(mappedBy = "partyProfile", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<ClauseAnalysisEntity> clauseAnalyses = new ArrayList<>();

    // Default values
    @PrePersist
    public void setDefaults() {
        if (riskTolerance.isEmpty()) {
            riskTolerance.add("MEDIUM");
        }
        if (dealBreakers.isEmpty()) {
            dealBreakers.add("unlimited liability");
            dealBreakers.add("personal guarantee");
        }
        if (preferredTerms.isEmpty()) {
            preferredTerms.add("mutual indemnification");
            preferredTerms.add("reasonable liability cap");
            preferredTerms.add("jurisdiction in home state");
        }
    }
}