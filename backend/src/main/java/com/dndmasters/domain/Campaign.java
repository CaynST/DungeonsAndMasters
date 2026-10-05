package com.dndmasters.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {

    private Long id;
    private String name;
    private String setting;
    private String description;
    private String dmName = "El Dungeon Master";
    private String dmAvatar;
    private String image;
    private CampaignStatus status = CampaignStatus.LOBBY;
    private GamePhase phase = GamePhase.INTRO;
    private Long activeSceneId;
    private Long createdBy;
    private String edition = "DND_5E_2014";
    private String premise;
    private String villain;
    private String objective;
    private int spotlightIndex;
    private boolean shopOpen;
    private String createdAt;
    private String updatedAt;

    private List<CampaignMember> members = new ArrayList<>();

    public record CampaignMember(
            Long id,
            Long userId,
            String displayName,
            Long characterId,
            String characterName,
            String role,
            boolean ready) {
    }
}
