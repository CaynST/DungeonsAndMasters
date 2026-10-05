package com.dndmasters;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/dnd-test.db"
})
class DndMastersApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring context, JPA, and Flyway migrations all initialize.
    }
}
