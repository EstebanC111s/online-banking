package com.onlinebanking.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    List<OutboxMessage> findTop50ByOrderByIdDesc();

    List<OutboxMessage> findByRecipientIgnoreCaseOrderByIdDesc(String recipient);

    long deleteByRecipientIgnoreCase(String recipient);
}
