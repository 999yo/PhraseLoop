package com.phraseloop.dto;

import com.phraseloop.domain.ReviewHistory;
import java.time.Instant;
import java.time.LocalDate;

public record ReviewHistoryResponse(
        long id,
        long phraseId,
        String englishText,
        String japaneseText,
        boolean remembered,
        Instant reviewedAt,
        LocalDate nextReviewDate
) {
    public static ReviewHistoryResponse from(ReviewHistory history) {
        return new ReviewHistoryResponse(
                history.getId(),
                history.getPhrase().getId(),
                history.getPhrase().getEnglishText(),
                history.getPhrase().getJapaneseText(),
                history.isRemembered(),
                history.getReviewedAt(),
                history.getNextReviewDate());
    }
}
