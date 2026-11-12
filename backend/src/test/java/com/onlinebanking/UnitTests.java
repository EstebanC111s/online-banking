package com.onlinebanking;

import com.onlinebanking.account.Account;
import com.onlinebanking.account.AccountType;
import com.onlinebanking.common.BusinessDays;
import com.onlinebanking.common.InsufficientFundsException;
import com.onlinebanking.common.Money;
import com.onlinebanking.customer.Customer;
import com.onlinebanking.customer.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnitTests {

    @Test
    void businessDaysSkipWeekends() {
        LocalDate friday = LocalDate.of(2026, 11, 13);
        assertThat(BusinessDays.add(friday, 1)).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(BusinessDays.add(friday, 5)).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void accountNeverGoesNegative() {
        Account account = new Account("1234567890", new Customer("X", "x@x.co", "h", LocalDate.of(1990, 1, 1), Role.CUSTOMER),
                AccountType.SAVINGS);
        account.credit(Money.of("100"));
        assertThatThrownBy(() -> account.debit(Money.of("100.01"))).isInstanceOf(InsufficientFundsException.class);
        assertThat(account.getBalance()).isEqualByComparingTo("100");
    }

    @Test
    void moneyUsesTwoDecimals() {
        assertThat(Money.of("10.005").toPlainString()).isEqualTo("10.00");
        assertThat(Money.of("10.015").toPlainString()).isEqualTo("10.02");
    }
}
