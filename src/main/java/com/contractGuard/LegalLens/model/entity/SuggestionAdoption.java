package com.contractGuard.LegalLens.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "suggestion_adoptions", indexes = {
        @Index(name = "idx_suggestion_contract", columnList = "contract_id, suggestion_hash"),
        @Index(name = "idx_adoption_status", columnList = "status")
})
@Data
public class SuggestionAdoption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    private String suggestionHash;
    private String suggestionText;
    private String clauseType;

    private String adoptedInVersion; // Which contract version adopted it
    private String status; // PENDING, ACCEPTED, REJECTED, MODIFIED

    @CreationTimestamp
    private LocalDateTime suggestedAt;

    private LocalDateTime adoptedAt;

    private String adoptionNotes;

    @Column(columnDefinition = "TEXT")
    private String originalText;

    @Column(columnDefinition = "TEXT")
    private String suggestedText;
}