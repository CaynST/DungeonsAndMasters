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
@Table(name = "dice_rolls")
@Getter
@Setter
@NoArgsConstructor
public class DiceRollEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String expression;

    @Column(name = "rolls_json", nullable = false)
    private String rollsJson = "[]";

    @Column(nullable = false)
    private int modifier;

    @Column(nullable = false)
    private int total;

    private boolean natural20;
    private boolean natural1;

    private String purpose;

    @Column(name = "created_at", nullable = false)
    private String createdAt;
}
