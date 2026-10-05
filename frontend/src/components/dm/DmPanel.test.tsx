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
  it("shows the DM narration", () => {
    render(
      <DmPanel
        dmName="El Archivista"
        narration="Los muros respiran humedad."
        events={events}
        providerId="mock"
        onRollCheck={() => {}}
        onSay={() => {}}
      />,
    );
    expect(screen.getByText("El Archivista")).toBeInTheDocument();
    expect(screen.getByText("Los muros respiran humedad.")).toBeInTheDocument();
    expect(screen.getByText("mock")).toBeInTheDocument();
    expect(screen.getByText(/Comienza el combate/)).toBeInTheDocument();
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
