package com.dndmasters.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
class GameFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String register() throws Exception {
        String username = "hero" + UUID.randomUUID().toString().substring(0, 8);
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", "secret123"));
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createCampaign(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Test Cripta\"}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void predefinedCharactersAreAvailable() throws Exception {
        mockMvc.perform(get("/api/characters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    @Test
    void fullIntroFlowPlacesPartyAndRolesDice() throws Exception {
        String token = register();
        long campaignId = createCampaign(token);

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        MvcResult start = mockMvc.perform(post("/api/campaigns/" + campaignId + "/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.narration").isNotEmpty())
                .andReturn();
        JsonNode startJson = objectMapper.readTree(start.getResponse().getContentAsString());
        assertThat(startJson.get("checkRequest").get("skill").asText()).isEqualTo("perception");

        MvcResult state = mockMvc.perform(get("/api/campaigns/" + campaignId + "/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scene.orderIndex").value(0))
                .andExpect(jsonPath("$.map.width").isNumber())
                .andReturn();
        JsonNode stateJson = objectMapper.readTree(state.getResponse().getContentAsString());
        // Only the joined player's character plus the scene's NPC — no injected demo heroes.
        assertThat(stateJson.get("entities").size()).isGreaterThanOrEqualTo(2);

        // Roll a die through the API.
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/actions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DICE\",\"expression\":\"1d20+5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.roll.total").isNumber());
    }

    @Test
    void movementIsValidated() throws Exception {
        String token = register();
        long campaignId = createCampaign(token);
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/join")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/start")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        MvcResult state = mockMvc.perform(get("/api/campaigns/" + campaignId + "/state"))
                .andReturn();
        JsonNode entities = objectMapper.readTree(state.getResponse().getContentAsString()).get("entities");
        long playerEntityId = -1;
        for (JsonNode entity : entities) {
            if ("PLAYER".equals(entity.get("kind").asText())
                    && "PLAYER".equals(entity.path("data").path("controller").asText())) {
                playerEntityId = entity.get("id").asLong();
                break;
            }
        }
        assertThat(playerEntityId).isPositive();

        // Wall cell (0,0) must be rejected.
        String intoWall = objectMapper.writeValueAsString(Map.of(
                "type", "MOVE", "actorEntityId", playerEntityId,
                "path", List.of(Map.of("x", 0, "y", 0))));
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/actions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(intoWall))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(false));
    }
}
