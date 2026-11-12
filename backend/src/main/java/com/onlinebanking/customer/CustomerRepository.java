package com.onlinebanking.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Customer> findByGuardianToken(String token);

    Optional<Customer> findByUnsubscribeToken(String token);
}
