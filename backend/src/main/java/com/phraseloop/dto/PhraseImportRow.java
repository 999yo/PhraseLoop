package com.phraseloop.dto;

public record PhraseImportRow(int line, String japanese, String english, String category, String error) {
}
