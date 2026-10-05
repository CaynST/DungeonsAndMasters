package com.dndmasters.web;

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
class ShopApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String token() throws Exception {
        String username = "shop" + UUID.randomUUID().toString().substring(0, 8);
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", "secret123"));
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createCharacter(String token) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Comerciante",
                "edition", "dnd-5e-2014",
                "speciesId", "human",
                "classId", "fighter",
                "backgroundId", "soldier",
                "scores", Map.of("strength", 15, "dexterity", 12, "constitution", 14,
                        "intelligence", 10, "wisdom", 10, "charisma", 8),
                "skills", List.of("athletics", "perception"),
                "armor", "leather",
                "weapon", "longsword"));
        MvcResult result = mockMvc.perform(post("/api/characters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(120))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void buysAndSellsItemsUpdatingGoldAndInventory() throws Exception {
        String token = token();
        long characterId = createCharacter(token);

        MvcResult campaign = mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Mercado\"}"))
                .andReturn();
        long campaignId = objectMapper.readTree(campaign.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("characterId", characterId))))
                .andExpect(status().isOk());

        // Shop shows the catalogue and the character's wealth.
        mockMvc.perform(get("/api/campaigns/" + campaignId + "/shop")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(120))
                .andExpect(jsonPath("$.items").isArray());

        // Buy a 50 gp potion → gold drops to 70.
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/shop/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"potion-healing\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(70));

        // Sell it back → half price (25) → gold 95.
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/shop/sell")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"Poción de Curación\",\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(95));
    }

    @Test
    void equippingArmorChangesAc() throws Exception {
        String token = token();
        long characterId = createCharacter(token);
        MvcResult campaign = mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Armería\"}"))
                .andReturn();
        long campaignId = objectMapper.readTree(campaign.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("characterId", characterId))))
                .andExpect(status().isOk());

        // Buy chain mail (75 gp) then equip it → flat AC 16.
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/shop/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"chain\",\"quantity\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/shop/equip")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"Cota de mallas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ac").value(16));
    }

    @Test
    void rejectsPurchaseWithoutEnoughGold() throws Exception {
        String token = token();
        long characterId = createCharacter(token);
        MvcResult campaign = mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Caro\"}"))
                .andReturn();
        long campaignId = objectMapper.readTree(campaign.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("characterId", characterId))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/shop/buy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"plate\",\"quantity\":1}"))
                .andExpect(status().isBadRequest());
    }
}
