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
@Table(name = "combat_states")
@Getter
@Setter
@NoArgsConstructor
public class CombatStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "scene_id")
    private Long sceneId;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int round;

    @Column(name = "current_turn_index", nullable = false)
    private int currentTurnIndex;

    @Column(nullable = false)
    private String status = "NOT_STARTED";

    @Column(name = "started_at")
    private String startedAt;

    @Column(name = "ended_at")
    private String endedAt;
}
