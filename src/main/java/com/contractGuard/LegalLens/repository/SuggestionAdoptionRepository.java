package com.contractGuard.LegalLens.repository;


import com.contractGuard.LegalLens.model.entity.SuggestionAdoption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuggestionAdoptionRepository extends JpaRepository<SuggestionAdoption, Long> {

    List<SuggestionAdoption> findByContractId(Long contractId);

    Optional<SuggestionAdoption> findByContractIdAndSuggestionHash(Long contractId, String suggestionHash);

    List<SuggestionAdoption> findByContractIdAndStatus(Long contractId, String status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(sa) FROM SuggestionAdoption sa WHERE sa.contract.id = :contractId AND sa.status = 'ACCEPTED'")
    Long countAcceptedSuggestions(@Param("contractId") Long contractId);
}