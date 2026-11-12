package com.onlinebanking.dispute;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    List<Dispute> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Dispute> findByStatusOrderByDueDateAsc(DisputeStatus status);

    boolean existsByTransactionIdAndStatus(Long transactionId, DisputeStatus status);

    boolean existsByTransactionIdAndStatusIn(Long transactionId, List<DisputeStatus> statuses);

    List<Dispute> findByStatusNot(DisputeStatus status);
}
