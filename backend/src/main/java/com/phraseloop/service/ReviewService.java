package com.phraseloop.service;

import com.phraseloop.domain.Phrase;
import com.phraseloop.domain.ReviewHistory;
import com.phraseloop.dto.PhraseResponse;
import com.phraseloop.dto.ReviewAnswerResponse;
import com.phraseloop.dto.ReviewHistoryResponse;
import com.phraseloop.dto.ReviewSessionRequest;
import com.phraseloop.error.ApiException;
import com.phraseloop.repository.PhraseRepository;
import com.phraseloop.repository.ReviewHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    @Transactional(readOnly = true)
    public List<PhraseResponse> session(long userId, ReviewSessionRequest request) {
        if (request.questionCount() != null && !Set.of(10, 20, 30).contains(request.questionCount())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "出題数は10、20、30、全問から選んでください");
        }
        boolean selected = request.all()
                || request.includeUncategorized()
                || (request.categories() != null && request.categories().stream().anyMatch(category -> category != null && !category.isBlank()));
        if (!selected) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "カテゴリを選んでください");
        }
        List<Phrase> pool = new ArrayList<>();
        for (Phrase phrase : phraseRepository.findByUser_IdOrderByUpdatedAtDesc(userId)) {
            if (included(phrase, request)) {
                pool.add(phrase);
            }
        }
        Collections.shuffle(pool);
        int size = request.questionCount() == null ? pool.size() : Math.min(request.questionCount(), pool.size());
        return pool.subList(0, size).stream().map(PhraseResponse::from).toList();
    }

    @Transactional
    public ReviewAnswerResponse answer(long userId, long phraseId, boolean remembered) {
        Phrase phrase = phraseRepository.findByIdAndUser_Id(phraseId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "フレーズが見つかりません"));
        LocalDate today = LocalDate.now(clock);
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

    private static boolean included(Phrase phrase, ReviewSessionRequest request) {
        if (request.all()) {
            return true;
        }
        if (PhraseCategories.isUncategorized(phrase.getCategory())) {
            return request.includeUncategorized();
        }
        if (request.categories() == null) {
            return false;
        }
        return request.categories().stream()
                .anyMatch(category -> category != null && category.trim().equals(phrase.getCategory()));
    }
}
