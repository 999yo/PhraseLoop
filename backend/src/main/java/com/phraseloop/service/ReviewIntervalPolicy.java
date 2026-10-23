package com.phraseloop.service;

import java.time.LocalDate;

public final class ReviewIntervalPolicy {

    public Schedule next(int currentStage, boolean remembered, LocalDate today) {
        if (currentStage < 0 || currentStage > 2) {
            throw new IllegalArgumentException("interval stage is out of range");
        }
        if (!remembered) {
            return new Schedule(0, today.plusDays(1));
        }
        if (currentStage == 0) {
            return new Schedule(1, today.plusDays(3));
        }
        return new Schedule(2, today.plusDays(7));
    }

    public record Schedule(int stage, LocalDate nextReviewDate) {
    }
}
