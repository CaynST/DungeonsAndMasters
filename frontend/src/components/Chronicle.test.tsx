import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Chronicle } from "@/components/Chronicle";
import type { GameEvent } from "@/types/game";

const { eventsMock } = vi.hoisted(() => ({ eventsMock: vi.fn() }));

vi.mock("@/lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api")>();
  return {
    ...actual,
    api: {
      ...actual.api,
      events: eventsMock,
    },
  };
});

function event(overrides: Partial<GameEvent> = {}): GameEvent {
  return {
    id: 1,
    campaignId: 1,
    sequence: 1,
    type: "NARRATION",
    actor: "Dungeon Master",
    message: "La cripta se abre.",
    dataJson: "{}",
    createdAt: "2026-11-08T10:30:00Z",
    ...overrides,
  };
}

function renderChronicle() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <Chronicle campaignId={1} />
    </QueryClientProvider>,
  );
}

describe("Chronicle states", () => {
  beforeEach(() => {
    eventsMock.mockReset();
  });

  it("shows a welcoming blank page when the story has not started", async () => {
    eventsMock.mockResolvedValue([]);

    renderChronicle();

    expect(await screen.findByText("La crónica está en blanco")).toBeInTheDocument();
    expect(screen.getByText("0 entradas")).toBeInTheDocument();
  });

  it("renders entries with translated labels and a readable count", async () => {
    eventsMock.mockResolvedValue([
      event({ id: 1, sequence: 1, message: "La cripta se abre." }),
      event({
        id: 2,
        sequence: 2,
        type: "COMBAT_STARTED",
        actor: undefined,
        message: "Los goblins atacan.",
        createdAt: "2026-11-08T10:31:00Z",
      }),
    ]);

    renderChronicle();

    expect(await screen.findByText(/La cripta se abre/)).toBeInTheDocument();
    expect(screen.getByText("Narración")).toBeInTheDocument();
    expect(screen.getByText("Comienza el combate")).toBeInTheDocument();
    expect(screen.getByText("Los goblins atacan.")).toBeInTheDocument();
    expect(screen.getByText("2 entradas")).toBeInTheDocument();
  });

  it("offers a retry when the chronicle cannot be loaded", async () => {
    eventsMock.mockRejectedValue(new Error("offline"));

    renderChronicle();

    expect(await screen.findByText("No se pudo recuperar la crónica")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Reintentar/ })).toBeInTheDocument();
  });
});
