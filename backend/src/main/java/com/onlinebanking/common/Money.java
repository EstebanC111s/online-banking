package com.onlinebanking.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    private static final Locale CO = Locale.forLanguageTag("es-CO");

    private Money() {
    }

    public static BigDecimal of(String value) {
        return normalize(new BigDecimal(value));
    }

    public static BigDecimal normalize(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static String format(BigDecimal value) {
        NumberFormat f = NumberFormat.getCurrencyInstance(CO);
        return f.format(value);
    }
}
