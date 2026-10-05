package com.dndmasters.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 32) String username,
            @NotBlank @Size(min = 4, max = 64) String password,
            @Size(max = 48) String displayName) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record UserView(Long id, String username, String displayName, String avatar) {
    }

    public record AuthResponse(String token, UserView user) {
    }
}
