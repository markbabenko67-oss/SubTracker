package com.subtracker.app;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Duration;

public class Subscription {

    public static final int PERIOD_MONTHLY = 0;
    public static final int PERIOD_YEARLY = 1;
    public static final int PERIOD_WEEKLY = 2;
    public static final int PERIOD_QUARTERLY = 3;
    public static final int PERIOD_HALF_YEAR = 4;

    public static final int[] MONTHS_IN_PERIOD = {12, 1, 52, 4, 2};

    public long id;
    public String name;
    public double price;
    public int period;
    public int day;
    public long nextCharge;

    public double annualCost() {
        return price * MONTHS_IN_PERIOD[period];
    }

    public double monthlyCost() {
        return annualCost() / 12.0;
    }

    public static ZonedDateTime nextOccurrence(int day, long fromMillis, int period) {
        ZonedDateTime now = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(fromMillis), ZoneId.systemDefault());
        ZonedDateTime candidate = now
                .withHour(9).withMinute(0).withSecond(0).withNano(0)
                .withDayOfMonth(clampDay(day, now));

        for (int i = 0; i < 36; i++) {
            if (!candidate.isBefore(now)) {
                return candidate;
            }
            candidate = advance(candidate, period, day);
        }
        return now.withHour(9).withMinute(0).withSecond(0).withNano(0).plusDays(1);
    }

    public static ZonedDateTime advance(ZonedDateTime dt, int period, int day) {
        switch (period) {
            case PERIOD_MONTHLY: return clamp(dt.plusMonths(1), day);
            case PERIOD_YEARLY: return clamp(dt.plusYears(1), day);
            case PERIOD_WEEKLY: return dt.plusWeeks(1);
            case PERIOD_QUARTERLY: return clamp(dt.plusMonths(3), day);
            case PERIOD_HALF_YEAR: return clamp(dt.plusMonths(6), day);
            default: return clamp(dt.plusMonths(1), day);
        }
    }

    private static ZonedDateTime clamp(ZonedDateTime dt, int day) {
        return dt.withDayOfMonth(clampDay(day, dt));
    }

    private static int clampDay(int day, ZonedDateTime dt) {
        return Math.min(day, dt.getMonth().length(dt.toLocalDate().isLeapYear()));
    }

    public static int daysUntil(long nextCharge) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime today = now.toLocalDate().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime chargeDay = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(nextCharge), ZoneId.systemDefault())
                .toLocalDate().atStartOfDay(ZoneId.systemDefault());
        return (int) Duration.between(today, chargeDay).toDays();
    }
}