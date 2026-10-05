package com.dndmasters.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
public class CampaignEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String setting;
    private String description;

    @Column(name = "dm_name", nullable = false)
    private String dmName = "El Dungeon Master";

    @Column(name = "dm_avatar")
    private String dmAvatar;

    private String image;

    @Column(nullable = false)
    private String status = "LOBBY";

    @Column(nullable = false)
    private String phase = "INTRO";

    @Column(nullable = false)
    private String edition = "DND_5E_2014";

    @Column(name = "active_scene_id")
    private Long activeSceneId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;
}
