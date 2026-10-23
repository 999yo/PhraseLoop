package com.phraseloop.service;

import com.phraseloop.dto.DashboardResponse;
import com.phraseloop.repository.PhraseRepository;
import com.phraseloop.repository.ReviewHistoryRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final PhraseRepository phraseRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final Clock clock;
    private final ZoneId zoneId;

    public DashboardService(
            PhraseRepository phraseRepository,
            ReviewHistoryRepository reviewHistoryRepository,
            Clock clock,
            ZoneId zoneId) {
        this.phraseRepository = phraseRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.clock = clock;
        this.zoneId = zoneId;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(long userId) {
        LocalDate today = LocalDate.now(clock);
        Set<LocalDate> reviewedDays = reviewHistoryRepository.findReviewedAtByUserId(userId).stream()
                .map(instant -> instant.atZone(zoneId).toLocalDate())
                .collect(Collectors.toSet());
        YearMonth month = YearMonth.from(today);
        List<LocalDate> reviewedThisMonth = reviewedDays.stream()
                .filter(date -> YearMonth.from(date).equals(month))
                .sorted()
                .toList();
        return new DashboardResponse(
                (int) phraseRepository.countByUser_IdAndNextReviewDateLessThanEqual(userId, today),
                (int) phraseRepository.countByUser_Id(userId),
                (int) phraseRepository.countByUser_IdAndIntervalStage(userId, (short) 2),
                today,
                new DashboardResponse.StreakResponse(
                        StreakCalculator.days(reviewedDays, today),
                        reviewedDays.contains(today)),
                new DashboardResponse.CalendarResponse(month.getYear(), month.getMonthValue(), reviewedThisMonth));
    }
}
