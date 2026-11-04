package com.phraseloop.dto;

import java.util.List;

public record PhraseImportResult(List<PhraseImportRow> rows, int savedCount) {
}
