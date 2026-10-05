import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { InitiativeTracker } from "@/components/InitiativeTracker";
import type { CombatState, GameEntity } from "@/types/game";

const entities: GameEntity[] = [
  {
    id: 1,
    sceneId: 1,
    mapId: 1,
    kind: "PLAYER",
    name: "Kael",
    x: 1,
    y: 1,
    hp: 20,
    maxHp: 28,
    ac: 16,
    speed: 6,
    alive: true,
    data: {},
  },
  {
    id: 2,
    sceneId: 1,
    mapId: 1,
    kind: "ENEMY",
    name: "Goblin",
    x: 5,
    y: 5,
    hp: 0,
    maxHp: 7,
    ac: 13,
    speed: 6,
    alive: false,
    data: {},
  },
];

const combat: CombatState = {
  campaignId: 1,
  active: true,
  round: 2,
  currentTurnIndex: 0,
  status: "ACTIVE",
  participants: [
    { entityId: 1, name: "Kael", kind: "PLAYER", initiative: 18, orderIndex: 0, alive: true },
    { entityId: 2, name: "Goblin", kind: "ENEMY", initiative: 12, orderIndex: 1, alive: false },
  ],
};

describe("InitiativeTracker", () => {
  it("shows the round and orders participants", () => {
    render(<InitiativeTracker combat={combat} entities={entities} />);
    expect(screen.getByText("Ronda 2")).toBeInTheDocument();
    expect(screen.getByText(/Turno de/)).toBeInTheDocument();
    expect(screen.getAllByText(/Kael/).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Goblin/).length).toBeGreaterThan(0);
  });

  it("highlights the current participant", () => {
    render(<InitiativeTracker combat={combat} entities={entities} />);
    expect(screen.getByText(/Turno de/)).toBeInTheDocument();
  });
});
