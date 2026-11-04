package com.phraseloop.service;

public final class PhraseCategories {

    public static final String UNCATEGORIZED = "未分類";

    private PhraseCategories() {
    }

    public static boolean isUncategorized(String category) {
        return category == null || category.isBlank() || UNCATEGORIZED.equals(category.trim());
    }
}
