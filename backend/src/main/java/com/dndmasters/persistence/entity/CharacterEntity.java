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
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
public class CharacterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String cls;

    @Column(nullable = false)
    private String race;

    private int level = 1;
    private int hp;
    private int maxHp;
    private int ac;
    private int strength;
    private int dexterity;
    private int constitution;
    private int intelligence;
    private int wisdom;
    private int charisma;
    private int speed = 6;
    private String color;
    private String icon;
    private String portrait;

    @Column(name = "attacks_json", nullable = false)
    private String attacksJson = "[]";

    @Column(name = "inventory_json", nullable = false)
    private String inventoryJson = "[]";

    private boolean predefined;

    @Column(name = "created_at", nullable = false)
    private String createdAt;
}
