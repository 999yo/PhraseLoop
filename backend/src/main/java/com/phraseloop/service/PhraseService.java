package com.phraseloop.service;

import com.phraseloop.domain.Phrase;
import com.phraseloop.domain.User;
import com.phraseloop.dto.PhraseRequest;
import com.phraseloop.dto.PhraseResponse;
import com.phraseloop.error.ApiException;
import com.phraseloop.repository.PhraseRepository;
import com.phraseloop.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
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
    public List<PhraseResponse> list(long userId, String query) {
        List<Phrase> phrases;
        if (query == null || query.isBlank()) {
            phrases = phraseRepository.findByUser_IdOrderByUpdatedAtDesc(userId);
        } else {
            phrases = phraseRepository.search(userId, query.trim());
        }
        return phrases.stream().map(PhraseResponse::from).toList();
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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
