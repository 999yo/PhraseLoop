package com.phraseloop.repository;

import com.phraseloop.domain.Phrase;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhraseRepository extends JpaRepository<Phrase, Long> {

    Optional<Phrase> findByIdAndUser_Id(Long id, Long userId);

    List<Phrase> findByUser_IdOrderByUpdatedAtDesc(Long userId);

    List<Phrase> findByUser_IdAndNextReviewDateLessThanEqualOrderByNextReviewDateAscIdAsc(Long userId, LocalDate date);

    long countByUser_Id(Long userId);

    long countByUser_IdAndNextReviewDateLessThanEqual(Long userId, LocalDate date);

    long countByUser_IdAndIntervalStage(Long userId, short intervalStage);

    @Query("""
            SELECT p FROM Phrase p
            WHERE p.user.id = :userId
              AND (
                LOWER(p.englishText) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.japaneseText) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.exampleSentence, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.category, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            ORDER BY p.updatedAt DESC
            """)
    List<Phrase> search(@Param("userId") Long userId, @Param("q") String query);
}
