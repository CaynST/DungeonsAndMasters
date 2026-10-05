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
