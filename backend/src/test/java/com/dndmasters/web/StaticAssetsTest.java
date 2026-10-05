package com.dndmasters.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.TestPropertySource;

/** Ensures the bundled placeholder art exists so the UI never shows broken images. */
@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/dnd-test.db")
class StaticAssetsTest {

    @Autowired
    ResourceLoader resourceLoader;

    @ParameterizedTest
    @ValueSource(strings = {
            "classpath:static/images/campaigns/valdros.png",
            "classpath:static/images/heroes/kael.png",
            "classpath:static/images/dm/dungeon-master.png",
            "classpath:static/images/scenes/valdros-0.png",
            "classpath:static/images/enemies/goblin.png",
            "classpath:static/images/enemies/guardian.png"
    })
    void placeholderImagesExist(String location) {
        Resource resource = resourceLoader.getResource(location);
        assertThat(resource.exists()).as(location).isTrue();
    }

    @Test
    void allCampaignImageReferencesResolve() {
        for (String path : new String[]{
                "campaigns/valdros.png", "scenes/valdros-0.png", "scenes/valdros-1.png",
                "scenes/valdros-2.png", "heroes/kael.png", "heroes/lyra.png", "heroes/aria.png",
                "heroes/thorin.png"}) {
            assertThat(resourceLoader.getResource("classpath:static/images/" + path).exists())
                    .as(path).isTrue();
        }
    }
}
