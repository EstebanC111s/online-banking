package com.onlinebanking.config;

import com.onlinebanking.auth.AuthService;
import com.onlinebanking.customer.Customer;
import com.onlinebanking.customer.CustomerRepository;
import com.onlinebanking.customer.Role;
import com.onlinebanking.publicinfo.LegalDocuments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public DataSeeder(CustomerRepository customers, PasswordEncoder passwordEncoder, AppProperties props) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (customers.existsByEmailIgnoreCase(props.adminEmail())) {
            return;
        }
        Customer admin = new Customer("Administrador", props.adminEmail(), passwordEncoder.encode(props.adminPassword()),
                LocalDate.of(1990, 1, 1), Role.ADMIN);
        admin.acceptLegal(LegalDocuments.TERMS_VERSION, LegalDocuments.PRIVACY_VERSION);
        admin.setUnsubscribeToken(AuthService.randomToken());
        customers.save(admin);
        log.info("Usuario administrador creado: {}", props.adminEmail());
    }
}
