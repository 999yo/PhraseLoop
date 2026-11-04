package com.phraseloop.controller;

import com.phraseloop.dto.PhraseImportResult;
import com.phraseloop.security.CurrentUser;
import com.phraseloop.service.PhraseImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/phrases/import")
public class PhraseImportController {

    private final PhraseImportService phraseImportService;
    private final CurrentUser currentUser;

    public PhraseImportController(PhraseImportService phraseImportService, CurrentUser currentUser) {
        this.phraseImportService = phraseImportService;
        this.currentUser = currentUser;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhraseImportResult preview(@RequestParam("file") MultipartFile file) {
        return phraseImportService.preview(file);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhraseImportResult> importFile(@RequestParam("file") MultipartFile file) {
        PhraseImportResult result = phraseImportService.importFile(currentUser.require().getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
