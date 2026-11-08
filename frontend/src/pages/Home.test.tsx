import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { Home } from "@/pages/Home";
import type { Campaign, CampaignPage, CampaignStatus } from "@/types/game";

const { campaignsPageMock } = vi.hoisted(() => ({ campaignsPageMock: vi.fn() }));

vi.mock("@/lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api")>();
  return {
    ...actual,
    getStoredUser: () => ({ id: 1, username: "hero", displayName: "Hero", role: "USER" }),
    api: {
      ...actual.api,
      campaignsPage: campaignsPageMock,
      characters: vi.fn().mockResolvedValue([]),
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
});
