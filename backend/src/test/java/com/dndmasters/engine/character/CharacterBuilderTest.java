package com.dndmasters.engine.character;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dndmasters.domain.Ability;
import com.dndmasters.domain.Attack;
import com.dndmasters.domain.PlayerCharacter;
import com.dndmasters.domain.Stats;
import com.dndmasters.rules.Edition;
import com.dndmasters.rules.RuleSetRegistry;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CharacterBuilderTest {

    private final CharacterBuilder builder = new CharacterBuilder(new RuleSetRegistry());

    private Map<Ability, Integer> scores(int str, int dex, int con, int intel, int wis, int cha) {
        Map<Ability, Integer> map = new EnumMap<>(Ability.class);
        map.put(Ability.STRENGTH, str);
        map.put(Ability.DEXTERITY, dex);
        map.put(Ability.CONSTITUTION, con);
        map.put(Ability.INTELLIGENCE, intel);
        map.put(Ability.WISDOM, wis);
        map.put(Ability.CHARISMA, cha);
        return map;
    }

    @Test
    void appliesSpeciesAbilityBonusesIn2014() {
        CharacterBuildRequest request = new CharacterBuildRequest(
                "Borin", Edition.DND_5E_2014, "dwarf", "fighter", "soldier",
                scores(15, 12, 14, 10, 10, 8), List.of("athletics", "perception"), "cadena", "longsword");

        PlayerCharacter character = builder.build(request);

        // Dwarf grants +2 CON → 14 + 2 = 16
        assertThat(character.getStats().constitution()).isEqualTo(16);
        assertThat(character.getRace()).isEqualTo("Enano");
        assertThat(character.getCls()).isEqualTo("Guerrero");
    }

    @Test
    void computesHpFromHitDieAndConstitution() {
        // Fighter d10, CON 14 (+2) → 10 + 2 = 12
        CharacterBuildRequest request = new CharacterBuildRequest(
                "Kael", Edition.DND_5E_2014, "human", "fighter", "soldier",
                scores(15, 12, 14, 10, 10, 8), List.of("athletics", "perception"), "cadena", "longsword");

        PlayerCharacter character = builder.build(request);
        assertThat(character.getMaxHp()).isEqualTo(12);
        assertThat(character.getHp()).isEqualTo(12);
    }

    @Test
    void computesArmorClassForArmorTypes() {
        // No armor: 10 + DEX. High-elf gives +2 DEX → 14+2=16 → +3 → AC 13.
        CharacterBuildRequest wizard = new CharacterBuildRequest(
                "Lyra", Edition.DND_5E_2014, "high-elf", "wizard", "sage",
                scores(8, 14, 12, 15, 10, 10), List.of("arcana", "history"), "ninguna", "quarterstaff");
        assertThat(builder.build(wizard).getAc()).isEqualTo(13);

        CharacterBuildRequest fighter = new CharacterBuildRequest(
                "Kael", Edition.DND_5E_2014, "human", "fighter", "soldier",
                scores(15, 12, 14, 10, 10, 8), List.of("athletics", "perception"), "cadena", "longsword");
        assertThat(builder.build(fighter).getAc()).isEqualTo(16);
    }

    @Test
    void leatherArmorUsesElevenPlusDex() {
        // Half-elf: +2 CHA, +1 DEX, +1 CON → DEX 15+1=16 → +3. Leather 11 + 3 = 14.
        CharacterBuildRequest rogue = new CharacterBuildRequest(
                "Aria", Edition.DND_5E_2014, "half-elf", "rogue", "criminal",
                scores(10, 15, 12, 13, 10, 10), List.of("stealth", "perception", "investigation", "acrobatics"),
                "cuero", "shortsword");
        assertThat(builder.build(rogue).getAc()).isEqualTo(14);
    }

    @Test
    void derivesAttackFromWeaponAndAbilityAndProficiency() {
        CharacterBuildRequest request = new CharacterBuildRequest(
                "Kael", Edition.DND_5E_2014, "human", "fighter", "soldier",
                scores(15, 12, 14, 10, 10, 8), List.of("athletics", "perception"), "cadena", "longsword");
        PlayerCharacter character = builder.build(request);
        // Human +1 STR → STR 16 (+3), +2 proficiency = +5; longsword 1d8 + 3
        Attack longsword = character.getAttacks().stream().filter(a -> a.name().equals("Longsword")).findFirst().orElseThrow();
        assertThat(longsword.attackBonus()).isEqualTo(5);
        assertThat(longsword.damageExpression()).isEqualTo("1d8+3");
    }

    @Test
    void rejectsOverBudgetPointBuy() {
        CharacterBuildRequest request = new CharacterBuildRequest(
                "Cheto", Edition.DND_5E_2014, "human", "fighter", "soldier",
                scores(15, 15, 15, 15, 15, 15), List.of("athletics", "perception"), "cadena", "longsword");
        assertThatThrownBy(() -> builder.build(request)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("puntos");
    }

    @Test
    void rejectsTooManyOrInvalidSkills() {
        CharacterBuildRequest tooMany = new CharacterBuildRequest(
                "Kael", Edition.DND_5E_2014, "human", "fighter", "soldier",
                scores(15, 12, 14, 10, 10, 8),
                List.of("athletics", "perception", "history"), "cadena", "longsword");
        assertThatThrownBy(() -> builder.build(tooMany)).isInstanceOf(IllegalArgumentException.class);

        CharacterBuildRequest invalid = new CharacterBuildRequest(
                "Kael", Edition.DND_5E_2014, "human", "fighter", "soldier",
                scores(15, 12, 14, 10, 10, 8), List.of("cooking", "perception"), "cadena", "longsword");
        assertThatThrownBy(() -> builder.build(invalid)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void backgroundGrantsSkillsAutomatically() {
        CharacterBuildRequest request = new CharacterBuildRequest(
                "Borin", Edition.DND_5E_2014, "dwarf", "fighter", "sage",
                scores(15, 12, 14, 10, 10, 8), List.of("athletics", "perception"), "cadena", "longsword");
        PlayerCharacter character = builder.build(request);
        @SuppressWarnings("unchecked")
        List<String> skills = (List<String>) character.getCreationData().get("skills");
        assertThat(skills).contains("arcana", "history", "athletics", "perception");
    }

    @Test
    void twoThousandTwentyFourTakesBonusesFromBackground() {
        // Soldier background +2 STR, human gives no bonus in 2024.
        CharacterBuildRequest request = new CharacterBuildRequest(
                "Kael", Edition.DND_5E_2024, "human", "fighter", "soldier",
                scores(15, 12, 14, 10, 10, 8), List.of("athletics", "perception"), "cadena", "longsword");
        PlayerCharacter character = builder.build(request);
        assertThat(character.getStats().strength()).isEqualTo(17); // 15 + 2
        assertThat(character.getStats().constitution()).isEqualTo(15); // 14 + 1
    }
}
