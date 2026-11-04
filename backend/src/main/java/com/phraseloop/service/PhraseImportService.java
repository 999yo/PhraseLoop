package com.phraseloop.service;

import com.phraseloop.dto.PhraseImportResult;
import com.phraseloop.dto.PhraseImportRow;
import com.phraseloop.error.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhraseImportService {

    private final PhraseService phraseService;

    public PhraseImportService(PhraseService phraseService) {
        this.phraseService = phraseService;
    }

    public PhraseImportResult preview(MultipartFile file) {
        return new PhraseImportResult(CsvPhraseParser.parse(read(file)), 0);
    }

    @Transactional
    public PhraseImportResult importFile(long userId, MultipartFile file) {
        List<PhraseImportRow> rows = CsvPhraseParser.parse(read(file));
        if (rows.stream().anyMatch(row -> row.error() != null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "CSVの内容を確認してください");
        }
        for (PhraseImportRow row : rows) {
            phraseService.createImported(userId, row.english(), row.japanese(), row.category());
        }
        return new PhraseImportResult(rows, rows.size());
    }

    private static String read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "ファイルを選んでください");
        }
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "CSVを読み取れませんでした");
        }
    }
}
