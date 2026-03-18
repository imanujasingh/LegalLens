package com.contractGuard.LegalLens.model.entity;


import com.contractGuard.LegalLens.model.enums.NegotiationStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "negotiation_tracking", indexes = {
        @Index(name = "idx_contract_negotiation", columnList = "contract_id, round_number"),
        @Index(name = "idx_negotiation_status", columnList = "status")
})
@Data
public class NegotiationTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    private Integer roundNumber = 1;

    @Enumerated(EnumType.STRING)
    private NegotiationStatus status = NegotiationStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> changesRequested = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> changesAccepted = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> changesRejected = new HashMap<>();

    @Column(columnDefinition = "TEXT")
    private String counterpartyComments;

    @Column(columnDefinition = "TEXT")
    private String ourResponse;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String counterpartyEmail;
    private String sentBy;

    // Helper method to generate negotiation summary
    public String generateSummary() {
        return String.format("Round %d - %s: %d requested, %d accepted, %d rejected",
                roundNumber, status,
                changesRequested.size(),
                changesAccepted.size(),
                changesRejected.size());
    }
}
