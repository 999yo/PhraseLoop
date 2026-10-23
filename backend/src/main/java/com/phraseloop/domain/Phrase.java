package com.phraseloop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "phrases")
public class Phrase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "english_text", nullable = false, length = 500)
    private String englishText;

    @Column(name = "japanese_text", nullable = false, length = 500)
    private String japaneseText;

    @Column(name = "example_sentence", length = 1000)
    private String exampleSentence;

    @Column(length = 100)
    private String category;

    @Column(name = "interval_stage", nullable = false)
    private short intervalStage;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Phrase() {
    }

    public Phrase(User user, String englishText, String japaneseText, String exampleSentence, String category, LocalDate nextReviewDate) {
        this.user = user;
        this.englishText = englishText;
        this.japaneseText = japaneseText;
        this.exampleSentence = exampleSentence;
        this.category = category;
        this.intervalStage = 0;
        this.nextReviewDate = nextReviewDate;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateText(String englishText, String japaneseText, String exampleSentence, String category) {
        this.englishText = englishText;
        this.japaneseText = japaneseText;
        this.exampleSentence = exampleSentence;
        this.category = category;
    }

    public void applySchedule(short intervalStage, LocalDate nextReviewDate) {
        this.intervalStage = intervalStage;
        this.nextReviewDate = nextReviewDate;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getEnglishText() {
        return englishText;
    }

    public String getJapaneseText() {
        return japaneseText;
    }

    public String getExampleSentence() {
        return exampleSentence;
    }

    public String getCategory() {
        return category;
    }

    public short getIntervalStage() {
        return intervalStage;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
