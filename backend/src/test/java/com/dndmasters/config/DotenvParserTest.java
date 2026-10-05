package com.dndmasters.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotenvParserTest {

    @Test
    void parsesKeyValuePairs(@TempDir Path dir) throws Exception {
        Path file = dir.resolve(".env");
        Files.writeString(file, """
                # comment
                AI_PROVIDER=opencode-go
                AI_MODEL=mimo-v2.6-flash

                AI_FALLBACK_MODELS=a,b,c
                """);

        Map<String, String> values = DotenvParser.parse(file);

        assertThat(values)
                .containsEntry("AI_PROVIDER", "opencode-go")
                .containsEntry("AI_MODEL", "mimo-v2.6-flash")
                .containsEntry("AI_FALLBACK_MODELS", "a,b,c")
                .hasSize(3);
    }

    @Test
    void stripsQuotesAndTrimsWhitespace(@TempDir Path dir) throws Exception {
        Path file = dir.resolve(".env");
        Files.writeString(file, """
                KEY1="quoted value"
                KEY2='single quoted'
                KEY3=  spaced
                """);
        Map<String, String> values = DotenvParser.parse(file);
        assertThat(values)
                .containsEntry("KEY1", "quoted value")
                .containsEntry("KEY2", "single quoted")
                .containsEntry("KEY3", "spaced");
    }

    @Test
    void ignoresCommentsBlanksAndMalformedLines(@TempDir Path dir) throws Exception {
        Path file = dir.resolve(".env");
        Files.writeString(file, """
                # a comment

                NO_EQUALS_HERE
                =emptykey
                VALID=ok
                """);
        Map<String, String> values = DotenvParser.parse(file);
        assertThat(values).containsExactly(Map.entry("VALID", "ok"));
    }

    @Test
    void returnsEmptyMapForMissingFile(@TempDir Path dir) {
        assertThat(DotenvParser.parse(dir.resolve("does-not-exist"))).isEmpty();
    }
}
