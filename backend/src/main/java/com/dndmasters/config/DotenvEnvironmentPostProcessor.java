package com.dndmasters.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads a local {@code .env} file into the Spring environment at startup, so the app can
 * be run with a plain {@code mvn spring-boot:run} (working directory {@code backend/})
 * without exporting variables by hand.
 *
 * <p>Real environment variables and JVM system properties always win: the dotenv values
 * are added with the lowest precedence. Missing files are ignored.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Tests must never hit the network; Surefire sets this flag.
        if (Boolean.getBoolean("dotenv.disabled") || "true".equalsIgnoreCase(System.getenv("DOTENV_DISABLED"))) {
            return;
        }
        Path file = locate();
        if (file == null) {
            return;
        }
        Map<String, String> values = DotenvParser.parse(file);
        if (values.isEmpty()) {
            return;
        }
        // addLast → the real environment and system properties keep precedence.
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, Map.copyOf(values)));
    }

    /** Looks for {@code .env} in the working directory and in its parent (repo root). */
    private Path locate() {
        String configured = System.getProperty("dotenv.path", System.getenv("DOTENV_PATH"));
        if (configured != null && !configured.isBlank()) {
            Path path = Paths.get(configured);
            return Files.isRegularFile(path) ? path : null;
        }
        Path working = Paths.get(".env").toAbsolutePath();
        if (Files.isRegularFile(working)) {
            return working;
        }
        Path parent = Paths.get("..", ".env").toAbsolutePath();
        return Files.isRegularFile(parent) ? parent : null;
    }

    @Override
    public int getOrder() {
        // Run after config file processing so we can add the lowest-precedence source.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
