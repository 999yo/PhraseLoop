package com.phraseloop.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StreakCalculatorTest {

    private final LocalDate today = LocalDate.of(2026, 10, 23);

    @Test
    void noReviewsIsZero() {
        assertThat(StreakCalculator.days(Set.of(), today)).isZero();
    }

    @Test
    void todayAloneStartsAStreak() {
        assertThat(StreakCalculator.days(Set.of(today), today)).isEqualTo(1);
    }

    @Test
    void yesterdayCountsWhenTodayIsNotDone() {
        assertThat(StreakCalculator.days(Set.of(today.minusDays(1), today.minusDays(2)), today)).isEqualTo(2);
    }

    @Test
    void gapBeforeYesterdayBreaksTheStreak() {
        assertThat(StreakCalculator.days(Set.of(today.minusDays(2)), today)).isZero();
    }

    @Test
    void todayContinuesThroughYesterday() {
        Set<LocalDate> days = Set.of(today, today.minusDays(1), today.minusDays(2));
        assertThat(StreakCalculator.days(days, today)).isEqualTo(3);
    }

    @Test
    void aGapStopsTheCountEvenIfTodayWasReviewed() {
        Set<LocalDate> days = Set.of(today, today.minusDays(2));
        assertThat(StreakCalculator.days(days, today)).isEqualTo(1);
    }
}
