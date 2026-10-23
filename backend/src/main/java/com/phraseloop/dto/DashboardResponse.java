package com.phraseloop.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        int dueTodayCount,
        int totalPhraseCount,
        int masteredCount,
        LocalDate today,
        StreakResponse streak,
        CalendarResponse calendar
) {
    public record StreakResponse(int days, boolean todayReviewed) {
    }

    public record CalendarResponse(int year, int month, List<LocalDate> reviewedDates) {
    }
}
