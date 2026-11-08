import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { Lobby } from "@/pages/Lobby";
import type { Campaign, CampaignMember, PlayerCharacter } from "@/types/game";

const { campaignMock, charactersMock } = vi.hoisted(() => ({
  campaignMock: vi.fn(),
  charactersMock: vi.fn(),
}));

vi.mock("@/lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api")>();
  return {
    ...actual,
    getStoredUser: () => ({ id: 1, username: "hero", displayName: "Hero", role: "USER" }),
    api: {
      ...actual.api,
      campaign: campaignMock,
      characters: charactersMock,
      joinCampaign: vi.fn(),
      startCampaign: vi.fn(),
    },
  };
});

function member(overrides: Partial<CampaignMember> = {}): CampaignMember {
  return {
    id: 1,
    userId: 1,
    displayName: "Hero",
    characterId: 2,
    characterName: "Kael",
    role: "PLAYER",
    ready: true,
    ...overrides,
  };
}

function campaign(members: CampaignMember[] = []): Campaign {
  return {
    id: 1,
    name: "La Cripta de Valdros",
    setting: "Una cripta olvidada bajo la taberna.",
    dmName: "El Dungeon Master",
    dmAvatar: "/images/dm/dungeon-master.png",
    image: "/images/campaigns/valdros.png",
    status: "LOBBY",
    phase: "INTRO",
    members,
  };
}

function hero(): PlayerCharacter {
  return {
    id: 2,
    name: "Kael",
    cls: "Guerrero",
    race: "Humano",
    level: 1,
    hp: 12,
    maxHp: 12,
    ac: 16,
    speed: 30,
    stats: {
      strength: 16,
      dexterity: 14,
      constitution: 14,
      intelligence: 10,
      wisdom: 12,
      charisma: 8,
    },
    attacks: [],
    inventory: [],
    predefined: false,
  };
}

function renderLobby() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/campaign/1/lobby"]}>
        <Routes>
          <Route path="/campaign/:id/lobby" element={<Lobby />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("Lobby states", () => {
  beforeEach(() => {
    campaignMock.mockReset();
    charactersMock.mockReset();
  });

  it("invites the first player to take a seat and create a hero", async () => {
    campaignMock.mockResolvedValue(campaign());
    charactersMock.mockResolvedValue([]);

    renderLobby();

    expect(await screen.findByText("Nadie ha tomado asiento todavía")).toBeInTheDocument();
    expect(screen.getByText("No tienes héroes todavía")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Crear personaje/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Unirme a la partida/ })).toBeDisabled();
  });

  it("summarises the party readiness for an existing member", async () => {
    campaignMock.mockResolvedValue(campaign([member()]));
    charactersMock.mockResolvedValue([hero()]);

    renderLobby();

    expect(await screen.findByText("1 de 1 listos")).toBeInTheDocument();
    expect(screen.getByText("Tú")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Comenzar aventura/ })).toBeInTheDocument();
  });

  it("confirms the selected hero before joining", async () => {
    campaignMock.mockResolvedValue(campaign());
    charactersMock.mockResolvedValue([hero()]);

    renderLobby();

    const choice = await screen.findByRole("button", { name: /Kael/ });
    await userEvent.click(choice);

    expect(choice).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByText(/Entrarás en la mesa como/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Unirme a la partida/ })).toBeEnabled();
  });
});
