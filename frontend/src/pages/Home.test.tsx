import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { Home } from "@/pages/Home";
import type { Campaign, CampaignPage, CampaignStatus } from "@/types/game";

const { campaignsPageMock, charactersMock, createCampaignMock } = vi.hoisted(() => ({
  campaignsPageMock: vi.fn(),
  charactersMock: vi.fn(),
  createCampaignMock: vi.fn(),
}));

vi.mock("@/lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api")>();
  return {
    ...actual,
    getStoredUser: () => ({ id: 1, username: "hero", displayName: "Hero", role: "USER" }),
    api: {
      ...actual.api,
      campaignsPage: campaignsPageMock,
      characters: charactersMock,
      createCampaign: createCampaignMock,
    },
  };
});

function campaign(id: number, name: string, status: CampaignStatus): Campaign {
  return {
    id,
    name,
    setting: "Una cripta olvidada",
    dmName: "El Dungeon Master",
    dmAvatar: "/images/dm/dungeon-master.png",
    image: "/images/campaigns/valdros.png",
    status,
    phase: "INTRO",
    members: [],
  };
}

function page(content: Campaign[], pageNumber = 0): CampaignPage {
  return {
    content,
    page: pageNumber,
    size: 6,
    totalElements: 12,
    totalPages: 2,
    first: pageNumber === 0,
    last: pageNumber > 0,
  };
}

function emptyPage(): CampaignPage {
  return {
    content: [],
    page: 0,
    size: 6,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  };
}

function renderHome() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <Home />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("Home campaign browsing", () => {
  beforeEach(() => {
    campaignsPageMock.mockReset();
    charactersMock.mockReset();
    createCampaignMock.mockReset();
    charactersMock.mockResolvedValue([]);
  });

  it("requests the first page and filters by status", async () => {
    campaignsPageMock.mockImplementation(({ status }: { status?: string }) =>
      Promise.resolve(
        page([
          status === "ACTIVE"
            ? campaign(2, "Campaña en curso", "ACTIVE")
            : campaign(1, "Campaña en lobby", "LOBBY"),
        ]),
      ),
    );

    renderHome();

    expect((await screen.findAllByText("Campaña en lobby")).length).toBeGreaterThan(0);
    expect(campaignsPageMock).toHaveBeenCalledWith({ page: 0, size: 6, status: undefined });

    await userEvent.click(screen.getByRole("button", { name: "En curso" }));

    expect(await screen.findByText("Campaña en curso")).toBeInTheDocument();
    await waitFor(() =>
      expect(campaignsPageMock).toHaveBeenCalledWith({ page: 0, size: 6, status: "ACTIVE" }),
    );
  });

  it("creates a campaign from the keyboard inside the create form", async () => {
    campaignsPageMock.mockResolvedValue(page([campaign(1, "Campaña en lobby", "LOBBY")]));
    createCampaignMock.mockResolvedValue(campaign(9, "Nueva Mesa", "LOBBY"));

    renderHome();

    await screen.findAllByText("Campaña en lobby");
    await userEvent.click(screen.getByRole("button", { name: "Crear campaña" }));
    await userEvent.type(screen.getByLabelText("Nombre de la campaña"), "Nueva Mesa{Enter}");

    await waitFor(() =>
      expect(createCampaignMock).toHaveBeenCalledWith("Nueva Mesa", "dnd-5e-2014"),
    );
  });

  it("paginates without discarding campaigns", async () => {
    campaignsPageMock.mockImplementation(({ page: pageNumber }: { page?: number }) =>
      Promise.resolve(
        pageNumber === 1
          ? page([campaign(2, "Campaña página dos", "ACTIVE")], 1)
          : page([campaign(1, "Campaña página uno", "LOBBY")]),
      ),
    );

    renderHome();

    expect((await screen.findAllByText("Campaña página uno")).length).toBeGreaterThan(0);
    expect(screen.getByText("Página 1 de 2")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: /Siguiente/ }));

    expect((await screen.findAllByText("Campaña página dos")).length).toBeGreaterThan(0);
    expect(screen.getByText("Página 2 de 2")).toBeInTheDocument();
    await waitFor(() =>
      expect(campaignsPageMock).toHaveBeenCalledWith({ page: 1, size: 6, status: undefined }),
    );
  });

  it("guides the user when there are no campaigns or heroes yet", async () => {
    campaignsPageMock.mockResolvedValue(emptyPage());

    renderHome();

    expect(await screen.findByText("La mesa está vacía")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Crear la primera campaña/ })).toBeInTheDocument();
    expect(await screen.findByText("Aún no has forjado ningún héroe")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Crear mi primer héroe/ })).toBeInTheDocument();
  });

  it("offers a way back when a status filter has no results", async () => {
    campaignsPageMock.mockResolvedValue(emptyPage());

    renderHome();

    expect(await screen.findByText("La mesa está vacía")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "En curso" }));

    expect(await screen.findByText("Sin campañas en este estado")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /Ver todas/ }));

    expect(await screen.findByText("La mesa está vacía")).toBeInTheDocument();
  });
});
