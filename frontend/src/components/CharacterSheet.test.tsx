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

  it("renders languages and proficiencies from creation data", () => {
    const borin: PlayerCharacter = {
      ...kael,
      name: "Borin",
      creationData: {
        languages: ["Común", "Enano"],
        armorProficiencies: ["Todas las armaduras", "Escudos"],
        weaponProficiencies: ["Armas simples", "Armas marciales"],
        toolProficiencies: ["Set de juego (a elección)", "Vehículos (terrestres)"],
      },
    };

    render(<CharacterSheet character={borin} />);

    expect(screen.getByText("Idiomas y competencias")).toBeInTheDocument();
    expect(screen.getByText("Común")).toBeInTheDocument();
    expect(screen.getByText("Enano")).toBeInTheDocument();
    expect(screen.getByText("Todas las armaduras")).toBeInTheDocument();
    expect(screen.getByText("Vehículos (terrestres)")).toBeInTheDocument();
  });

  it("hides the proficiency section when creation data is absent", () => {
    render(<CharacterSheet character={kael} />);
    expect(screen.queryByText("Idiomas y competencias")).not.toBeInTheDocument();
  });

  it("shows carried weight and carrying capacity", () => {
    render(
      <CharacterSheet
        character={{ ...kael, carriedWeight: 22.5, carryingCapacity: 210, encumbered: false }}
      />,
    );

    expect(screen.getByText("Carga")).toBeInTheDocument();
    expect(screen.getByText("22.5 / 210 lb")).toBeInTheDocument();
    expect(screen.queryByText(/Sobrecargado/)).not.toBeInTheDocument();
  });

  it("warns about encumbrance without changing speed", () => {
    render(
      <CharacterSheet
        character={{ ...kael, carriedWeight: 220, carryingCapacity: 210, encumbered: true }}
      />,
    );

    expect(screen.getByText(/Sobrecargado/)).toBeInTheDocument();
    expect(screen.getByText(/220 lb supera tu capacidad de 210 lb/)).toBeInTheDocument();
    expect(screen.getByText("6")).toBeInTheDocument(); // speed stays at 6
  });

  it("falls back to inventory weights when the server omits totals", () => {
    const loaded: PlayerCharacter = {
      ...kael,
      stats: { ...kael.stats, strength: 10 },
      inventory: [
        { name: "Espada larga", quantity: 1, weight: 3 },
        { name: "Flechas", quantity: 20, weight: 0.05 },
      ],
    };

    render(<CharacterSheet character={loaded} />);

    expect(screen.getByText("4 / 150 lb")).toBeInTheDocument();
    expect(screen.getByText("3 lb")).toBeInTheDocument();
    expect(screen.getByText("1 lb")).toBeInTheDocument();
  });
});
