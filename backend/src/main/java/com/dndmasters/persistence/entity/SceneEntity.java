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
@Table(name = "scenes")
@Getter
@Setter
@NoArgsConstructor
public class SceneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String kind = "NARRATIVE";

    @Column(name = "image_prompt")
    private String imagePrompt;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "map_id")
    private Long mapId;
}
