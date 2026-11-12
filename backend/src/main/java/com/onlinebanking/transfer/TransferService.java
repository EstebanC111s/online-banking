package com.onlinebanking.transfer;

import com.onlinebanking.account.*;
import com.onlinebanking.common.BusinessException;
import com.onlinebanking.common.InsufficientFundsException;
import com.onlinebanking.common.Money;
import com.onlinebanking.common.NotFoundException;
import com.onlinebanking.config.AppProperties;
import com.onlinebanking.customer.Customer;
import com.onlinebanking.fees.FeeSchedule;
import com.onlinebanking.notification.Mailer;
import com.onlinebanking.transfer.TransferDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private final AccountRepository accounts;
    private final BankTransactionRepository transactions;
    private final TransferQuoteRepository quotes;
    private final FeeSchedule fees;
    private final Mailer mailer;
    private final AppProperties props;

    public TransferService(AccountRepository accounts, BankTransactionRepository transactions, TransferQuoteRepository quotes,
                           FeeSchedule fees, Mailer mailer, AppProperties props) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.quotes = quotes;
        this.fees = fees;
        this.mailer = mailer;
        this.props = props;
    }

    @Transactional
    public QuoteResponse quote(Customer customer, QuoteRequest req) {
        Account from = accounts.findByIdAndOwnerId(req.fromAccountId(), customer.getId())
                .orElseThrow(() -> new NotFoundException("No encontramos la cuenta de origen"));
        Account to = accounts.findByNumber(req.toAccountNumber())
                .orElseThrow(() -> new NotFoundException("No existe ninguna cuenta con ese número"));

        if (!from.isOpen()) {
            throw BusinessException.conflict("ACCOUNT_NOT_OPEN", "La cuenta de origen está cerrada");
        }
        if (!to.isOpen()) {
            throw BusinessException.conflict("DESTINATION_NOT_OPEN", "La cuenta destino no puede recibir dinero");
        }
        if (from.getId().equals(to.getId())) {
            throw BusinessException.badRequest("SAME_ACCOUNT", "La cuenta de origen y destino son la misma");
        }

        BigDecimal amount = Money.normalize(req.amount());
        boolean ownAccount = to.isOwnedBy(customer.getId());
        BigDecimal fee = fees.transferFee(ownAccount);
        BigDecimal total = amount.add(fee);
        if (from.getBalance().compareTo(total) < 0) {
            throw new InsufficientFundsException(from.getBalance(), total);
        }

        String description = (req.description() == null || req.description().isBlank())
                ? "Transferencia" : req.description().trim();
        Instant expiresAt = Instant.now().plusSeconds(props.quoteTtlSeconds());
        TransferQuote q = quotes.save(new TransferQuote(UUID.randomUUID().toString(), customer, from, to, amount, fee,
                description, expiresAt));
        return toResponse(q, ownAccount);
    }

    @Transactional
    public TransferResult confirm(Long customerId, String quoteId) {
        TransferQuote q = quotes.findByIdForUpdate(quoteId)
                .filter(x -> x.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> new NotFoundException("No encontramos esa cotización"));

        if (q.getStatus() == QuoteStatus.CONFIRMED) {
            return result(q, q.getFromAccount().getBalance());
        }
        if (q.getStatus() != QuoteStatus.PENDING) {
            throw BusinessException.conflict("QUOTE_NOT_PENDING", "Esta cotización fue cancelada o venció. Crea una nueva.");
        }
        if (q.isExpired(Instant.now())) {
            // no se marca EXPIRED aca porque la excepcion hace rollback
            throw new BusinessException(HttpStatus.GONE, "QUOTE_EXPIRED",
                    "La cotización venció. Vuelve a cotizar para ver el valor actualizado.");
        }

        // siempre en el mismo orden (id menor primero) para no generar deadlocks
        Long fromId = q.getFromAccount().getId();
        Long toId = q.getToAccount().getId();
        Account first = lock(Math.min(fromId, toId));
        Account second = lock(Math.max(fromId, toId));
        Account from = first.getId().equals(fromId) ? first : second;
        Account to = first.getId().equals(toId) ? first : second;

        if (!from.isOpen() || !to.isOpen()) {
            throw BusinessException.conflict("ACCOUNT_NOT_OPEN", "Una de las cuentas ya no está abierta");
        }
        if (from.getBalance().compareTo(q.getTotal()) < 0) {
            throw new InsufficientFundsException(from.getBalance(), q.getTotal());
        }

        String reference = "TRF-" + q.getId().substring(0, 8).toUpperCase();

        from.debit(q.getAmount());
        transactions.save(new BankTransaction(from, TransactionType.TRANSFER_OUT, q.getAmount().negate(),
                q.getDescription(), reference, to.getNumber()));

        if (q.getFee().signum() > 0) {
            from.debit(q.getFee());
            transactions.save(new BankTransaction(from, TransactionType.FEE, q.getFee().negate(),
                    "Comisión transferencia " + reference, reference, null));
        }

        to.credit(q.getAmount());
        transactions.save(new BankTransaction(to, TransactionType.TRANSFER_IN, q.getAmount(),
                q.getDescription(), reference, from.getNumber()));

        q.markConfirmed();

        mailer.sendTransactional(from.getOwner().getEmail(), "Enviaste " + Money.format(q.getAmount()),
                "Transferencia " + reference + " a la cuenta " + mask(to.getNumber()) + ". Comisión: "
                        + Money.format(q.getFee()) + ". Si no la reconoces, abre una disputa desde tus movimientos.");
        if (!to.getOwner().getId().equals(from.getOwner().getId())) {
            mailer.sendTransactional(to.getOwner().getEmail(), "Recibiste " + Money.format(q.getAmount()),
                    "Recibiste una transferencia " + reference + " en tu cuenta " + mask(to.getNumber()) + ".");
        }
        return result(q, from.getBalance());
    }

    @Transactional
    public QuoteResponse cancel(Long customerId, String quoteId) {
        TransferQuote q = quotes.findByIdForUpdate(quoteId)
                .filter(x -> x.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> new NotFoundException("No encontramos esa cotización"));
        if (q.getStatus() == QuoteStatus.PENDING) {
            q.markCancelled();
        }
        return toResponse(q, q.getToAccount().isOwnedBy(customerId));
    }

    private Account lock(Long id) {
        return accounts.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("No encontramos esa cuenta"));
    }

    private TransferResult result(TransferQuote q, BigDecimal newBalance) {
        return new TransferResult("TRF-" + q.getId().substring(0, 8).toUpperCase(), q.getStatus().name(),
                q.getAmount(), q.getFee(), q.getTotal(), newBalance, q.getConfirmedAt());
    }

    private QuoteResponse toResponse(TransferQuote q, boolean ownAccount) {
        String recipient = ownAccount ? "Tú (cuenta propia)" : maskName(q.getToAccount().getOwner().getFullName());
        return new QuoteResponse(q.getId(), q.getFromAccount().getNumber(), q.getToAccount().getNumber(), recipient,
                ownAccount, q.getAmount(), q.getFee(), q.getTotal(), q.getDescription(), displayStatus(q), q.getExpiresAt());
    }

    private static String displayStatus(TransferQuote q) {
        if (q.getStatus() == QuoteStatus.PENDING && q.isExpired(Instant.now())) {
            return QuoteStatus.EXPIRED.name();
        }
        return q.getStatus().name();
    }

    static String maskName(String fullName) {
        return Arrays.stream(fullName.trim().split("\\s+"))
                .map(part -> part.length() <= 2 ? part : part.substring(0, 2) + "***")
                .collect(Collectors.joining(" "));
    }

    static String mask(String accountNumber) {
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
