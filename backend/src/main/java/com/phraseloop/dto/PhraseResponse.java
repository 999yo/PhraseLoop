package com.phraseloop.dto;

import com.phraseloop.domain.Phrase;
import java.time.Instant;
import java.time.LocalDate;

public record PhraseResponse(
        long id,
        String englishText,
        String japaneseText,
        String exampleSentence,
        String category,
        short intervalStage,
        LocalDate nextReviewDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static PhraseResponse from(Phrase phrase) {
        return new PhraseResponse(
                phrase.getId(),
                phrase.getEnglishText(),
                phrase.getJapaneseText(),
                phrase.getExampleSentence(),
                phrase.getCategory(),
                phrase.getIntervalStage(),
                phrase.getNextReviewDate(),
                phrase.getCreatedAt(),
                phrase.getUpdatedAt());
    }
}
