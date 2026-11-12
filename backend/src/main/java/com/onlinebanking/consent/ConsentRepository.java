package com.onlinebanking.consent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConsentRepository extends JpaRepository<ConsentRecord, Long> {

    Optional<ConsentRecord> findFirstByCustomerIdAndTypeOrderByCreatedAtDescIdDesc(Long customerId, ConsentType type);

    List<ConsentRecord> findByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

    @Modifying
    @Query("delete from ConsentRecord c where c.customer.id = :customerId")
    void deleteByCustomerId(Long customerId);
}
