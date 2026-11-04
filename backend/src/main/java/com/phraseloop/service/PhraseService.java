package com.phraseloop.service;

import com.phraseloop.domain.Phrase;
import com.phraseloop.domain.User;
import com.phraseloop.dto.PhraseCategoryCount;
import com.phraseloop.dto.PhraseRequest;
import com.phraseloop.dto.PhraseResponse;
import com.phraseloop.error.ApiException;
import com.phraseloop.repository.PhraseRepository;
import com.phraseloop.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PhraseService {

    private final PhraseRepository phraseRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public PhraseService(PhraseRepository phraseRepository, UserRepository userRepository, Clock clock) {
        this.phraseRepository = phraseRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PhraseResponse> list(long userId, String query, String category, boolean uncategorized) {
        if (uncategorized && category != null && !category.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "カテゴリの指定が重複しています");
        }
        List<Phrase> phrases;
        if (query == null || query.isBlank()) {
            phrases = phraseRepository.findByUser_IdOrderByUpdatedAtDesc(userId);
        } else {
            phrases = phraseRepository.search(userId, query.trim());
        }
        return phrases.stream()
                .filter(phrase -> matchesCategory(phrase, category, uncategorized))
                .map(PhraseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PhraseCategoryCount> categories(long userId) {
        TreeMap<String, Integer> named = new TreeMap<>();
        int uncategorized = 0;
        for (Phrase phrase : phraseRepository.findByUser_IdOrderByUpdatedAtDesc(userId)) {
            if (PhraseCategories.isUncategorized(phrase.getCategory())) {
                uncategorized++;
            } else {
                named.merge(phrase.getCategory(), 1, Integer::sum);
            }
        }
        List<PhraseCategoryCount> counts = new ArrayList<>();
        named.forEach((name, count) -> counts.add(new PhraseCategoryCount(name, count)));
        if (uncategorized > 0) {
            counts.add(new PhraseCategoryCount(null, uncategorized));
        }
        return counts;
    }

    @Transactional
    public PhraseResponse createImported(long userId, String english, String japanese, String category) {
        User user = userRepository.getReferenceById(userId);
        Phrase phrase = new Phrase(user, english, japanese, null, category, LocalDate.now(clock));
        return PhraseResponse.from(phraseRepository.save(phrase));
    }

    @Transactional(readOnly = true)
    public PhraseResponse get(long userId, long phraseId) {
        return PhraseResponse.from(findOwned(userId, phraseId));
    }

    @Transactional
    public PhraseResponse create(long userId, PhraseRequest request) {
        User user = userRepository.getReferenceById(userId);
        Phrase phrase = new Phrase(
                user,
                request.englishText().trim(),
                request.japaneseText().trim(),
                blankToNull(request.exampleSentence()),
                blankToNull(request.category()),
                LocalDate.now(clock));
        return PhraseResponse.from(phraseRepository.save(phrase));
    }

    @Transactional
    public PhraseResponse update(long userId, long phraseId, PhraseRequest request) {
        Phrase phrase = findOwned(userId, phraseId);
        phrase.updateText(
                request.englishText().trim(),
                request.japaneseText().trim(),
                blankToNull(request.exampleSentence()),
                blankToNull(request.category()));
        return PhraseResponse.from(phrase);
    }

    @Transactional
    public void delete(long userId, long phraseId) {
        Phrase phrase = findOwned(userId, phraseId);
        phraseRepository.delete(phrase);
    }

    private Phrase findOwned(long userId, long phraseId) {
        return phraseRepository.findByIdAndUser_Id(phraseId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "フレーズが見つかりません"));
    }

    private static boolean matchesCategory(Phrase phrase, String category, boolean uncategorized) {
        if (uncategorized) {
            return PhraseCategories.isUncategorized(phrase.getCategory());
        }
        if (category == null || category.isBlank()) {
            return true;
        }
        return category.trim().equals(phrase.getCategory());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
