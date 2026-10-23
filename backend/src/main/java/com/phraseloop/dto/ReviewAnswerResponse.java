package com.phraseloop.dto;

import java.time.LocalDate;

public record ReviewAnswerResponse(
        long phraseId,
        boolean remembered,
        short intervalStage,
        LocalDate nextReviewDate
) {
}
