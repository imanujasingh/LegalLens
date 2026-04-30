package com.contractGuard.LegalLens.repository;

import com.contractGuard.LegalLens.model.entity.ClauseAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClauseAnalysisRepository extends JpaRepository<ClauseAnalysisEntity, Long> {

    List<ClauseAnalysisEntity> findByClauseContractId(Long contractId);

    List<ClauseAnalysisEntity> findByPartyProfileId(Long partyProfileId);

    Optional<ClauseAnalysisEntity> findByClauseId(Long clauseId);

    @Query("SELECT ca FROM ClauseAnalysisEntity ca WHERE ca.clause.contract.id = :contractId ORDER BY ca.clause.clauseNumber")
    List<ClauseAnalysisEntity> findByContractIdOrdered(@Param("contractId") Long contractId);

    @Query("SELECT AVG(ca.riskScore) FROM ClauseAnalysisEntity ca WHERE ca.clause.contract.id = :contractId")
    Double findAverageRiskScoreByContractId(@Param("contractId") Long contractId);

    @Query("SELECT COUNT(ca) FROM ClauseAnalysisEntity ca WHERE ca.clause.contract.id = :contractId AND ca.riskScore > :threshold")
    Long countHighRiskClauses(@Param("contractId") Long contractId, @Param("threshold") Double threshold);
}
