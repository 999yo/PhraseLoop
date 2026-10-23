package com.phraseloop.service;

import com.phraseloop.domain.Phrase;
import com.phraseloop.domain.ReviewHistory;
import com.phraseloop.dto.PhraseResponse;
import com.phraseloop.dto.ReviewAnswerResponse;
import com.phraseloop.dto.ReviewHistoryResponse;
import com.phraseloop.error.ApiException;
import com.phraseloop.repository.PhraseRepository;
import com.phraseloop.repository.ReviewHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final PhraseRepository phraseRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final ReviewIntervalPolicy intervalPolicy;
    private final Clock clock;

    public ReviewService(
            PhraseRepository phraseRepository,
            ReviewHistoryRepository reviewHistoryRepository,
            ReviewIntervalPolicy intervalPolicy,
            Clock clock) {
        this.phraseRepository = phraseRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.intervalPolicy = intervalPolicy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PhraseResponse> today(long userId) {
        LocalDate today = LocalDate.now(clock);
        return phraseRepository
                .findByUser_IdAndNextReviewDateLessThanEqualOrderByNextReviewDateAscIdAsc(userId, today)
                .stream()
                .map(PhraseResponse::from)
                .toList();
    }

    @Transactional
    public ReviewAnswerResponse answer(long userId, long phraseId, boolean remembered) {
        Phrase phrase = phraseRepository.findByIdAndUser_Id(phraseId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "フレーズが見つかりません"));
        LocalDate today = LocalDate.now(clock);
        if (phrase.getNextReviewDate().isAfter(today)) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIEW_NOT_DUE", "このフレーズは本日の復習対象ではありません");
        }
        ReviewIntervalPolicy.Schedule schedule = intervalPolicy.next(phrase.getIntervalStage(), remembered, today);
        phrase.applySchedule((short) schedule.stage(), schedule.nextReviewDate());
        reviewHistoryRepository.save(new ReviewHistory(
                phrase,
                phrase.getUser(),
                remembered,
                Instant.now(clock),
                schedule.nextReviewDate()));
        return new ReviewAnswerResponse(phrase.getId(), remembered, phrase.getIntervalStage(), phrase.getNextReviewDate());
    }

    @Transactional(readOnly = true)
    public List<ReviewHistoryResponse> history(long userId) {
        return reviewHistoryRepository.findRecentByUserId(userId).stream()
                .limit(50)
                .map(ReviewHistoryResponse::from)
                .toList();
    }
}
