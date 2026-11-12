package com.onlinebanking.account;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId, Pageable pageable);

    List<BankTransaction> findByAccountOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);

    Optional<BankTransaction> findByIdAndAccountOwnerId(Long id, Long ownerId);

    boolean existsByAccountOwnerId(Long ownerId);

    long countByAccountId(Long accountId);
}
