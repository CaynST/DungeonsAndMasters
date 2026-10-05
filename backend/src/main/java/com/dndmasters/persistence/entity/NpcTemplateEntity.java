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
@Table(name = "npc_templates")
@Getter
@Setter
@NoArgsConstructor
public class NpcTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(nullable = false)
    private String name;

    private String description;
    private String portrait;

    @Column(nullable = false)
    private String disposition = "NEUTRAL";

    @Column(name = "dialogue_json", nullable = false)
    private String dialogueJson = "[]";
}
