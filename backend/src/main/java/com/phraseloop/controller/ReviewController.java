package com.phraseloop.controller;

import com.phraseloop.dto.PhraseResponse;
import com.phraseloop.dto.ReviewAnswerRequest;
import com.phraseloop.dto.ReviewAnswerResponse;
import com.phraseloop.dto.ReviewHistoryResponse;
import com.phraseloop.security.CurrentUser;
import com.phraseloop.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUser currentUser;

    public ReviewController(ReviewService reviewService, CurrentUser currentUser) {
        this.reviewService = reviewService;
        this.currentUser = currentUser;
    }

    @GetMapping("/today")
    public List<PhraseResponse> today() {
        return reviewService.today(currentUser.require().getId());
    }

    @PostMapping("/{phraseId}/answer")
    public ReviewAnswerResponse answer(@PathVariable long phraseId, @Valid @RequestBody ReviewAnswerRequest request) {
        return reviewService.answer(currentUser.require().getId(), phraseId, request.remembered());
    }

    @GetMapping("/history")
    public List<ReviewHistoryResponse> history() {
        return reviewService.history(currentUser.require().getId());
    }
}
