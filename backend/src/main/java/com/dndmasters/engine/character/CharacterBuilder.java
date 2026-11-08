package com.dndmasters.engine.character;

import com.dndmasters.domain.Ability;
import com.dndmasters.domain.Attack;
import com.dndmasters.domain.InventoryItem;
import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.domain.Stats;
import com.dndmasters.engine.rules.FightingStyleRules;
import com.dndmasters.engine.rules.Modifier;
import com.dndmasters.rules.Edition;
import com.dndmasters.rules.RuleSet;
import com.dndmasters.rules.RuleSetRegistry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Turns a {@link CharacterBuildRequest} into a complete {@link PlayerCharacter},
 * applying SRD rules: ability bonuses, HP, AC, skills, saving throws, attacks and
 * starting equipment. All math happens here; the client and the AI never compute stats.
 */
@Component
public class CharacterBuilder {

    private static final int PROFICIENCY_BONUS = 2; // level 1

    private static final Map<String, ArmorProfile> ARMOR = Map.ofEntries(
            Map.entry("ninguna", new ArmorProfile("Ninguna", 10, true, 0)),
            Map.entry("none", new ArmorProfile("Ninguna", 10, true, 0)),
            Map.entry("cuero", new ArmorProfile("Armadura de cuero", 11, true, 10)),
            Map.entry("leather", new ArmorProfile("Armadura de cuero", 11, true, 10)),
            Map.entry("cuero-tachonado", new ArmorProfile("Cuero tachonado", 12, true, 13)),
            Map.entry("camisote", new ArmorProfile("Camisote de mallas", 13, true, 20)),
            Map.entry("cadena", new ArmorProfile("Cota de mallas", 16, false, 55)),
            Map.entry("chain", new ArmorProfile("Cota de mallas", 16, false, 55)),
            Map.entry("placas", new ArmorProfile("Armadura de placas", 18, false, 65)));

    private static final Map<String, WeaponProfile> WEAPONS = Map.ofEntries(
            Map.entry("longsword", new WeaponProfile("Longsword", "1d8", Ability.STRENGTH, false, 1, false, 3)),
            Map.entry("shortsword", new WeaponProfile("Espada corta", "1d6", Ability.DEXTERITY, false, 1, false, 2)),
            Map.entry("greataxe", new WeaponProfile("Hacha grande", "1d12", Ability.STRENGTH, false, 1, true, 7)),
            Map.entry("handaxe", new WeaponProfile("Hacha de mano", "1d6", Ability.STRENGTH, false, 1, false, 2)),
            Map.entry("warhammer", new WeaponProfile("Martillo de guerra", "1d8", Ability.STRENGTH, false, 1, false, 2)),
            Map.entry("mace", new WeaponProfile("Maza", "1d6", Ability.STRENGTH, false, 1, false, 4)),
            Map.entry("quarterstaff", new WeaponProfile("Bastón", "1d6", Ability.STRENGTH, false, 1, false, 4)),
            Map.entry("dagger", new WeaponProfile("Daga", "1d4", Ability.DEXTERITY, true, 4, false, 1)),
            Map.entry("rapier", new WeaponProfile("Estoque", "1d8", Ability.DEXTERITY, false, 1, false, 2)),
            Map.entry("scimitar", new WeaponProfile("Cimitarra", "1d6", Ability.DEXTERITY, false, 1, false, 3)),
            Map.entry("shortbow", new WeaponProfile("Arco corto", "1d6", Ability.DEXTERITY, true, 16, false, 2)),
            Map.entry("longbow", new WeaponProfile("Arco largo", "1d8", Ability.DEXTERITY, true, 24, false, 2)),
            Map.entry("javelin", new WeaponProfile("Jabalina", "1d6", Ability.STRENGTH, true, 8, false, 2)));

    private final RuleSetRegistry registry;

    public CharacterBuilder(RuleSetRegistry registry) {
        this.registry = registry;
    }

    public PlayerCharacter build(CharacterBuildRequest request) {
        RuleSet set = registry.get(request.edition());
        RuleSet.Species species = request.species(set);
        RuleSet.CharacterClass characterClass = request.characterClass(set);
        RuleSet.Background background = request.background(set);

        validatePointBuy(request);
        List<String> skillIds = validateSkills(request, characterClass, background);

        Map<Ability, Integer> finalScores = applyBonuses(request.baseScores(), species, background);
        Stats stats = toStats(finalScores);

        int conMod = Modifier.of(stats.constitution());
        int maxHp = Math.max(1, hitDieValue(characterClass.hitDie()) + conMod);

        ArmorProfile armor = armorFor(request.armor(), characterClass.startingArmor());
        int ac = armor.flat() || !armor.dexterity()
                ? armor.base()
                : armor.base() + Modifier.of(stats.dexterity());
        String fightingStyle = resolveFightingStyle(request, characterClass);
        ac += FightingStyleRules.defenseBonus(fightingStyle, !"Ninguna".equals(armor.name()));

        String weaponId = request.weapon() == null || request.weapon().isBlank()
                ? characterClass.weapons().get(0)
                : request.weapon();
        WeaponProfile weapon = WEAPONS.getOrDefault(normalize(weaponId), WEAPONS.get("longsword"));
        Attack attack = toAttack(weapon, stats);

        PlayerCharacter character = new PlayerCharacter();
        character.setName(request.name().trim());
        character.setCls(characterClass.name());
        character.setRace(species.name());
        character.setLevel(1);
        character.setHp(maxHp);
        character.setMaxHp(maxHp);
        character.setAc(ac);
        character.setStats(stats);
        character.setSpeed(species.speed() / 5); // grid squares (30ft ≈ 6)
        character.setColor(colorFor(characterClass.id()));
        character.setIcon(iconFor(characterClass.id()));
        character.setAttacks(List.of(attack));
        character.setInventory(startingInventory(characterClass, armor, weapon));
        character.setGold(startingGold(characterClass.id()));
        character.setPredefined(false);

        // Store creation metadata for later (level-up, re-render, verification).
        CharacterProficiencies.Proficiencies proficiencies = CharacterProficiencies.derive(
                request.edition(), species.id(), characterClass.id(), background.id());

        Map<String, Object> creationData = new LinkedHashMap<>();
        creationData.put("edition", request.edition().name());
        creationData.put("speciesId", species.id());
        creationData.put("classId", characterClass.id());
        creationData.put("backgroundId", background.id());
        creationData.put("savingThrows", characterClass.savingThrows());
        creationData.put("skills", skillIds);
        creationData.put("languages", proficiencies.languages());
        creationData.put("armorProficiencies", proficiencies.armor());
        creationData.put("weaponProficiencies", proficiencies.weapons());
        creationData.put("toolProficiencies", proficiencies.tools());
        creationData.put("proficiencyBonus", PROFICIENCY_BONUS);
        creationData.put("hitDie", characterClass.hitDie());
        creationData.put("baseScores", request.baseScores());
        if (fightingStyle != null) {
            creationData.put("fightingStyle", fightingStyle);
        }
        character.setCreationData(creationData);
        return character;
    }

    private String resolveFightingStyle(CharacterBuildRequest request, RuleSet.CharacterClass characterClass) {
        if (request.fightingStyle() == null || request.fightingStyle().isBlank()) {
            return null;
        }
        if (!"fighter".equals(characterClass.id()) && !"paladin".equals(characterClass.id())) {
            throw new IllegalArgumentException("Solo guerreros y paladinos eligen estilo de combate");
        }
        String style = FightingStyleRules.normalize(request.fightingStyle());
        if (!FightingStyleRules.isKnown(style)) {
            throw new IllegalArgumentException("Estilo de combate no válido: " + request.fightingStyle());
        }
        return style;
    }

    private void validatePointBuy(CharacterBuildRequest request) {        if (!PointBuy.isValid(request.baseScores())) {
            throw new IllegalArgumentException("La distribución de puntos no es válida (deben sumar 27)");
        }
        for (Map.Entry<Ability, Integer> entry : request.baseScores().entrySet()) {
            int value = entry.getValue();
            if (value < PointBuy.MIN_SCORE || value > PointBuy.MAX_SCORE) {
                throw new IllegalArgumentException("Característica fuera de rango: " + value);
            }
        }
    }

    private List<String> validateSkills(CharacterBuildRequest request, RuleSet.CharacterClass characterClass,
                                        RuleSet.Background background) {
        List<String> chosen = new ArrayList<>(request.chosenSkills());
        for (String skill : chosen) {
            String normalized = normalize(skill);
            if (!characterClass.skillChoices().contains(normalized)) {
                throw new IllegalArgumentException("Habilidad no permitida para " + characterClass.name() + ": " + skill);
            }
        }
        if (chosen.size() > characterClass.skillChoiceCount()) {
            throw new IllegalArgumentException("Has elegido demasiadas habilidades (máx " + characterClass.skillChoiceCount() + ")");
        }
        List<String> all = new ArrayList<>(background.skills());
        for (String skill : chosen) {
            String normalized = normalize(skill);
            if (!all.contains(normalized)) {
                all.add(normalized);
            }
        }
        return all;
    }

    private Map<Ability, Integer> applyBonuses(Map<Ability, Integer> base, RuleSet.Species species,
                                               RuleSet.Background background) {
        Map<Ability, Integer> result = new EnumMap<>(Ability.class);
        result.putAll(base);
        applyBonusesJson(result, species.abilityBonuses());
        applyBonusesJson(result, background.abilityBonuses());
        return result;
    }

    private void applyBonusesJson(Map<Ability, Integer> scores, Map<String, Integer> bonuses) {
        for (Map.Entry<String, Integer> bonus : bonuses.entrySet()) {
            Ability ability = abilityByName(bonus.getKey());
            if (ability != null) {
                scores.merge(ability, bonus.getValue(), Integer::sum);
            }
        }
    }

    private Stats toStats(Map<Ability, Integer> scores) {
        return new Stats(
                scores.getOrDefault(Ability.STRENGTH, 10),
                scores.getOrDefault(Ability.DEXTERITY, 10),
                scores.getOrDefault(Ability.CONSTITUTION, 10),
                scores.getOrDefault(Ability.INTELLIGENCE, 10),
                scores.getOrDefault(Ability.WISDOM, 10),
                scores.getOrDefault(Ability.CHARISMA, 10));
    }

    private ArmorProfile armorFor(String requested, String classDefault) {
        String key = requested == null || requested.isBlank() ? classDefault : requested;
        return ARMOR.getOrDefault(normalize(key), ARMOR.get("ninguna"));
    }

    private Attack toAttack(WeaponProfile weapon, Stats stats) {
        int abilityMod = Modifier.of(weapon.ability() == Ability.STRENGTH ? stats.strength() : stats.dexterity());
        int bonus = abilityMod + PROFICIENCY_BONUS;
        String damageMod = abilityMod == 0 ? "" : (abilityMod > 0 ? "+" + abilityMod : String.valueOf(abilityMod));
        String expression = weapon.damage() + damageMod;
        return weapon.ranged()
                ? Attack.ranged(weapon.name(), bonus, expression, weapon.range())
                : weapon.twoHanded()
                        ? Attack.meleeTwoHanded(weapon.name(), bonus, expression)
                        : Attack.melee(weapon.name(), bonus, expression);
    }

    private List<InventoryItem> startingInventory(RuleSet.CharacterClass characterClass, ArmorProfile armor,
                                                  WeaponProfile weapon) {
        List<InventoryItem> items = new ArrayList<>();
        if (!"Ninguna".equals(armor.name())) {
            items.add(new InventoryItem(armor.name(), "Protección: CA base " + armor.base(), "shield", 1,
                    armor.weight()));
        }
        items.add(new InventoryItem(weapon.name(), "Arma de " + characterClass.name(), "sword", 1,
                weapon.weight()));
        items.add(new InventoryItem("Poción de Curación", "Recupera 2d4+2 HP.", "flask", 1, 0.5));
        items.add(new InventoryItem("Mochila de aventurero", "Raciones, antorcha y yesca.", "package", 1, 10));
        switch (characterClass.id()) {
            case "wizard", "sorcerer", "warlock" ->
                    items.add(InventoryItem.of("Foco arcano", "Foco para componentes materiales.", 1));
            case "cleric", "paladin" ->
                    items.add(InventoryItem.of("Símbolo sagrado", "Foco para componentes materiales.", 1));
            case "druid", "ranger" ->
                    items.add(InventoryItem.of("Foco druídico", "Foco para componentes materiales.", 1));
            case "bard" ->
                    items.add(InventoryItem.of("Instrumento musical", "Foco para componentes materiales.", 2));
            default -> {
                // Non-casters do not need a spellcasting focus.
            }
        }
        if ("rogue".equals(characterClass.id())) {
            items.add(InventoryItem.of("Herramientas de ladrón", "Abre cerraduras y desactiva trampas.", 1));
        }
        if (usesArrows(weapon)) {
            items.add(InventoryItem.of("Flechas", "Munición para arcos.", "crosshair", 20, 0.05));
        }
        return items;
    }

    /** Bows use arrows; thrown weapons (javelin, dagger) do not consume ammunition. */
    private boolean usesArrows(WeaponProfile weapon) {
        return "Arco corto".equals(weapon.name()) || "Arco largo".equals(weapon.name());
    }

    private int startingGold(String classId) {
        return switch (classId) {
            case "fighter", "paladin" -> 120;
            case "rogue", "cleric" -> 110;
            case "bard", "ranger", "warlock" -> 100;
            case "wizard", "druid", "sorcerer" -> 90;
            case "barbarian" -> 80;
            case "monk" -> 20;
            default -> 100;
        };
    }

    private int hitDieValue(String hitDie) {
        return switch (hitDie.toLowerCase(Locale.ROOT)) {
            case "d6" -> 6;
            case "d8" -> 8;
            case "d10" -> 10;
            case "d12" -> 12;
            default -> 8;
        };
    }

    private Ability abilityByName(String name) {
        return switch (normalize(name)) {
            case "strength", "str", "fue", "fuerza" -> Ability.STRENGTH;
            case "dexterity", "dex", "des", "destreza" -> Ability.DEXTERITY;
            case "constitution", "con", "constitucion" -> Ability.CONSTITUTION;
            case "intelligence", "int", "inteligencia" -> Ability.INTELLIGENCE;
            case "wisdom", "wis", "sab", "sabiduria" -> Ability.WISDOM;
            case "charisma", "cha", "car", "carisma" -> Ability.CHARISMA;
            default -> null;
        };
    }

    private String colorFor(String classId) {
        return switch (classId) {
            case "fighter" -> "#C2410C";
            case "wizard" -> "#7C3AED";
            case "rogue" -> "#0F766E";
            case "cleric" -> "#B45309";
            case "barbarian" -> "#9A3412";
            case "bard" -> "#DB2777";
            case "druid" -> "#15803D";
            case "monk" -> "#0E7490";
            case "paladin" -> "#CA8A04";
            case "ranger" -> "#166534";
            case "sorcerer" -> "#9333EA";
            case "warlock" -> "#6D28D9";
            default -> "#C9A227";
        };
    }

    private String iconFor(String classId) {
        return switch (classId) {
            case "fighter", "paladin", "barbarian" -> "sword";
            case "wizard", "sorcerer", "warlock" -> "sparkles";
            case "rogue", "ranger" -> "dagger";
            case "cleric", "druid" -> "shield";
            case "bard" -> "music";
            case "monk" -> "hand";
            default -> "user";
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(" ", "-").replace("_", "-");
    }

    private record ArmorProfile(String name, int base, boolean dexterity, double weight) {
        boolean flat() {
            return !dexterity;
        }
    }

    private record WeaponProfile(String name, String damage, Ability ability, boolean ranged, int range,
                                 boolean twoHanded, double weight) {
    }
}
