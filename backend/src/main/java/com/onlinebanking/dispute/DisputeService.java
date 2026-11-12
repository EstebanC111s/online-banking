package com.onlinebanking.dispute;

import com.onlinebanking.account.*;
import com.onlinebanking.common.BusinessDays;
import com.onlinebanking.common.BusinessException;
import com.onlinebanking.common.NotFoundException;
import com.onlinebanking.customer.Customer;
import com.onlinebanking.dispute.DisputeDtos.DisputeView;
import com.onlinebanking.notification.Mailer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class DisputeService {

    static final int SLA_BUSINESS_DAYS = 15;
    static final Duration MAX_AGE = Duration.ofDays(90);

    private final DisputeRepository disputes;
    private final BankTransactionRepository transactions;
    private final AccountRepository accounts;
    private final Mailer mailer;

    public DisputeService(DisputeRepository disputes, BankTransactionRepository transactions,
                          AccountRepository accounts, Mailer mailer) {
        this.disputes = disputes;
        this.transactions = transactions;
        this.accounts = accounts;
        this.mailer = mailer;
    }

    @Transactional
    public DisputeView open(Customer customer, Long transactionId, String reason) {
        BankTransaction tx = transactions.findByIdAndAccountOwnerId(transactionId, customer.getId())
                .orElseThrow(() -> new NotFoundException("Movimiento"));
        if (!tx.getType().isDisputable()) {
            throw BusinessException.badRequest("NOT_DISPUTABLE", "Solo se pueden disputar transferencias enviadas y comisiones");
        }
        if (tx.getCreatedAt().isBefore(Instant.now().minus(MAX_AGE))) {
            throw BusinessException.conflict("TOO_OLD", "Solo se pueden disputar movimientos de los últimos 90 días");
        }
        if (disputes.existsByTransactionIdAndStatusIn(tx.getId(), List.of(DisputeStatus.OPEN, DisputeStatus.REFUNDED))) {
            throw BusinessException.conflict("ALREADY_DISPUTED", "Ya existe una disputa abierta o devuelta para este movimiento");
        }

        Dispute d = disputes.save(new Dispute(customer, tx, reason.trim(), BusinessDays.add(BusinessDays.today(), SLA_BUSINESS_DAYS)));

        if (tx.getType() == TransactionType.FEE) {
            refund(d, "Devolución automática de comisión: las comisiones disputadas se devuelven sin preguntas.");
        } else {
            mailer.sendTransactional(customer.getEmail(), "Recibimos tu disputa #" + d.getId(),
                    "La revisaremos y te responderemos a más tardar el " + d.getDueDate() + ".");
        }
        return DisputeView.of(d);
    }

    @Transactional(readOnly = true)
    public List<DisputeView> mine(Long customerId) {
        return disputes.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(DisputeView::of).toList();
    }

    @Transactional(readOnly = true)
    public List<DisputeView> openQueue() {
        return disputes.findByStatusOrderByDueDateAsc(DisputeStatus.OPEN).stream().map(DisputeView::of).toList();
    }

    @Transactional
    public DisputeView resolve(Long disputeId, boolean doRefund, String note) {
        Dispute d = disputes.findById(disputeId).orElseThrow(() -> new NotFoundException("Disputa"));
        if (doRefund) {
            refund(d, (note == null || note.isBlank()) ? "Disputa aceptada" : note);
        } else {
            d.resolve(DisputeStatus.REJECTED, note);
            mailer.sendTransactional(d.getCustomer().getEmail(), "Respuesta a tu disputa #" + d.getId(),
                    "No procede la devolución. Motivo: " + note);
        }
        return DisputeView.of(d);
    }

    private void refund(Dispute d, String note) {
        BankTransaction original = d.getTransaction();
        BigDecimal amount = original.getAmount().abs();
        Account account = accounts.findByIdForUpdate(original.getAccount().getId())
                .orElseThrow(() -> new NotFoundException("Cuenta"));
        if (!account.isOpen()) {
            throw BusinessException.conflict("ACCOUNT_NOT_OPEN", "La cuenta del movimiento está cerrada; no se puede abonar la devolución");
        }
        account.credit(amount);
        transactions.save(new BankTransaction(account, TransactionType.REFUND, amount,
                "Devolución disputa #" + d.getId(), original.getReference(), null));
        d.resolve(DisputeStatus.REFUNDED, note);
        mailer.sendTransactional(d.getCustomer().getEmail(), "Devolución aprobada - disputa #" + d.getId(),
                "Abonamos el dinero en tu cuenta. " + note);
    }
}
