package com.onlinebanking.admin;

import com.onlinebanking.customer.Customer;
import com.onlinebanking.customer.CustomerRepository;
import com.onlinebanking.notification.Mailer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CustomerRepository customers;
    private final Mailer mailer;

    public AdminController(CustomerRepository customers, Mailer mailer) {
        this.customers = customers;
        this.mailer = mailer;
    }

    public record NewsletterRequest(@NotBlank @Size(max = 200) String subject, @NotBlank @Size(max = 3000) String body) {
    }

    public record NewsletterResult(int recipients, int skippedWithoutConsent) {
    }

    @PostMapping("/newsletter")
    @Transactional
    public NewsletterResult newsletter(@Valid @RequestBody NewsletterRequest req) {
        int sent = 0;
        int skipped = 0;
        for (Customer c : customers.findAll()) {
            if (mailer.sendMarketing(c, req.subject(), req.body())) {
                sent++;
            } else {
                skipped++;
            }
        }
        return new NewsletterResult(sent, skipped);
    }
}
