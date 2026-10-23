package com.phraseloop;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.phraseloop.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

class PhraseIntegrationTest extends IntegrationTestBase {

    @Test
    void ownerCanCreateUpdateSearchAndDelete() throws Exception {
        MockHttpSession session = register("owner@example.com");

        MvcResult created = mockMvc.perform(post("/api/phrases")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(phrase("get used to", "慣れる", "I got used to it.", "日常")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.intervalStage").value(0))
                .andExpect(jsonPath("$.englishText").value("get used to"))
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(put("/api/phrases/" + id)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(phrase("be used to", "慣れている", "", "日常")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.englishText").value("be used to"))
                .andExpect(jsonPath("$.exampleSentence").isEmpty())
                .andExpect(jsonPath("$.intervalStage").value(0));

        mockMvc.perform(get("/api/phrases").session(session).param("q", "慣れて"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/phrases").session(session).param("q", "存在しない"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(delete("/api/phrases/" + id).with(csrf()).session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/phrases/" + id).session(session)).andExpect(status().isNotFound());
    }

    @Test
    void invalidPhraseIsRejected() throws Exception {
        MockHttpSession session = register("invalid@example.com");

        mockMvc.perform(post("/api/phrases")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(phrase("", "", null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.englishText").exists())
                .andExpect(jsonPath("$.fields.japaneseText").exists());
    }

    @Test
    void anotherUserCannotReadUpdateOrDelete() throws Exception {
        MockHttpSession owner = register("a@example.com");
        MockHttpSession other = register("b@example.com");
        MvcResult created = mockMvc.perform(post("/api/phrases")
                        .with(csrf())
                        .session(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(phrase("attend a meeting", "会議に参加する", null, "仕事")))
                .andExpect(status().isCreated())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(get("/api/phrases/" + id).session(other)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/phrases").session(other)).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(put("/api/phrases/" + id)
                        .with(csrf())
                        .session(other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(phrase("stolen", "盗んだ", null, null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/phrases/" + id).with(csrf()).session(other)).andExpect(status().isNotFound());

        mockMvc.perform(get("/api/phrases/" + id).session(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.englishText").value("attend a meeting"));
    }

    private static String phrase(String english, String japanese, String example, String category) {
        String exampleJson = example == null ? "null" : "\"" + example + "\"";
        String categoryJson = category == null ? "null" : "\"" + category + "\"";
        return """
                {"englishText":"%s","japaneseText":"%s","exampleSentence":%s,"category":%s}
                """.formatted(english, japanese, exampleJson, categoryJson);
    }
}
