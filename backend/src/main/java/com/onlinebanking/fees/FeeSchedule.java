package com.onlinebanking.fees;

import com.onlinebanking.common.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// la tabla publica y el cobro de las transferencias salen de aca
@Component
public class FeeSchedule {

    private static final BigDecimal OWN_TRANSFER = Money.of("0");
    private static final BigDecimal THIRD_PARTY_TRANSFER = Money.of("1500");
    private static final BigDecimal ACCOUNT_OPENING = Money.of("0");
    private static final BigDecimal MONTHLY_MAINTENANCE = Money.of("0");

    public record FeeItem(String concept, BigDecimal amount, String note) {
    }

    public BigDecimal transferFee(boolean sameOwner) {
        return sameOwner ? OWN_TRANSFER : THIRD_PARTY_TRANSFER;
    }

    public BigDecimal thirdPartyTransferFee() {
        return THIRD_PARTY_TRANSFER;
    }

    public BigDecimal ownTransferFee() {
        return OWN_TRANSFER;
    }

    public BigDecimal accountOpeningFee() {
        return ACCOUNT_OPENING;
    }

    public BigDecimal monthlyMaintenanceFee() {
        return MONTHLY_MAINTENANCE;
    }

    public List<FeeItem> publicTable() {
        return List.of(
                new FeeItem("Apertura de cuenta", ACCOUNT_OPENING, "Siempre gratis"),
                new FeeItem("Cuota de manejo mensual", MONTHLY_MAINTENANCE, "No existe"),
                new FeeItem("Transferencia entre tus propias cuentas", OWN_TRANSFER, "Gratis"),
                new FeeItem("Transferencia a otra persona en Banco Gocomezbo", THIRD_PARTY_TRANSFER, "Se muestra antes de confirmar"),
                new FeeItem("Cierre de cuenta o retracto", Money.ZERO, "Sin penalidad"),
                new FeeItem("Descarga de tus datos", Money.ZERO, "Gratis, cuando quieras")
        );
    }
}
