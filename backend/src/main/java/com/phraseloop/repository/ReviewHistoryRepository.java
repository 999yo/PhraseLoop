package com.phraseloop.repository;

import com.phraseloop.domain.ReviewHistory;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewHistoryRepository extends JpaRepository<ReviewHistory, Long> {

    @Query("SELECT h.reviewedAt FROM ReviewHistory h WHERE h.user.id = :userId")
    List<Instant> findReviewedAtByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT h FROM ReviewHistory h
            JOIN FETCH h.phrase
            WHERE h.user.id = :userId
            ORDER BY h.reviewedAt DESC, h.id DESC
            """)
    List<ReviewHistory> findRecentByUserId(@Param("userId") Long userId);
}
