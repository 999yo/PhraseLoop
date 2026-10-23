package com.phraseloop.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewAnswerRequest(
        @NotNull(message = "覚えたかどうかを選択してください")
        Boolean remembered
) {
}
