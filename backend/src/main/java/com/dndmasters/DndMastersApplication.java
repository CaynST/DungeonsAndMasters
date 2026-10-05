package com.dndmasters;

import java.io.File;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DndMastersApplication {

    public static void main(String[] args) {
        // SQLite does not create missing parent directories for its file.
        new File("data").mkdirs();
        SpringApplication.run(DndMastersApplication.class, args);
    }
}
