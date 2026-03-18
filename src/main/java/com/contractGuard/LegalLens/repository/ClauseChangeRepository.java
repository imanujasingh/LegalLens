package com.contractGuard.LegalLens.repository;


import com.contractGuard.LegalLens.model.entity.ClauseChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClauseChangeRepository extends JpaRepository<ClauseChange, Long> {

    List<ClauseChange> findByContractId(Long contractId);

    List<ClauseChange> findByContractIdAndChangeType(Long contractId, String changeType);

    List<ClauseChange> findByContractIdAndUserAcceptedSuggestionTrue(Long contractId);
}
