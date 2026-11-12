package com.onlinebanking.privacy;

import com.onlinebanking.account.*;
import com.onlinebanking.account.AccountDtos.TransactionView;
import com.onlinebanking.auth.AuthService;
import com.onlinebanking.common.BusinessException;
import com.onlinebanking.common.Money;
import com.onlinebanking.consent.ConsentRecord;
import com.onlinebanking.consent.ConsentRepository;
import com.onlinebanking.consent.ConsentService;
import com.onlinebanking.customer.Customer;
import com.onlinebanking.customer.CustomerRepository;
import com.onlinebanking.dispute.DisputeDtos.DisputeView;
import com.onlinebanking.dispute.DisputeRepository;
import com.onlinebanking.dispute.DisputeStatus;
import com.onlinebanking.dispute.DisputeService;
import com.onlinebanking.notification.OutboxRepository;
import com.onlinebanking.publicinfo.LegalDocuments;
import com.onlinebanking.review.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrivacyService {

    private final AccountRepository accounts;
    private final BankTransactionRepository transactions;
    private final ConsentService consentService;
    private final ConsentRepository consentRepository;
    private final DisputeService disputeService;
    private final DisputeRepository disputeRepository;
    private final ReviewRepository reviews;
    private final OutboxRepository outbox;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customers;

    public PrivacyService(AccountRepository accounts, BankTransactionRepository transactions, ConsentService consentService,
                          ConsentRepository consentRepository, DisputeService disputeService, DisputeRepository disputeRepository,
                          ReviewRepository reviews, OutboxRepository outbox, PasswordEncoder passwordEncoder,
                          CustomerRepository customers) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.consentService = consentService;
        this.consentRepository = consentRepository;
        this.disputeService = disputeService;
        this.disputeRepository = disputeRepository;
        this.reviews = reviews;
        this.outbox = outbox;
        this.passwordEncoder = passwordEncoder;
        this.customers = customers;
    }

    // el Customer que llega del controller esta detached (open-in-view = false)
    private Customer managed(Customer c) {
        return customers.findById(c.getId()).orElseThrow();
    }

    public record MeView(Long id, String fullName, String email, LocalDate birthDate, String status, String role,
                         String termsVersion, Instant termsAcceptedAt, String privacyVersion, Instant privacyAcceptedAt,
                         String currentTermsVersion, String currentPrivacyVersion, boolean needsLegalAcceptance) {
    }

    public MeView me(Customer c) {
        boolean needs = !LegalDocuments.TERMS_VERSION.equals(c.getTermsVersion())
                || !LegalDocuments.PRIVACY_VERSION.equals(c.getPrivacyVersion());
        return new MeView(c.getId(), c.getFullName(), c.getEmail(), c.getBirthDate(), c.getStatus().name(), c.getRole().name(),
                c.getTermsVersion(), c.getTermsAcceptedAt(), c.getPrivacyVersion(), c.getPrivacyAcceptedAt(),
                LegalDocuments.TERMS_VERSION, LegalDocuments.PRIVACY_VERSION, needs);
    }

    @Transactional
    public MeView acceptLegal(Customer detached, String termsVersion, String privacyVersion) {
        Customer c = managed(detached);
        if (!LegalDocuments.TERMS_VERSION.equals(termsVersion) || !LegalDocuments.PRIVACY_VERSION.equals(privacyVersion)) {
            throw BusinessException.badRequest("OUTDATED_VERSION", "Estás aceptando una versión que ya no es la vigente. Recarga la página.");
        }
        c.acceptLegal(termsVersion, privacyVersion);
        return me(c);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(Customer c) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("generatedAt", Instant.now());
        data.put("note", "Estos son todos los datos personales que Banco Ceiba Demo guarda sobre ti.");
        data.put("profile", me(c));
        data.put("consents", consentService.current(c.getId()));
        data.put("consentHistory", consentService.history(c.getId()).stream()
                .map(r -> Map.of("type", r.getType(), "granted", r.isGranted(), "source", r.getSource(), "at", r.getCreatedAt()))
                .toList());
        data.put("accounts", accounts.findByOwnerIdOrderByIdAsc(c.getId()).stream()
                .map(a -> Map.of("number", a.getNumber(), "type", a.getType(), "status", a.getStatus(),
                        "balance", a.getBalance(), "openedAt", a.getOpenedAt()))
                .toList());
        data.put("transactions", transactions.findByAccountOwnerIdOrderByCreatedAtDescIdDesc(c.getId()).stream()
                .map(TransactionView::of).toList());
        data.put("disputes", disputeService.mine(c.getId()));
        reviews.findByAuthorId(c.getId()).ifPresent(r ->
                data.put("review", Map.of("rating", r.getRating(), "comment", r.getComment(), "createdAt", r.getCreatedAt())));
        if (c.getGuardianEmail() != null) {
            data.put("guardianEmail", c.getGuardianEmail());
        }
        return data;
    }

    @Transactional
    public void deleteMe(Customer detached, String password) {
        Customer c = managed(detached);
        if (password == null || !passwordEncoder.matches(password, c.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "BAD_PASSWORD", "La contraseña no es correcta");
        }
        List<Account> mine = accounts.findByOwnerIdOrderByIdAsc(c.getId());
        List<Account> withMoney = mine.stream().filter(a -> a.isOpen() && a.getBalance().signum() != 0).toList();
        if (!withMoney.isEmpty()) {
            String detail = withMoney.stream().map(a -> a.getNumber() + " (" + Money.format(a.getBalance()) + ")")
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw BusinessException.conflict("BALANCE_NOT_ZERO",
                    "Antes de borrar tus datos, deja en cero estas cuentas: " + detail + ". Puedes transferir el dinero a otra persona.");
        }
        boolean openDisputes = disputeRepository.findByCustomerIdOrderByCreatedAtDesc(c.getId()).stream()
                .anyMatch(d -> d.getStatus() == DisputeStatus.OPEN);
        if (openDisputes) {
            throw BusinessException.conflict("OPEN_DISPUTES",
                    "Tienes disputas en revisión. Espera la respuesta (así podemos devolverte el dinero si procede).");
        }

        for (Account a : mine) {
            if (a.isOpen()) {
                accounts.findByIdForUpdate(a.getId()).ifPresent(locked -> locked.close(AccountStatus.CLOSED));
            }
        }
        consentRepository.deleteByCustomerId(c.getId());
        reviews.findByAuthorId(c.getId()).ifPresent(reviews::delete);
        outbox.deleteByRecipientIgnoreCase(c.getEmail());
        c.anonymize(passwordEncoder.encode(AuthService.randomToken()), AuthService.randomToken());
    }

    @Transactional(readOnly = true)
    public List<ConsentRecord> rawHistory(Long customerId) {
        return consentService.history(customerId);
    }

    @Transactional(readOnly = true)
    public List<DisputeView> disputes(Long customerId) {
        return disputeService.mine(customerId);
    }
}
