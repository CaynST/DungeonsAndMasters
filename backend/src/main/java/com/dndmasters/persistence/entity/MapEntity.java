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
@Table(name = "maps")
@Getter
@Setter
@NoArgsConstructor
public class MapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "tile_size", nullable = false)
    private int tileSize = 48;

    @Column(name = "walls_json", nullable = false)
    private String wallsJson = "[]";

    @Column(name = "doors_json", nullable = false)
    private String doorsJson = "[]";

    @Column(name = "obstacles_json", nullable = false)
    private String obstaclesJson = "[]";

    @Column(name = "decor_json", nullable = false)
    private String decorJson = "[]";
}
