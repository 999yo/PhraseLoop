package com.phraseloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.phraseloop.domain.Phrase;
import com.phraseloop.domain.ReviewHistory;
import com.phraseloop.domain.User;
import com.phraseloop.repository.PhraseRepository;
import com.phraseloop.repository.ReviewHistoryRepository;
import com.phraseloop.repository.UserRepository;
import com.phraseloop.support.IntegrationTestBase;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

class ReviewIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PhraseRepository phraseRepository;

    @Autowired
    private ReviewHistoryRepository reviewHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void rememberedAnswerUpdatesScheduleAndHistory() throws Exception {
        MockHttpSession session = register("review@example.com");
        long phraseId = createPhrase(session, "look forward to", "楽しみにする");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));

        mockMvc.perform(post("/api/reviews/" + phraseId + "/answer")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remembered\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalStage").value(1))
                .andExpect(jsonPath("$.nextReviewDate").value(today.plusDays(3).toString()))
                .andExpect(jsonPath("$.answer").doesNotExist());

        Phrase phrase = phraseRepository.findById(phraseId).orElseThrow();
        assertThat(phrase.getIntervalStage()).isEqualTo((short) 1);
        ReviewHistory history = reviewHistoryRepository.findAll().getFirst();
        assertThat(history.isRemembered()).isTrue();
        assertThat(history.getReviewedAt()).isNotNull();
        assertThat(history.getNextReviewDate()).isEqualTo(today.plusDays(3));

        mockMvc.perform(post("/api/reviews/" + phraseId + "/answer")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remembered\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalStage").value(2))
                .andExpect(jsonPath("$.nextReviewDate").value(today.plusDays(7).toString()));

        mockMvc.perform(get("/api/reviews/history").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].remembered").value(true));
    }

    @Test
    void forgottenAnswerIsDueTomorrow() throws Exception {
        MockHttpSession session = register("forget@example.com");
        long phraseId = createPhrase(session, "give up", "諦める");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));

        mockMvc.perform(post("/api/reviews/" + phraseId + "/answer")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remembered\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalStage").value(0))
                .andExpect(jsonPath("$.nextReviewDate").value(today.plusDays(1).toString()));

        mockMvc.perform(get("/api/reviews/today").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anotherUserCannotAnswerOrSeeHistory() throws Exception {
        MockHttpSession owner = register("owner-review@example.com");
        MockHttpSession other = register("other-review@example.com");
        long phraseId = createPhrase(owner, "carry out", "実行する");

        mockMvc.perform(post("/api/reviews/" + phraseId + "/answer")
                        .with(csrf())
                        .session(other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remembered\":true}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/reviews/" + phraseId + "/answer")
                        .with(csrf())
                        .session(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remembered\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reviews/history").session(other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/dashboard").session(other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streak.days").value(0))
                .andExpect(jsonPath("$.totalPhraseCount").value(0));
    }

    @Test
    void dashboardCountsStreakFromStoredHistory() throws Exception {
        MockHttpSession session = register("streak@example.com");
        long phraseId = createPhrase(session, "keep on", "続ける");
        mockMvc.perform(post("/api/reviews/" + phraseId + "/answer")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remembered\":true}"))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("streak@example.com").orElseThrow();
        Phrase phrase = phraseRepository.findById(phraseId).orElseThrow();
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");
        LocalDate today = LocalDate.now(tokyo);
        reviewHistoryRepository.saveAndFlush(new ReviewHistory(
                phrase,
                user,
                true,
                today.minusDays(1).atTime(12, 0).atZone(tokyo).toInstant(),
                today));

        mockMvc.perform(get("/api/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPhraseCount").value(1))
                .andExpect(jsonPath("$.masteredCount").value(0))
                .andExpect(jsonPath("$.dueTodayCount").value(0))
                .andExpect(jsonPath("$.streak.days").value(2))
                .andExpect(jsonPath("$.streak.todayReviewed").value(true))
                .andExpect(jsonPath("$.today").value(today.toString()))
                .andExpect(jsonPath("$.calendar.reviewedDates", org.hamcrest.Matchers.hasItem(today.toString())));

        assertThat(reviewHistoryRepository.findReviewedAtByUserId(user.getId())).hasSize(2);
    }

    @Test
    void sessionDrawsSelectedCategoriesWithoutDuplicates() throws Exception {
        MockHttpSession session = register("session@example.com");
        createPhrase(session, "good morning", "おはよう", "日常会話");
        createPhrase(session, "thank you", "ありがとう", "日常会話");
        createPhrase(session, "excuse me", "すみません", "日常会話");
        createPhrase(session, "attend a meeting", "会議に参加する", "仕事");
        createPhrase(session, "hello", "こんにちは", null);

        mockMvc.perform(get("/api/phrases/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("仕事"))
                .andExpect(jsonPath("$[0].phraseCount").value(1))
                .andExpect(jsonPath("$[1].name").value("日常会話"))
                .andExpect(jsonPath("$[1].phraseCount").value(3))
                .andExpect(jsonPath("$[2].name").value(nullValue()))
                .andExpect(jsonPath("$[2].phraseCount").value(1));

        MvcResult daily = mockMvc.perform(post("/api/reviews/session")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"all":false,"categories":["日常会話"],"includeUncategorized":false,"questionCount":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();
        List<String> categories = com.jayway.jsonpath.JsonPath.read(daily.getResponse().getContentAsString(), "$[*].category");
        List<Integer> ids = com.jayway.jsonpath.JsonPath.read(daily.getResponse().getContentAsString(), "$[*].id");
        assertThat(categories).containsOnly("日常会話");
        assertThat(new HashSet<>(ids)).hasSize(3);

        mockMvc.perform(post("/api/reviews/session")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"all":false,"categories":[],"includeUncategorized":true,"questionCount":20}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].japaneseText").value("こんにちは"));

        for (int extra = 0; extra < 6; extra++) {
            createPhrase(session, "extra " + extra, "追加" + extra, "仕事");
        }
        MvcResult limited = mockMvc.perform(post("/api/reviews/session")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"all":true,"categories":[],"includeUncategorized":false,"questionCount":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andReturn();
        List<Integer> limitedIds = com.jayway.jsonpath.JsonPath.read(limited.getResponse().getContentAsString(), "$[*].id");
        assertThat(new HashSet<>(limitedIds)).hasSize(10);

        mockMvc.perform(multipart("/api/phrases/import/preview")
                        .file(csv("""
                                japanese,english,category
                                おはよう,Good morning,日常会話
                                ありがとう,Thank you,
                                すみません,Excuse me,   
                                ,See you,
                                """))
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(0))
                .andExpect(jsonPath("$.rows[0].category").value("日常会話"))
                .andExpect(jsonPath("$.rows[1].category").value("未分類"))
                .andExpect(jsonPath("$.rows[1].error").value(nullValue()))
                .andExpect(jsonPath("$.rows[2].category").value("未分類"))
                .andExpect(jsonPath("$.rows[3].error").value("日本語が空欄です"));

        mockMvc.perform(get("/api/phrases").session(session)).andExpect(jsonPath("$.length()").value(11));

        mockMvc.perform(multipart("/api/phrases/import")
                        .file(csv("""
                                japanese,english,category
                                ,See you,
                                """))
                        .with(csrf())
                        .session(session))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/phrases").session(session)).andExpect(jsonPath("$.length()").value(11));

        mockMvc.perform(get("/api/phrases").session(session).param("category", "日常会話").param("uncategorized", "true"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/phrases/import")
                        .file(csv("""
                                japanese,english,category
                                おはよう,Good morning,日常会話
                                ありがとう,Thank you,
                                すみません,Excuse me,   
                                """))
                        .with(csrf())
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.savedCount").value(3));

        mockMvc.perform(get("/api/phrases").session(session).param("category", "日常会話"))
                .andExpect(jsonPath("$.length()").value(4));
        mockMvc.perform(get("/api/phrases").session(session).param("uncategorized", "true"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    private long createPhrase(MockHttpSession session, String english, String japanese) throws Exception {
        return createPhrase(session, english, japanese, null);
    }

    private long createPhrase(MockHttpSession session, String english, String japanese, String category) throws Exception {
        String categoryJson = category == null ? "null" : "\"" + category + "\"";
        MvcResult created = mockMvc.perform(post("/api/phrases")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"englishText":"%s","japaneseText":"%s","category":%s}
                                """.formatted(english, japanese, categoryJson)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "phrases.csv", "text/csv", content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
