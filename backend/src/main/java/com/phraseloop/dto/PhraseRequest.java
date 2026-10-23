package com.phraseloop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PhraseRequest(
        @NotBlank(message = "英語フレーズを入力してください")
        @Size(max = 500, message = "英語フレーズは500文字以内で入力してください")
        String englishText,
        @NotBlank(message = "日本語訳を入力してください")
        @Size(max = 500, message = "日本語訳は500文字以内で入力してください")
        String japaneseText,
        @Size(max = 1000, message = "例文は1000文字以内で入力してください")
        String exampleSentence,
        @Size(max = 100, message = "カテゴリは100文字以内で入力してください")
        String category
) {
}
