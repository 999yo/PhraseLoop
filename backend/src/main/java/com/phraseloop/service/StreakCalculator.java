package com.phraseloop.service;

import java.time.LocalDate;
import java.util.Set;

public final class StreakCalculator {

    private StreakCalculator() {
    }

    public static int days(Set<LocalDate> reviewedDays, LocalDate today) {
        LocalDate cursor = reviewedDays.contains(today) ? today : today.minusDays(1);
        int count = 0;
        while (reviewedDays.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }
}
