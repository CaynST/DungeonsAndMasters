package com.dndmasters.economy;

import com.dndmasters.domain.Ability;
import com.dndmasters.domain.Attack;
import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.engine.rules.Modifier;
import com.dndmasters.persistence.entity.CharacterEntity;
import com.dndmasters.persistence.mapper.CharacterMapper;
import com.dndmasters.persistence.repository.CharacterRepository;
import com.dndmasters.service.CampaignService;
import com.dndmasters.service.GameEventService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Equips owned items and recomputes AC and attacks accordingly. */
@Service
public class EquipmentService {

    private static final int PROFICIENCY = 2;

    private record ArmorProfile(String name, int base, boolean dexterity) {
    }

    private record WeaponProfile(String name, String damage, Ability ability, boolean ranged, int range) {
    }

    private static final Map<String, ArmorProfile> ARMOR = Map.ofEntries(
            Map.entry("leather", new ArmorProfile("Armadura de cuero", 11, true)),
            Map.entry("studded", new ArmorProfile("Cuero tachonado", 12, true)),
            Map.entry("chain", new ArmorProfile("Cota de mallas", 16, false)),
            Map.entry("plate", new ArmorProfile("Armadura de placas", 18, false)));

    private static final Map<String, WeaponProfile> WEAPONS = Map.ofEntries(
            Map.entry("longsword", new WeaponProfile("Espada larga", "1d8", Ability.STRENGTH, false, 1)),
            Map.entry("shortsword", new WeaponProfile("Espada corta", "1d6", Ability.DEXTERITY, false, 1)),
            Map.entry("greataxe", new WeaponProfile("Hacha grande", "1d12", Ability.STRENGTH, false, 1)),
            Map.entry("warhammer", new WeaponProfile("Martillo de guerra", "1d8", Ability.STRENGTH, false, 1)),
            Map.entry("mace", new WeaponProfile("Maza", "1d6", Ability.STRENGTH, false, 1)),
            Map.entry("dagger", new WeaponProfile("Daga", "1d4", Ability.DEXTERITY, true, 4)),
            Map.entry("rapier", new WeaponProfile("Estoque", "1d8", Ability.DEXTERITY, false, 1)),
            Map.entry("shortbow", new WeaponProfile("Arco corto", "1d6", Ability.DEXTERITY, true, 16)),
            Map.entry("longbow", new WeaponProfile("Arco largo", "1d8", Ability.DEXTERITY, true, 24)));

    private static final Set<String> WEAPON_NAMES = WEAPONS.values().stream().map(WeaponProfile::name)
            .collect(java.util.stream.Collectors.toSet());

    private final CampaignService campaigns;
    private final CharacterRepository characters;
    private final CharacterMapper mapper;
    private final GameEventService events;

    public EquipmentService(CampaignService campaigns, CharacterRepository characters, CharacterMapper mapper,
                            GameEventService events) {
        this.campaigns = campaigns;
        this.characters = characters;
        this.mapper = mapper;
        this.events = events;
    }

    @Transactional
    public PlayerCharacter equip(Long campaignId, Long userId, String itemName) {
        PlayerCharacter character = characterOf(campaignId, userId);
        ItemCatalog.ItemDef item = ItemCatalog.byName(itemName);
        if (item == null || (!"WEAPON".equals(item.kind()) && !"ARMOR".equals(item.kind()))) {
            throw new IllegalStateException("Ese objeto no se puede equipar");
        }
        boolean owned = character.getInventory().stream().anyMatch(i -> i.name().equalsIgnoreCase(itemName));
        if (!owned) {
            throw new IllegalStateException("No tienes " + itemName);
        }
        Map<String, Object> data = new HashMap<>(character.getCreationData());
        if ("WEAPON".equals(item.kind())) {
            data.put("equippedWeapon", item.id());
        } else if ("shield".equals(item.id())) {
            data.put("equippedShield", true);
        } else {
            data.put("equippedArmor", item.id());
        }
        character.setCreationData(data);
        recompute(character);
        save(character);
        events.record(campaignId, null, "EQUIP", character.getName(),
                character.getName() + " equipa " + item.name() + ".", Map.of());
        return character;
    }

    @Transactional
    public PlayerCharacter unequip(Long campaignId, Long userId, String slot) {
        PlayerCharacter character = characterOf(campaignId, userId);
        Map<String, Object> data = new HashMap<>(character.getCreationData());
        switch (slot) {
            case "weapon" -> data.remove("equippedWeapon");
            case "shield" -> data.remove("equippedShield");
            default -> data.remove("equippedArmor");
        }
        character.setCreationData(data);
        recompute(character);
        save(character);
        return character;
    }

    private void recompute(PlayerCharacter character) {
        Map<String, Object> data = character.getCreationData();
        String armorId = (String) data.get("equippedArmor");
        String weaponId = (String) data.get("equippedWeapon");
        boolean shield = Boolean.TRUE.equals(data.get("equippedShield"));

        int dexMod = Modifier.of(character.getStats().dexterity());
        int ac = 10 + dexMod;
        if (armorId != null && ARMOR.containsKey(armorId)) {
            ArmorProfile armor = ARMOR.get(armorId);
            ac = armor.dexterity() ? armor.base() + dexMod : armor.base();
        }
        if (shield) {
            ac += 2;
        }
        character.setAc(ac);

        if (weaponId != null && WEAPONS.containsKey(weaponId)) {
            WeaponProfile weapon = WEAPONS.get(weaponId);
            int abilityMod = Modifier.of(
                    weapon.ability() == Ability.STRENGTH ? character.getStats().strength() : character.getStats().dexterity());
            int bonus = abilityMod + PROFICIENCY;
            String damage = weapon.damage() + (abilityMod == 0 ? "" : (abilityMod > 0 ? "+" + abilityMod : String.valueOf(abilityMod)));
            Attack equipped = weapon.ranged()
                    ? Attack.ranged(weapon.name(), bonus, damage, weapon.range())
                    : Attack.melee(weapon.name(), bonus, damage);

            List<Attack> attacks = new ArrayList<>();
            attacks.add(equipped);
            // Keep non-weapon attacks (spells, natural attacks).
            character.getAttacks().stream().filter(a -> !WEAPON_NAMES.contains(a.name())).forEach(attacks::add);
            character.setAttacks(attacks);
        }
    }

    private PlayerCharacter characterOf(Long campaignId, Long userId) {
        Long characterId = campaigns.characterIdForUser(campaignId, userId);
        if (characterId == null) {
            throw new IllegalStateException("No tienes un personaje en esta campaña");
        }
        CharacterEntity entity = characters.findById(characterId)
                .orElseThrow(() -> new IllegalStateException("Personaje no encontrado"));
        return mapper.toDomain(entity);
    }

    private void save(PlayerCharacter character) {
        CharacterEntity entity = mapper.toEntity(character);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(java.time.Instant.now().toString());
        }
        characters.save(entity);
    }
}
