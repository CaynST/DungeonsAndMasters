package com.dndmasters.image;

import org.springframework.stereotype.Component;

/** Returns bundled placeholder art; the hash keeps the choice stable per prompt. */
@Component
public class PlaceholderImageProvider implements ImageProvider {

    private static final String[] PLACEHOLDERS = {
            "/images/scenes/placeholder-crypt.png",
            "/images/scenes/placeholder-tavern.png",
            "/images/scenes/placeholder-battle.png",
            "/images/scenes/placeholder-boss.png"
    };

    @Override
    public String id() {
        return "placeholder";
    }

    @Override
    public String imageFor(String prompt, String fallback) {
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        if (prompt == null || prompt.isBlank()) {
            return PLACEHOLDERS[0];
        }
        int index = Math.floorMod(prompt.hashCode(), PLACEHOLDERS.length);
        return PLACEHOLDERS[index];
    }
}
