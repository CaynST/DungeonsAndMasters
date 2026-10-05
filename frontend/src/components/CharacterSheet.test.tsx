import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { CharacterSheet } from "@/components/CharacterSheet";
import type { PlayerCharacter } from "@/types/game";

const kael: PlayerCharacter = {
  id: 1,
  name: "Kael",
  cls: "Fighter",
  race: "Human",
  level: 1,
  hp: 23,
  maxHp: 28,
  ac: 16,
  speed: 6,
  color: "#C2410C",
  stats: { strength: 14, dexterity: 16, constitution: 13, intelligence: 10, wisdom: 12, charisma: 8 },
  attacks: [{ name: "Longsword", attackBonus: 5, damageExpression: "1d8+3", range: 1, ranged: false }],
  inventory: [{ name: "Escudo", description: "+2 CA", quantity: 1 }],
  predefined: true,
};

describe("CharacterSheet", () => {
  it("renders identity, HP, AC and stats", () => {
    render(<CharacterSheet character={kael} />);
    expect(screen.getByText("Kael")).toBeInTheDocument();
    expect(screen.getByText("Human · Fighter")).toBeInTheDocument();
    expect(screen.getByText("23 / 28")).toBeInTheDocument();
    expect(screen.getByText("🛡 16")).toBeInTheDocument();
    expect(screen.getByText("14")).toBeInTheDocument();
  });

  it("renders attacks and inventory", () => {
    render(<CharacterSheet character={kael} />);
    expect(screen.getByText("Longsword")).toBeInTheDocument();
    expect(screen.getByText("Escudo")).toBeInTheDocument();
  });
});
