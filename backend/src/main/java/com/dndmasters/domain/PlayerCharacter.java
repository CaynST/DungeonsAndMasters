package com.dndmasters.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCharacter {

    private Long id;
    private Long ownerId;
    private String name;
    private String cls;
    private String race;
    private int level = 1;
    private int hp;
    private int maxHp;
    private int ac;
    private Stats stats;
    private int speed = 6;
    private String color;
    private String icon;
    private String portrait;
    private List<Attack> attacks = new ArrayList<>();
    private List<InventoryItem> inventory = new ArrayList<>();
    private boolean predefined;
    private Map<String, Object> creationData = new HashMap<>();
    private int gold;
    private int silver;
    private int copper;

    /** Total wealth expressed in gold pieces (1 gp = 10 sp = 100 cp). */
    public double totalGold() {
        return gold + silver / 10.0 + copper / 100.0;
    }

    public boolean alive() {
        return hp > 0;
    }
}
