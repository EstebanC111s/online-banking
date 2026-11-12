package com.onlinebanking.transfer;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TransferQuoteRepository extends JpaRepository<TransferQuote, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from TransferQuote q where q.id = :id")
    Optional<TransferQuote> findByIdForUpdate(String id);
}
