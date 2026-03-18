package com.contractGuard.LegalLens.repository;


import com.contractGuard.LegalLens.model.entity.NegotiationTracking;
import com.contractGuard.LegalLens.model.enums.NegotiationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NegotiationTrackingRepository extends JpaRepository<NegotiationTracking, Long> {

    List<NegotiationTracking> findByContractId(Long contractId);

    List<NegotiationTracking> findByContractIdOrderByRoundNumberDesc(Long contractId);

    Optional<NegotiationTracking> findByContractIdAndRoundNumber(Long contractId, Integer roundNumber);

    List<NegotiationTracking> findByStatus(NegotiationStatus status);
}
