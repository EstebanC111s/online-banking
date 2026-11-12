package com.onlinebanking.consent;

import com.onlinebanking.customer.Customer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class ConsentService {

    private final ConsentRepository consents;

    public ConsentService(ConsentRepository consents) {
        this.consents = consents;
    }

    public record ConsentState(ConsentType type, String description, boolean granted, Instant updatedAt, ConsentSource source) {
    }

    @Transactional(readOnly = true)
    public boolean isGranted(Long customerId, ConsentType type) {
        return consents.findFirstByCustomerIdAndTypeOrderByCreatedAtDescIdDesc(customerId, type)
                .map(ConsentRecord::isGranted)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<ConsentState> current(Long customerId) {
        return Arrays.stream(ConsentType.values())
                .map(type -> consents.findFirstByCustomerIdAndTypeOrderByCreatedAtDescIdDesc(customerId, type)
                        .map(r -> new ConsentState(type, type.getDescription(), r.isGranted(), r.getCreatedAt(), r.getSource()))
                        .orElse(new ConsentState(type, type.getDescription(), false, null, null)))
                .toList();
    }

    @Transactional
    public void set(Customer customer, ConsentType type, boolean granted, ConsentSource source) {
        boolean currentlyGranted = isGranted(customer.getId(), type);
        boolean neverRecorded = consents.findFirstByCustomerIdAndTypeOrderByCreatedAtDescIdDesc(customer.getId(), type).isEmpty();
        if (neverRecorded || currentlyGranted != granted) {
            consents.save(new ConsentRecord(customer, type, granted, source));
        }
    }

    @Transactional(readOnly = true)
    public List<ConsentRecord> history(Long customerId) {
        return consents.findByCustomerIdOrderByCreatedAtDescIdDesc(customerId);
    }
}
