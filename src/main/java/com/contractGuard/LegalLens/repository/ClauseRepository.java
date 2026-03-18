package com.contractGuard.LegalLens.repository;


import com.contractGuard.LegalLens.model.entity.ClauseEntity;
import com.contractGuard.LegalLens.model.enums.ClauseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClauseRepository extends JpaRepository<ClauseEntity, Long> {

    List<ClauseEntity> findByContractId(Long contractId);

    List<ClauseEntity> findByContractIdAndClauseType(Long contractId, ClauseType clauseType);

    @Query("SELECT c FROM ClauseEntity c WHERE c.contract.id = :contractId ORDER BY c.clauseNumber")
    List<ClauseEntity> findByContractIdOrdered(@Param("contractId") Long contractId);

    @Query("SELECT c.clauseType, COUNT(c) FROM ClauseEntity c WHERE c.contract.id = :contractId GROUP BY c.clauseType")
    List<Object[]> countByClauseType(@Param("contractId") Long contractId);
}