package com.onlinebanking.notification;

import com.onlinebanking.config.AppProperties;
import com.onlinebanking.consent.ConsentService;
import com.onlinebanking.consent.ConsentType;
import com.onlinebanking.customer.Customer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Mailer {

    private final OutboxRepository outbox;
    private final ConsentService consents;
    private final AppProperties props;

    public Mailer(OutboxRepository outbox, ConsentService consents, AppProperties props) {
        this.outbox = outbox;
        this.consents = consents;
        this.props = props;
    }

    @Transactional
    public void sendTransactional(String to, String subject, String body) {
        outbox.save(new OutboxMessage(to, subject, body, MessageKind.TRANSACTIONAL));
    }

    @Transactional
    public boolean sendMarketing(Customer customer, String subject, String body) {
        if (!customer.isActive() || !consents.isGranted(customer.getId(), ConsentType.MARKETING)) {
            return false;
        }
        String withFooter = body
                + "\n\n---\nRecibes este correo porque aceptaste novedades de Banco Ceiba Demo."
                + "\nDarte de baja (un clic, sin iniciar sesión): " + unsubscribeLink(customer);
        outbox.save(new OutboxMessage(customer.getEmail(), subject, withFooter, MessageKind.MARKETING));
        return true;
    }

    public String unsubscribeLink(Customer customer) {
        return props.frontendUrl() + "/baja?token=" + customer.getUnsubscribeToken();
    }

    public String guardianLink(String token) {
        return props.frontendUrl() + "/tutor?token=" + token;
    }
}
