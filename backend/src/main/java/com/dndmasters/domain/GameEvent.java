package com.dndmasters.domain;

public record GameEvent(
        Long id,
        Long campaignId,
        Long sceneId,
        long sequence,
        String type,
        String actor,
        String message,
        String dataJson,
        String createdAt) {
}
