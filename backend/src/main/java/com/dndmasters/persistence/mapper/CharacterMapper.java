package com.dndmasters.persistence.mapper;

import com.dndmasters.domain.Attack;
import com.dndmasters.domain.InventoryItem;
import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.domain.Stats;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.util.Json;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CharacterMapper {

    public PlayerCharacter toDomain(CharacterEntity e) {
        PlayerCharacter c = new PlayerCharacter();
        c.setId(e.getId());
        c.setOwnerId(e.getOwnerId());
        c.setName(e.getName());
        c.setCls(e.getCls());
        c.setRace(e.getRace());
        c.setLevel(e.getLevel());
        c.setHp(e.getHp());
        c.setMaxHp(e.getMaxHp());
        c.setAc(e.getAc());
        c.setStats(new Stats(e.getStrength(), e.getDexterity(), e.getConstitution(),
                e.getIntelligence(), e.getWisdom(), e.getCharisma()));
        c.setSpeed(e.getSpeed());
        c.setColor(e.getColor());
        c.setIcon(e.getIcon());
        c.setPortrait(e.getPortrait());
        c.setAttacks(Json.readList(e.getAttacksJson(), Attack.class));
        c.setInventory(Json.readList(e.getInventoryJson(), InventoryItem.class));
        c.setCreationData(Json.readMap(e.getCreationDataJson()));
        c.setPredefined(e.isPredefined());
        return c;
    }

    public CharacterEntity toEntity(PlayerCharacter c) {
        CharacterEntity e = new CharacterEntity();
        e.setId(c.getId());
        e.setOwnerId(c.getOwnerId());
        e.setName(c.getName());
        e.setCls(c.getCls());
        e.setRace(c.getRace());
        e.setLevel(c.getLevel());
        e.setHp(c.getHp());
        e.setMaxHp(c.getMaxHp());
        e.setAc(c.getAc());
        Stats s = c.getStats();
        e.setStrength(s.strength());
        e.setDexterity(s.dexterity());
        e.setConstitution(s.constitution());
        e.setIntelligence(s.intelligence());
        e.setWisdom(s.wisdom());
        e.setCharisma(s.charisma());
        e.setSpeed(c.getSpeed());
        e.setColor(c.getColor());
        e.setIcon(c.getIcon());
        e.setPortrait(c.getPortrait());
        e.setAttacksJson(Json.write(c.getAttacks() == null ? List.of() : c.getAttacks()));
        e.setInventoryJson(Json.write(c.getInventory() == null ? List.of() : c.getInventory()));
        e.setCreationDataJson(Json.write(c.getCreationData() == null ? java.util.Map.of() : c.getCreationData()));
        e.setPredefined(c.isPredefined());
        return e;
    }
}
