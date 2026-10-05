package com.dndmasters.domain;

public record Scene(
        Long id,
        Long campaignId,
        int orderIndex,
        String title,
        String description,
        String kind,
        String imagePrompt,
        String imageUrl) {
}
