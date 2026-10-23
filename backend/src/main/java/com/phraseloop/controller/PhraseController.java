package com.phraseloop.controller;

import com.phraseloop.dto.PhraseRequest;
import com.phraseloop.dto.PhraseResponse;
import com.phraseloop.security.CurrentUser;
import com.phraseloop.service.PhraseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/phrases")
public class PhraseController {

    private final PhraseService phraseService;
    private final CurrentUser currentUser;

    public PhraseController(PhraseService phraseService, CurrentUser currentUser) {
        this.phraseService = phraseService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<PhraseResponse> list(@RequestParam(name = "q", required = false) String query) {
        return phraseService.list(currentUser.require().getId(), query);
    }

    @GetMapping("/{id}")
    public PhraseResponse get(@PathVariable long id) {
        return phraseService.get(currentUser.require().getId(), id);
    }

    @PostMapping
    public ResponseEntity<PhraseResponse> create(@Valid @RequestBody PhraseRequest request) {
        PhraseResponse phrase = phraseService.create(currentUser.require().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(phrase);
    }

    @PutMapping("/{id}")
    public PhraseResponse update(@PathVariable long id, @Valid @RequestBody PhraseRequest request) {
        return phraseService.update(currentUser.require().getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        phraseService.delete(currentUser.require().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
