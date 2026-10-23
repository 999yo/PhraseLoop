package com.phraseloop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "review_histories")
public class ReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phrase_id", nullable = false)
    private Phrase phrase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean remembered;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    protected ReviewHistory() {
    }

    public ReviewHistory(Phrase phrase, User user, boolean remembered, Instant reviewedAt, LocalDate nextReviewDate) {
        this.phrase = phrase;
        this.user = user;
        this.remembered = remembered;
        this.reviewedAt = reviewedAt;
        this.nextReviewDate = nextReviewDate;
    }

    public Long getId() {
        return id;
    }

    public Phrase getPhrase() {
        return phrase;
    }

    public User getUser() {
        return user;
    }

    public boolean isRemembered() {
        return remembered;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }
}
