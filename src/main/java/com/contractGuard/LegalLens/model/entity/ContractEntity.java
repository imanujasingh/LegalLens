package com.contractGuard.LegalLens.model.entity;

import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contracts", indexes = {
        @Index(name = "idx_contract_uuid", columnList = "contract_uuid"),
        @Index(name = "idx_user_status", columnList = "uploaded_by, analysis_status"),
        @Index(name = "idx_parent_contract", columnList = "parent_contract_id")
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ContractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(name = "contract_uuid", nullable = false, unique = true)
    private String contractUuid = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String originalFilename;

    private String filePath;
    private Long fileSize;
    private String contentType;
    private String fileHash; // For duplicate detection

    @Column(columnDefinition = "TEXT")
    private String parsedText;

    @Column(name = "uploaded_by")
    private String uploadedBy = "anonymous"; // Will be user email when auth added

    @CreationTimestamp
    private LocalDateTime uploadDate;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private AnalysisStatus analysisStatus = AnalysisStatus.UPLOADED;

    private String failureReason;
    private Integer progressPercentage = 0;
    private Double riskScore;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "is_latest_version", nullable = false)
    private Boolean isLatestVersion = true;

    @Column(columnDefinition = "TEXT")
    private String changeSummary;

    // Version chain
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_contract_id")
    @ToString.Exclude
    private ContractEntity parentContract;

    @OneToMany(mappedBy = "parentContract", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<ContractEntity> childVersions = new ArrayList<>();

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_profile_id")
    private PartyProfileEntity partyProfile;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ClauseEntity> clauses = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<ContractStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<ClauseChange> clauseChanges = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<NegotiationTracking> negotiationTracks = new ArrayList<>();

    // Helper methods
    public void updateStatus(AnalysisStatus newStatus, String reason) {
        this.analysisStatus = newStatus;
        this.failureReason = reason;

        ContractStatusHistory history = new ContractStatusHistory();
        history.setContract(this);
        history.setStatus(newStatus);
        history.setReason(reason);
        history.setChangedAt(LocalDateTime.now());
        this.statusHistory.add(history);
    }

    public void updateProgress(Integer percentage) {
        this.progressPercentage = Math.min(Math.max(percentage, 0), 100);
    }

    public boolean canRetry() {
        return this.analysisStatus.canRetry();
    }
}

