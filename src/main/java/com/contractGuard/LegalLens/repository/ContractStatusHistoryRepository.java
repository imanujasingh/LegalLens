package com.contractGuard.LegalLens.repository;


import com.contractGuard.LegalLens.model.entity.ContractStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractStatusHistoryRepository extends JpaRepository<ContractStatusHistory, Long> {

    List<ContractStatusHistory> findByContractId(Long contractId);

    List<ContractStatusHistory> findByContractIdOrderByChangedAtDesc(Long contractId);
}