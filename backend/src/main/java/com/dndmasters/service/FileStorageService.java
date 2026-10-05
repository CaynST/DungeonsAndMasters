package com.dndmasters.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Stores uploaded images (character portraits) under {@code data/uploads} and serves them. */
@Service
public class FileStorageService {

    private static final long MAX_BYTES = 2 * 1024 * 1024; // 2 MB

    private final Path root = Paths.get("data", "uploads").toAbsolutePath();

    public String storePortrait(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten imágenes");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen supera 2 MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        String filename = safe(prefix) + "-" + UUID.randomUUID() + extension;
        try {
            Files.createDirectories(root);
            Files.copy(file.getInputStream(), root.resolve(filename));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen");
        }
        return "/uploads/" + filename;
    }

    private String extensionOf(String original) {
        if (original == null) {
            return ".png";
        }
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot).toLowerCase() : ".png";
        return ext.matches("\\.(png|jpg|jpeg|webp|gif)") ? ext : ".png";
    }

    private String safe(String value) {
        return value == null ? "img" : value.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
    }
}
