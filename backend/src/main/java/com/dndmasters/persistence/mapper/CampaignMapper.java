package com.dndmasters.persistence.mapper;

import com.dndmasters.domain.Campaign;
import com.dndmasters.domain.CampaignStatus;
import com.dndmasters.domain.GamePhase;
import com.dndmasters.persistence.entity.CampaignEntity;
import org.springframework.stereotype.Component;

@Component
public class CampaignMapper {

    public Campaign toDomain(CampaignEntity e) {
        Campaign c = new Campaign();
        c.setId(e.getId());
        c.setName(e.getName());
        c.setSetting(e.getSetting());
        c.setDescription(e.getDescription());
        c.setDmName(e.getDmName());
        c.setDmAvatar(e.getDmAvatar());
        c.setImage(e.getImage());
        c.setStatus(CampaignStatus.valueOf(e.getStatus()));
        c.setPhase(GamePhase.valueOf(e.getPhase()));
        c.setActiveSceneId(e.getActiveSceneId());
        c.setCreatedBy(e.getCreatedBy());
        c.setEdition(e.getEdition());
        c.setPremise(e.getPremise());
        c.setVillain(e.getVillain());
        c.setObjective(e.getObjective());
        c.setSpotlightIndex(e.getSpotlightIndex());
        c.setShopOpen(e.isShopOpen());
        c.setCreatedAt(e.getCreatedAt());
        c.setUpdatedAt(e.getUpdatedAt());
        return c;
    }
}
