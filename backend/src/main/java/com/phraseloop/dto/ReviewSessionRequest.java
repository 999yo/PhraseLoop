package com.phraseloop.dto;

import java.util.List;

public record ReviewSessionRequest(
        boolean all,
        List<String> categories,
        boolean includeUncategorized,
        Integer questionCount
) {
}
