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

    /**
     * Returns the latest version in a contract chain identified by the root UUID.
     * A "chain" shares the same root contractUuid (the original upload's UUID),
     * so we walk parent links to find the head where isLatestVersion = true.
     *
     * For single-version contracts this is equivalent to findByContractUuid.
     * For revised contracts, use the ROOT contractUuid to get the active version.
     */
    @Query("""
            SELECT c FROM ContractEntity c
            WHERE c.isLatestVersion = true
              AND (
                c.contractUuid = :rootUuid
                OR c.parentContract.contractUuid = :rootUuid
                OR c.parentContract.parentContract.contractUuid = :rootUuid
              )
            ORDER BY c.version DESC
            LIMIT 1
            """)
    Optional<ContractEntity> findLatestVersionByRootUuid(@Param("rootUuid") String rootUuid);

    /**
     * Returns full version history for a contract chain, newest first.
     * Pass the root contractUuid (first uploaded version).
     */
    @Query("""
            SELECT c FROM ContractEntity c
            WHERE c.contractUuid = :rootUuid
               OR c.parentContract.contractUuid = :rootUuid
               OR c.parentContract.parentContract.contractUuid = :rootUuid
            ORDER BY c.version DESC
            """)
    List<ContractEntity> findVersionHistoryByRootUuid(@Param("rootUuid") String rootUuid);

    @Query("SELECT c FROM ContractEntity c WHERE c.uploadDate < :cutoffDate AND c.analysisStatus = :status")
    List<ContractEntity> findOldContractsByStatus(
            @Param("cutoffDate") LocalDateTime cutoffDate,
            @Param("status") AnalysisStatus status
    );

    @Query("SELECT COUNT(c) FROM ContractEntity c WHERE c.uploadedBy = :uploadedBy")
    Long countByUploadedBy(@Param("uploadedBy") String uploadedBy);

    @Query("SELECT AVG(c.riskScore) FROM ContractEntity c WHERE c.uploadedBy = :uploadedBy AND c.riskScore IS NOT NULL")
    Double findAverageRiskScoreByUploadedBy(@Param("uploadedBy") String uploadedBy);

    Optional<ContractEntity> findByFileHash(String fileHash);
}
