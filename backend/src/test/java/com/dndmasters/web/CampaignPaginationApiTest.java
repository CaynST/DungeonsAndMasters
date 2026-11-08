package com.dndmasters.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dndmasters.persistence.entity.CampaignEntity;
import com.dndmasters.persistence.repository.CampaignRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
class CampaignPaginationApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CampaignRepository campaigns;

    private String register() throws Exception {
        String username = "pager" + UUID.randomUUID().toString().substring(0, 8);
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", "secret123"));
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createCampaign(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/campaigns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private JsonNode page(String query) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/campaigns/page?" + query))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private boolean containsId(JsonNode page, long id) {
        for (JsonNode campaign : page.get("content")) {
            if (campaign.get("id").asLong() == id) {
                return true;
            }
        }
        return false;
    }

    @Test
    void keepsTheLegacyListEndpointAsAPlainArray() throws Exception {
        mockMvc.perform(get("/api/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void paginatesNewestFirstWithoutDroppingCampaigns() throws Exception {
        String token = register();
        long before = page("page=0&size=1").get("totalElements").asLong();

        long older = createCampaign(token, "Paginada vieja " + UUID.randomUUID());
        long newer = createCampaign(token, "Paginada nueva " + UUID.randomUUID());

        JsonNode firstPage = page("page=0&size=2");
        assertThat(firstPage.get("content").size()).isLessThanOrEqualTo(2);
        assertThat(firstPage.get("page").asInt()).isZero();
        assertThat(firstPage.get("size").asInt()).isEqualTo(2);
        assertThat(firstPage.get("totalElements").asLong()).isGreaterThanOrEqualTo(before + 2);
        assertThat(firstPage.get("totalPages").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(firstPage.get("first").asBoolean()).isTrue();
        assertThat(firstPage.get("last").asBoolean()).isFalse();
        assertThat(firstPage.get("content").get(0).get("id").asLong()).isEqualTo(newer);
        assertThat(firstPage.get("content").get(1).get("id").asLong()).isEqualTo(older);

        JsonNode secondPage = page("page=1&size=2");
        assertThat(secondPage.get("page").asInt()).isEqualTo(1);
        assertThat(secondPage.get("first").asBoolean()).isFalse();
        assertThat(secondPage.get("totalElements").asLong()).isEqualTo(firstPage.get("totalElements").asLong());
    }

    @Test
    void filtersByStatus() throws Exception {
        String token = register();
        long finishedId = createCampaign(token, "Terminada " + UUID.randomUUID());
        CampaignEntity entity = campaigns.findById(finishedId).orElseThrow();
        entity.setStatus("FINISHED");
        campaigns.save(entity);

        assertThat(containsId(page("status=FINISHED&size=100"), finishedId)).isTrue();
        assertThat(containsId(page("status=LOBBY&size=100"), finishedId)).isFalse();
        assertThat(containsId(page("status=all&size=100"), finishedId)).isTrue();
    }

    @Test
    void rejectsInvalidPaginationAndStatus() throws Exception {
        mockMvc.perform(get("/api/campaigns/page?size=0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/campaigns/page?page=-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/campaigns/page?status=NOPE"))
                .andExpect(status().isBadRequest());
    }
}
