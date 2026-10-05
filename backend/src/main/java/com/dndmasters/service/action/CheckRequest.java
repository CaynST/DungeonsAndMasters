package com.dndmasters.service.action;

public record CheckRequest(String skill, int dc, Long actorEntityId, String prompt) {
}
