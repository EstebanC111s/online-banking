package com.onlinebanking.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

public final class BusinessDays {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private BusinessDays() {
    }

    public static LocalDate add(LocalDate start, int days) {
        if (days < 0) {
            throw new IllegalArgumentException("days debe ser >= 0");
        }
        LocalDate date = start;
        int added = 0;
        while (added < days) {
            date = date.plusDays(1);
            if (isBusinessDay(date)) {
                added++;
            }
        }
        return date;
    }

    public static boolean isBusinessDay(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY;
    }

    public static LocalDate today() {
        return LocalDate.now(BOGOTA);
    }
}
