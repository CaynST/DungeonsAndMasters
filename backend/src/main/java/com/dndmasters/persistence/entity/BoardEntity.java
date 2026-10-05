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
@Table(name = "board_entities")
@Getter
@Setter
@NoArgsConstructor
public class BoardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "map_id", nullable = false)
    private Long mapId;

    @Column(nullable = false)
    private String kind;

    @Column(name = "ref_id")
    private Long refId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int x;

    @Column(nullable = false)
    private int y;

    @Column(nullable = false)
    private int hp;

    @Column(name = "max_hp", nullable = false)
    private int maxHp;

    @Column(nullable = false)
    private int ac;

    @Column(nullable = false)
    private int speed = 6;

    private String color;
    private String icon;
    private String portrait;

    @Column(nullable = false)
    private boolean alive = true;

    @Column(name = "data_json", nullable = false)
    private String dataJson = "{}";
}
