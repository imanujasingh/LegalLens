package com.contractGuard.LegalLens.model.entity;



import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "contract_status_history", indexes = {
        @Index(name = "idx_contract_status", columnList = "contract_id, status"),
        @Index(name = "idx_changed_at", columnList = "changed_at")
})
@Data
public class ContractStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    private LocalDateTime changedAt;

    private String changedBy = "system";
}