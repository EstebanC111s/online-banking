package com.onlinebanking.account;

import com.onlinebanking.account.AccountDtos.*;
import com.onlinebanking.common.BusinessDays;
import com.onlinebanking.common.BusinessException;
import com.onlinebanking.common.Money;
import com.onlinebanking.common.NotFoundException;
import com.onlinebanking.customer.Customer;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

@Service
public class AccountService {

    static final int MAX_OPEN_ACCOUNTS = 5;
    static final int RETRACT_BUSINESS_DAYS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final BankTransactionRepository transactions;

    public AccountService(AccountRepository accounts, BankTransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public List<AccountView> list(Long customerId) {
        return accounts.findByOwnerIdOrderByIdAsc(customerId).stream().map(this::toView).toList();
    }

    @Transactional
    public AccountView open(Customer owner, AccountType type) {
        long open = accounts.findByOwnerIdOrderByIdAsc(owner.getId()).stream().filter(Account::isOpen).count();
        if (open >= MAX_OPEN_ACCOUNTS) {
            throw BusinessException.conflict("TOO_MANY_ACCOUNTS", "Puedes tener máximo " + MAX_OPEN_ACCOUNTS + " cuentas abiertas");
        }
        Account account = accounts.save(new Account(newAccountNumber(), owner, type));
        return toView(account);
    }

    @Transactional
    public TransactionView deposit(Long customerId, Long accountId, BigDecimal amount) {
        Account account = lockOwned(customerId, accountId);
        BigDecimal value = Money.normalize(amount);
        account.credit(value);
        BankTransaction tx = transactions.save(new BankTransaction(account, TransactionType.DEPOSIT, value,
                "Depósito simulado", null, null));
        return TransactionView.of(tx);
    }

    @Transactional(readOnly = true)
    public List<TransactionView> transactions(Long customerId, Long accountId, int limit) {
        Account account = accounts.findByIdAndOwnerId(accountId, customerId).orElseThrow(() -> new NotFoundException("No encontramos esa cuenta"));
        int size = Math.min(Math.max(limit, 1), 200);
        return transactions.findByAccountIdOrderByCreatedAtDescIdDesc(account.getId(), PageRequest.of(0, size))
                .stream().map(TransactionView::of).toList();
    }

    @Transactional
    public AccountView retract(Long customerId, Long accountId) {
        Account account = lockOwned(customerId, accountId);
        if (!account.isOpen()) {
            throw BusinessException.conflict("ACCOUNT_NOT_OPEN", "La cuenta ya está cerrada");
        }
        LocalDate deadline = retractDeadline(account);
        if (BusinessDays.today().isAfter(deadline)) {
            throw BusinessException.conflict("RETRACT_EXPIRED",
                    "El plazo de retracto venció el " + deadline + ". Puedes cerrar la cuenta normalmente, también sin costo.");
        }
        requireZeroBalance(account);
        account.close(AccountStatus.RETRACTED);
        return toView(account);
    }

    @Transactional
    public AccountView close(Long customerId, Long accountId) {
        Account account = lockOwned(customerId, accountId);
        if (!account.isOpen()) {
            throw BusinessException.conflict("ACCOUNT_NOT_OPEN", "La cuenta ya está cerrada");
        }
        requireZeroBalance(account);
        account.close(AccountStatus.CLOSED);
        return toView(account);
    }

    public LocalDate retractDeadline(Account account) {
        LocalDate opened = account.getOpenedAt().atZone(BusinessDays.BOGOTA).toLocalDate();
        return BusinessDays.add(opened, RETRACT_BUSINESS_DAYS);
    }

    // primero el lock y despues validar el dueño, si no hibernate se queda con el saldo viejo
    private Account lockOwned(Long customerId, Long accountId) {
        return accounts.findByIdForUpdate(accountId)
                .filter(a -> a.isOwnedBy(customerId))
                .orElseThrow(() -> new NotFoundException("No encontramos esa cuenta"));
    }

    private void requireZeroBalance(Account account) {
        if (account.getBalance().signum() != 0) {
            throw BusinessException.conflict("BALANCE_NOT_ZERO",
                    "La cuenta tiene " + Money.format(account.getBalance())
                            + ". Muévelo primero a otra cuenta tuya (es gratis) y vuelve a intentarlo.");
        }
    }

    private AccountView toView(Account a) {
        LocalDate deadline = retractDeadline(a);
        boolean canRetract = a.isOpen() && !BusinessDays.today().isAfter(deadline);
        return new AccountView(a.getId(), a.getNumber(), a.getType(), a.getType().getLabel(), a.getStatus(),
                a.getBalance(), a.getOpenedAt(), a.isOpen() ? deadline : null, canRetract);
    }

    private String newAccountNumber() {
        String number;
        do {
            number = String.valueOf(1_000_000_000L + (long) (RANDOM.nextDouble() * 8_999_999_999L));
        } while (accounts.existsByNumber(number));
        return number;
    }
}
