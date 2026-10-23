package com.phraseloop.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReviewIntervalPolicyTest {

    private final ReviewIntervalPolicy policy = new ReviewIntervalPolicy();
    private final LocalDate today = LocalDate.of(2026, 10, 23);

    @Test
    void forgottenPhraseIsReviewedTomorrowAndResetsStage() {
        ReviewIntervalPolicy.Schedule schedule = policy.next(2, false, today);

        assertThat(schedule.stage()).isZero();
        assertThat(schedule.nextReviewDate()).isEqualTo(LocalDate.of(2026, 10, 24));
    }

    @Test
    void firstRememberMovesToThreeDays() {
        ReviewIntervalPolicy.Schedule schedule = policy.next(0, true, today);

        assertThat(schedule.stage()).isEqualTo(1);
        assertThat(schedule.nextReviewDate()).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void secondRememberMovesToSevenDays() {
        ReviewIntervalPolicy.Schedule schedule = policy.next(1, true, today);

        assertThat(schedule.stage()).isEqualTo(2);
        assertThat(schedule.nextReviewDate()).isEqualTo(LocalDate.of(2026, 10, 30));
    }

    @Test
    void masteredRememberStaysOnSevenDays() {
        ReviewIntervalPolicy.Schedule schedule = policy.next(2, true, today);

        assertThat(schedule.stage()).isEqualTo(2);
        assertThat(schedule.nextReviewDate()).isEqualTo(LocalDate.of(2026, 10, 30));
    }

    @Test
    void learningPhraseIsResetWhenForgotten() {
        ReviewIntervalPolicy.Schedule schedule = policy.next(1, false, today);

        assertThat(schedule.stage()).isZero();
        assertThat(schedule.nextReviewDate()).isEqualTo(today.plusDays(1));
    }
}
