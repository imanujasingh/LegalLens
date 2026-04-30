package com.contractGuard.LegalLens.repository;


import com.contractGuard.LegalLens.model.entity.ContractEntity;
import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<ContractEntity, Long> {

    Optional<ContractEntity> findByContractUuid(String contractUuid);

    List<ContractEntity> findByUploadedByOrderByUploadDateDesc(String uploadedBy);

    List<ContractEntity> findByAnalysisStatus(AnalysisStatus status);

    List<ContractEntity> findByParentContractId(Long parentContractId);

    Optional<ContractEntity> findFirstByContractUuidAndIsLatestVersionTrue(String contractUuid);

    List<ContractEntity> findByContractUuidOrderByVersionDesc(String contractUuid);

    @Query("SELECT c FROM ContractEntity c WHERE c.uploadDate < :cutoffDate AND c.analysisStatus = :status")
    List<ContractEntity> findOldContractsByStatus(
            @Param("cutoffDate") LocalDateTime cutoffDate,
            @Param("status") AnalysisStatus status
    );

    @Query("SELECT COUNT(c) FROM ContractEntity c WHERE c.uploadedBy = :uploadedBy")
    Long countByUploadedBy(@Param("uploadedBy") String uploadedBy);

    @Query("SELECT AVG(c.riskScore) FROM ContractEntity c WHERE c.uploadedBy = :uploadedBy AND c.riskScore IS NOT NULL")
    Double findAverageRiskScoreByUploadedBy(@Param("uploadedBy") String uploadedBy);

    Optional<Object> findByFileHash(String fileHash);
}