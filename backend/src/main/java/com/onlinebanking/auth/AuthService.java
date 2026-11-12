package com.onlinebanking.auth;

import com.onlinebanking.auth.AuthDtos.*;
import com.onlinebanking.common.BusinessDays;
import com.onlinebanking.common.BusinessException;
import com.onlinebanking.consent.ConsentService;
import com.onlinebanking.consent.ConsentSource;
import com.onlinebanking.consent.ConsentType;
import com.onlinebanking.customer.*;
import com.onlinebanking.notification.Mailer;
import com.onlinebanking.publicinfo.LegalDocuments;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Period;
import java.util.Base64;

@Service
public class AuthService {

    static final int MIN_AGE = 14;
    static final int ADULT_AGE = 18;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final LoginAttemptService attempts;
    private final ConsentService consents;
    private final Mailer mailer;

    public AuthService(CustomerRepository customers, PasswordEncoder passwordEncoder, TokenService tokens,
                       LoginAttemptService attempts, ConsentService consents, Mailer mailer) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.attempts = attempts;
        this.consents = consents;
        this.mailer = mailer;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (customers.existsByEmailIgnoreCase(email)) {
            throw BusinessException.conflict("EMAIL_TAKEN", "Ya existe una cuenta con ese correo");
        }

        int age = Period.between(req.birthDate(), BusinessDays.today()).getYears();
        if (age < MIN_AGE) {
            throw BusinessException.badRequest("TOO_YOUNG", "La edad mínima para abrir una cuenta es " + MIN_AGE + " años");
        }
        boolean minor = age < ADULT_AGE;
        if (minor && (req.guardianEmail() == null || req.guardianEmail().isBlank())) {
            throw BusinessException.badRequest("GUARDIAN_REQUIRED",
                    "Como eres menor de edad, necesitamos el correo de tu padre, madre o tutor para que autorice la cuenta");
        }
        if (minor && req.guardianEmail().trim().equalsIgnoreCase(email)) {
            throw BusinessException.badRequest("GUARDIAN_SAME_EMAIL", "El correo del tutor debe ser distinto al tuyo");
        }

        Customer c = new Customer(req.fullName().trim(), email, passwordEncoder.encode(req.password()), req.birthDate(), Role.CUSTOMER);
        c.acceptLegal(LegalDocuments.TERMS_VERSION, LegalDocuments.PRIVACY_VERSION);
        c.setUnsubscribeToken(randomToken());
        String guardianToken = null;
        if (minor) {
            guardianToken = randomToken();
            c.requireGuardian(req.guardianEmail().trim().toLowerCase(), guardianToken);
        }
        customers.save(c);

        consents.set(c, ConsentType.MARKETING, Boolean.TRUE.equals(req.marketingOptIn()), ConsentSource.REGISTRATION);
        consents.set(c, ConsentType.ANALYTICS, false, ConsentSource.REGISTRATION);

        if (minor) {
            mailer.sendTransactional(c.getGuardianEmail(), "Autoriza la cuenta de " + c.getFullName(),
                    c.getFullName() + " quiere abrir una cuenta en Banco Gocomezbo y te indicó como su padre, madre o tutor.\n"
                            + "Si estás de acuerdo, autorízala aquí: " + mailer.guardianLink(guardianToken) + "\n"
                            + "Si no reconoces esta solicitud, ignora este correo: la cuenta no podrá usarse.");
            return new RegisterResponse(c.getId(), c.getStatus().name(),
                    "Cuenta creada. Le enviamos un correo a tu tutor; podrás iniciar sesión cuando la autorice.");
        }
        mailer.sendTransactional(email, "Bienvenido a Banco Gocomezbo", "Hola " + c.getFullName() + ", tu registro quedó listo.");
        return new RegisterResponse(c.getId(), c.getStatus().name(), "Cuenta creada. Ya puedes iniciar sesión.");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        if (attempts.isLocked(email)) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "LOCKED",
                    "Demasiados intentos fallidos. Espera 15 minutos e inténtalo de nuevo.");
        }
        Customer c = customers.findByEmailIgnoreCase(email).orElse(null);
        // mismo error si no existe el correo, para no revelar quien esta registrado
        if (c == null || !passwordEncoder.matches(req.password(), c.getPasswordHash())) {
            attempts.registerFailure(email);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Correo o contraseña incorrectos");
        }
        if (c.getStatus() == CustomerStatus.PENDING_GUARDIAN) {
            throw BusinessException.forbidden("PENDING_GUARDIAN", "Tu cuenta espera la autorización de tu padre, madre o tutor");
        }
        if (!c.isActive()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Correo o contraseña incorrectos");
        }
        attempts.registerSuccess(email);
        TokenService.IssuedToken t = tokens.issue(c);
        return new LoginResponse(t.token(), t.expiresAt(), c.getFullName(), c.getRole().name());
    }

    @Transactional
    public MessageResponse approveByGuardian(String token) {
        Customer c = customers.findByGuardianToken(token)
                .orElseThrow(() -> BusinessException.badRequest("INVALID_TOKEN", "El enlace no es válido o ya fue usado"));
        c.approveByGuardian();
        mailer.sendTransactional(c.getEmail(), "Tu cuenta fue autorizada", "Tu tutor autorizó tu cuenta. Ya puedes iniciar sesión.");
        return new MessageResponse("Gracias. La cuenta de " + c.getFullName() + " quedó autorizada.");
    }

    @Transactional
    public MessageResponse unsubscribe(String token) {
        Customer c = customers.findByUnsubscribeToken(token)
                .orElseThrow(() -> BusinessException.badRequest("INVALID_TOKEN", "El enlace de baja no es válido"));
        consents.set(c, ConsentType.MARKETING, false, ConsentSource.UNSUBSCRIBE_LINK);
        return new MessageResponse("Listo: no te enviaremos más correos de publicidad. Los avisos de tus movimientos seguirán llegando.");
    }

    public static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
