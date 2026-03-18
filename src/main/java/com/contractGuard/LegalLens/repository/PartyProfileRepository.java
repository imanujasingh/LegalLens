package com.contractGuard.LegalLens.repository;


import com.contractGuard.LegalLens.model.entity.PartyProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartyProfileRepository extends JpaRepository<PartyProfileEntity, Long> {

    Optional<PartyProfileEntity> findByPartyName(String partyName);

    Optional<PartyProfileEntity> findByPartyNameAndPartyRole(String partyName, String partyRole);
}