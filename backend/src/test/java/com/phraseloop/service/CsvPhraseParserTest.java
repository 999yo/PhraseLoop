package com.phraseloop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.phraseloop.error.ApiException;
import org.junit.jupiter.api.Test;

class CsvPhraseParserTest {

    @Test
    void blankAndWhitespaceCategoriesBecomeUncategorized() {
        var rows = CsvPhraseParser.parse("""
                english,category,japanese,note
                Good morning,日常会話,おはよう,ignored
                Thank you,   ,ありがとう,x
                Excuse me,,すみません,x
                """);

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).line()).isEqualTo(2);
        assertThat(rows.get(0).japanese()).isEqualTo("おはよう");
        assertThat(rows.get(0).english()).isEqualTo("Good morning");
        assertThat(rows.get(0).category()).isEqualTo("日常会話");
        assertThat(rows.get(0).error()).isNull();
        assertThat(rows.get(1).category()).isEqualTo("未分類");
        assertThat(rows.get(1).error()).isNull();
        assertThat(rows.get(2).category()).isEqualTo("未分類");
        assertThat(rows.get(2).error()).isNull();
    }

    @Test
    void emptyJapaneseOrEnglishIsARowError() {
        var rows = CsvPhraseParser.parse("""
                japanese,english,category
                ,See you,
                こんにちは,,日常会話
                ,,会話
                """);

        assertThat(rows.get(0).error()).isEqualTo("日本語が空欄です");
        assertThat(rows.get(0).category()).isEqualTo("未分類");
        assertThat(rows.get(1).error()).isEqualTo("英語が空欄です");
        assertThat(rows.get(2).error()).isEqualTo("日本語が空欄です、英語が空欄です");
    }

    @Test
    void missingHeaderAndEmptyBodyAreFileErrors() {
        assertThatThrownBy(() -> CsvPhraseParser.parse("japanese,english\nこんにちは,Hello\n"))
                .isInstanceOf(ApiException.class)
                .hasMessage("ヘッダーには japanese, english, category が必要です");
        assertThatThrownBy(() -> CsvPhraseParser.parse("japanese,english,category\n"))
                .isInstanceOf(ApiException.class)
                .hasMessage("登録する行がありません");
        assertThatThrownBy(() -> CsvPhraseParser.parse(""))
                .isInstanceOf(ApiException.class)
                .hasMessage("ヘッダーには japanese, english, category が必要です");
    }

    @Test
    void stripsBomAndKeepsQuotedCommas() {
        var rows = CsvPhraseParser.parse("\uFEFFjapanese,english,category\n\"こんにちは,朝\",\"Hello, there\",会話\n");

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().japanese()).isEqualTo("こんにちは,朝");
        assertThat(rows.getFirst().english()).isEqualTo("Hello, there");
        assertThat(rows.getFirst().category()).isEqualTo("会話");
        assertThat(rows.getFirst().error()).isNull();
    }
}
