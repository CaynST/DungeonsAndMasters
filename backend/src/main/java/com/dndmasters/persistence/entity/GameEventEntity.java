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
@Table(name = "game_events")
@Getter
@Setter
@NoArgsConstructor
public class GameEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "scene_id")
    private Long sceneId;

    @Column(nullable = false)
    private long sequence;

    @Column(nullable = false)
    private String type;

    private String actor;

    @Column(nullable = false)
    private String message;

    @Column(name = "data_json", nullable = false)
    private String dataJson = "{}";

    @Column(name = "created_at", nullable = false)
    private String createdAt;
}
