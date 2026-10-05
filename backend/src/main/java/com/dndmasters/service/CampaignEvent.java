package com.dndmasters.service;

import com.dndmasters.domain.GameEvent;

/** Published on the Spring event bus whenever a campaign event is recorded. */
public record CampaignEvent(Long campaignId, GameEvent event) {
}
