package com.dndmasters.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class CharacterCreationApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String token() throws Exception {
        String username = "hero" + UUID.randomUUID().toString().substring(0, 8);
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", "secret123"));
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void servesRulesetCataloguePerEdition() throws Exception {
        mockMvc.perform(get("/api/rulesets/dnd-5e-2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edition").value("DND_5E_2024"))
                .andExpect(jsonPath("$.species").isArray())
                .andExpect(jsonPath("$.classes").isArray())
                .andExpect(jsonPath("$.backgrounds").isArray());
    }

    @Test
    void createsCustomCharacterApplyingRules() throws Exception {
        String token = token();
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Borin",
                "edition", "dnd-5e-2014",
                "speciesId", "dwarf",
                "classId", "fighter",
                "backgroundId", "soldier",
                "scores", Map.of("strength", 15, "dexterity", 12, "constitution", 14,
                        "intelligence", 10, "wisdom", 10, "charisma", 8),
                "skills", java.util.List.of("athletics", "perception"),
                "armor", "cadena",
                "weapon", "longsword"));

        mockMvc.perform(post("/api/characters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Borin"))
                .andExpect(jsonPath("$.race").value("Enano"))
                .andExpect(jsonPath("$.cls").value("Guerrero"))
                // dwarf +2 CON → 16; human-like test expects this
                .andExpect(jsonPath("$.stats.constitution").value(16))
                // fighter d10 + CON(+3) = 13
                .andExpect(jsonPath("$.maxHp").value(13))
                .andExpect(jsonPath("$.ac").value(16));
    }

    @Test
    void rejectsInvalidCharacterBuild() throws Exception {
        String token = token();
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Cheto",
                "edition", "dnd-5e-2014",
                "speciesId", "human",
                "classId", "fighter",
                "backgroundId", "soldier",
                "scores", Map.of("strength", 15, "dexterity", 15, "constitution", 15,
                        "intelligence", 15, "wisdom", 15, "charisma", 15),
                "skills", java.util.List.of("athletics", "perception")));

        mockMvc.perform(post("/api/characters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsCampaignWithEdition() throws Exception {
        String token = token();
        String body = objectMapper.writeValueAsString(Map.of("name", "Cripta 2024", "edition", "dnd-5e-2024"));
        mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edition").value("DND_5E_2024"));
    }
}
