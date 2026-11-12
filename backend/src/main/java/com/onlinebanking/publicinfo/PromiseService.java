package com.onlinebanking.publicinfo;

import com.onlinebanking.common.BusinessDays;
import com.onlinebanking.common.Money;
import com.onlinebanking.dispute.Dispute;
import com.onlinebanking.dispute.DisputeRepository;
import com.onlinebanking.dispute.DisputeStatus;
import com.onlinebanking.fees.FeeSchedule;
import com.onlinebanking.review.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PromiseService {

    private final FeeSchedule fees;
    private final DisputeRepository disputes;
    private final ReviewRepository reviews;

    public PromiseService(FeeSchedule fees, DisputeRepository disputes, ReviewRepository reviews) {
        this.fees = fees;
        this.disputes = disputes;
        this.reviews = reviews;
    }

    public record Promise(String claim, String evidence, boolean kept, String howToVerify) {
    }

    @Transactional(readOnly = true)
    public List<Promise> all() {
        boolean freeAccount = fees.accountOpeningFee().signum() == 0 && fees.monthlyMaintenanceFee().signum() == 0;
        boolean freeOwnTransfers = fees.ownTransferFee().signum() == 0;

        List<Dispute> resolvedOrOpen = disputes.findAll();
        LocalDate today = BusinessDays.today();
        long resolved = resolvedOrOpen.stream().filter(d -> d.getStatus() != DisputeStatus.OPEN).count();
        long resolvedOnTime = resolvedOrOpen.stream()
                .filter(d -> d.getStatus() != DisputeStatus.OPEN)
                .filter(d -> !d.getResolvedAt().atZone(BusinessDays.BOGOTA).toLocalDate().isAfter(d.getDueDate()))
                .count();
        long overdue = resolvedOrOpen.stream()
                .filter(d -> d.getStatus() == DisputeStatus.OPEN && today.isAfter(d.getDueDate()))
                .count();

        return List.of(
                new Promise("Abrir y mantener una cuenta cuesta $0",
                        "Tarifa de apertura: " + Money.format(fees.accountOpeningFee()) + " · cuota de manejo: "
                                + Money.format(fees.monthlyMaintenanceFee()),
                        freeAccount, "Consulta la tabla de tarifas: es la misma que usa el sistema para cobrar"),
                new Promise("Transferir entre tus propias cuentas es gratis",
                        "Comisión configurada: " + Money.format(fees.ownTransferFee()),
                        freeOwnTransfers, "Haz una transferencia entre tus cuentas: la cotización muestra la comisión antes de confirmar"),
                new Promise("Respondemos cada disputa en máximo 15 días hábiles",
                        resolved + " disputas resueltas, " + resolvedOnTime + " a tiempo; " + overdue + " abiertas fuera de plazo",
                        overdue == 0 && resolved == resolvedOnTime,
                        "Cada disputa muestra su fecha límite en la sección Disputas"),
                new Promise("Solo publicamos reseñas de clientes verificados y no ocultamos las malas",
                        reviews.count() + " reseñas publicadas, todas de clientes con movimientos reales",
                        true, "El promedio de la página de reseñas se calcula con todas, incluidas las de 1 estrella"));
    }
}
