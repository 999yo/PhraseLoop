package com.phraseloop.service;

import com.phraseloop.dto.PhraseImportRow;
import com.phraseloop.error.ApiException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

public final class CsvPhraseParser {

    private CsvPhraseParser() {
    }

    public static List<PhraseImportRow> parse(String text) {
        String source = text.startsWith("\uFEFF") ? text.substring(1) : text;
        List<List<String>> records = records(source);
        if (records.isEmpty() || records.getFirst().stream().allMatch(String::isBlank)) {
            throw fileError("ヘッダーには japanese, english, category が必要です");
        }
        Map<String, Integer> columns = new LinkedHashMap<>();
        List<String> header = records.getFirst();
        for (int index = 0; index < header.size(); index++) {
            columns.putIfAbsent(header.get(index).trim(), index);
        }
        for (String name : List.of("japanese", "english", "category")) {
            if (!columns.containsKey(name)) {
                throw fileError("ヘッダーには japanese, english, category が必要です");
            }
        }
        List<PhraseImportRow> rows = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            List<String> cells = records.get(index);
            if (cells.stream().allMatch(String::isBlank)) {
                continue;
            }
            String japanese = cell(cells, columns.get("japanese"));
            String english = cell(cells, columns.get("english"));
            String category = cell(cells, columns.get("category"));
            List<String> problems = new ArrayList<>();
            if (japanese.isEmpty()) {
                problems.add("日本語が空欄です");
            } else if (japanese.length() > 500) {
                problems.add("日本語は500文字以内で入力してください");
            }
            if (english.isEmpty()) {
                problems.add("英語が空欄です");
            } else if (english.length() > 500) {
                problems.add("英語は500文字以内で入力してください");
            }
            String storedCategory = category.isBlank() ? PhraseCategories.UNCATEGORIZED : category;
            if (storedCategory.length() > 100) {
                problems.add("カテゴリは100文字以内で入力してください");
            }
            rows.add(new PhraseImportRow(
                    index + 1,
                    japanese,
                    english,
                    storedCategory,
                    problems.isEmpty() ? null : String.join("、", problems)));
        }
        if (rows.isEmpty()) {
            throw fileError("登録する行がありません");
        }
        return rows;
    }

    private static String cell(List<String> cells, int index) {
        if (index >= cells.size()) {
            return "";
        }
        return cells.get(index).trim();
    }

    private static ApiException fileError(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private static List<List<String>> records(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (quoted) {
                if (current == '"') {
                    if (index + 1 < text.length() && text.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(current);
                }
                continue;
            }
            if (current == '"') {
                quoted = true;
            } else if (current == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (current == '\n') {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else if (current != '\r') {
                field.append(current);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
