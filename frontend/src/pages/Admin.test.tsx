import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Admin } from "@/pages/Admin";

const { adminUsersMock, adminCampaignsMock, adminCharactersMock, currentRole } = vi.hoisted(() => ({
  adminUsersMock: vi.fn(),
  adminCampaignsMock: vi.fn(),
  adminCharactersMock: vi.fn(),
  currentRole: { value: "ADMIN" },
}));

vi.mock("@/lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api")>();
  return {
    ...actual,
    getStoredUser: () => ({ id: 1, username: "hero", displayName: "Hero", role: currentRole.value }),
    api: {
      ...actual.api,
      adminUsers: adminUsersMock,
      adminCampaigns: adminCampaignsMock,
      adminCharacters: adminCharactersMock,
    },
  };
});

function renderAdmin() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <Admin />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("Admin", () => {
  beforeEach(() => {
    currentRole.value = "ADMIN";
    adminUsersMock.mockReset().mockResolvedValue([
      { id: 1, username: "hero", displayName: "Hero", role: "ADMIN", characterCount: 0 },
    ]);
    adminCampaignsMock.mockReset().mockResolvedValue([
      { id: 1, name: "La Cripta", edition: "dnd-5e-2014", status: "ACTIVE", memberCount: 2, owner: "Hero" },
    ]);
    adminCharactersMock.mockReset().mockResolvedValue([]);
  });

  it("exposes the sections as a labelled tablist", async () => {
    renderAdmin();

    const tablist = screen.getByRole("tablist", { name: "Secciones de administración" });
    expect(tablist).toBeInTheDocument();

    const usersTab = await screen.findByRole("tab", { name: "Usuarios (1)" });
    expect(usersTab).toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tabpanel")).toHaveAttribute("aria-labelledby", "admin-tab-users");
  });

  it("moves between tabs with the arrow keys", async () => {
    renderAdmin();

    const usersTab = await screen.findByRole("tab", { name: "Usuarios (1)" });
    usersTab.focus();
    await userEvent.keyboard("{ArrowRight}");

    const campaignsTab = screen.getByRole("tab", { name: "Campañas (1)" });
    expect(campaignsTab).toHaveAttribute("aria-selected", "true");
    expect(campaignsTab).toHaveFocus();
    expect(screen.getByRole("tabpanel")).toHaveAttribute("aria-labelledby", "admin-tab-campaigns");
  });

  it("captions the tables for screen readers", async () => {
    renderAdmin();

    await userEvent.click(await screen.findByRole("tab", { name: "Campañas (1)" }));
    expect(await screen.findByRole("table", { name: "Campañas del sistema" })).toBeInTheDocument();
    expect(screen.getByText("La Cripta")).toBeInTheDocument();
  });

  it("restricts access to administrators", async () => {
    currentRole.value = "USER";
    renderAdmin();

    expect(await screen.findByRole("heading", { name: "Acceso restringido" })).toBeInTheDocument();
    expect(screen.queryByRole("tablist")).not.toBeInTheDocument();
  });
});
