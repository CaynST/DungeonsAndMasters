import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { DmPanel } from "@/components/dm/DmPanel";
import type { GameEvent } from "@/types/game";

const events: GameEvent[] = [
  {
    id: 1,
    campaignId: 1,
    sequence: 1,
    type: "COMBAT_STARTED",
    actor: "Dungeon Master",
    message: "¡Comienza el combate!",
    dataJson: "{}",
    createdAt: "2026-01-01T00:00:00Z",
  },
];

describe("DmPanel", () => {
  it("shows the DM narration when there are no events yet", () => {
    render(
      <DmPanel
        dmName="El Archivista"
        narration="Los muros respiran humedad."
        events={[]}
        providerId="mock"
        onRollCheck={() => {}}
        onSay={() => {}}
      />,
    );
    expect(screen.getByText("El Archivista")).toBeInTheDocument();
    expect(screen.getByText("Los muros respiran humedad.")).toBeInTheDocument();
    expect(screen.getByText("mock")).toBeInTheDocument();
  });

  it("renders narrations and events in the log", () => {
    render(
      <DmPanel
        dmName="El Archivista"
        narration={null}
        events={[
          ...events,
          {
            id: 2,
            campaignId: 1,
            sequence: 2,
            type: "NARRATION",
            actor: "Dungeon Master",
            message: "El orco gruñe y alza su hacha.",
            dataJson: "{}",
            createdAt: "2026-01-01T00:00:01Z",
          },
        ]}
        onRollCheck={() => {}}
        onSay={() => {}}
      />,
    );
    expect(screen.getByText(/Comienza el combate/)).toBeInTheDocument();
    expect(screen.getByText("El orco gruñe y alza su hacha.")).toBeInTheDocument();
  });

  it("labels attacks rolled with advantage or disadvantage", () => {
    render(
      <DmPanel
        dmName="El Archivista"
        narration={null}
        events={[
          {
            id: 3,
            campaignId: 1,
            sequence: 3,
            type: "ATTACK",
            actor: "Kael",
            message: "Kael impacta a Goblin con Espada larga.",
            dataJson: JSON.stringify({ advantage: "ADVANTAGE" }),
            createdAt: "2026-01-01T00:00:02Z",
          },
          {
            id: 4,
            campaignId: 1,
            sequence: 4,
            type: "ATTACK_MISS",
            actor: "Goblin",
            message: "Goblin falla su ataque contra Kael.",
            dataJson: JSON.stringify({ advantage: "DISADVANTAGE" }),
            createdAt: "2026-01-01T00:00:03Z",
          },
          {
            id: 5,
            campaignId: 1,
            sequence: 5,
            type: "ATTACK",
            actor: "Kael",
            message: "Kael impacta a Goblin con Espada corta.",
            dataJson: JSON.stringify({ advantage: "NONE" }),
            createdAt: "2026-01-01T00:00:04Z",
          },
        ]}
        onRollCheck={() => {}}
        onSay={() => {}}
      />,
    );
    expect(screen.getByText("Ventaja")).toBeInTheDocument();
    expect(screen.getByText("Desventaja")).toBeInTheDocument();
  });

  it("renders a pending check with a roll button", () => {
    render(
      <DmPanel
        dmName="El Archivista"
        narration="..."
        events={[]}
        onRollCheck={() => {}}
        onSay={() => {}}
      />,
    );
    // No pending check in the store by default.
    expect(screen.queryByText(/Prueba solicitada/)).not.toBeInTheDocument();
  });
});
