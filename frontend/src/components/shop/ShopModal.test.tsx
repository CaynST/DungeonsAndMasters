import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ShopModal } from "@/components/shop/ShopModal";
import { api } from "@/lib/api";
import type { ShopView } from "@/types/game";

vi.mock("@/lib/api", () => ({
  api: {
    shop: vi.fn(),
    buy: vi.fn(),
    sell: vi.fn(),
    equip: vi.fn(),
  },
}));

const shopView: ShopView = {
  characterId: 1,
  gold: 100,
  silver: 0,
  copper: 0,
  carriedWeight: 9,
  carryingCapacity: 150,
  encumbered: false,
  items: [
    {
      id: "longsword",
      name: "Espada larga",
      kind: "WEAPON",
      price: 15,
      icon: "sword",
      description: "1d8 cortante.",
      weight: 3,
    },
  ],
  inventory: [{ name: "Escudo", description: "+2 CA", icon: "shield", quantity: 1, weight: 6 }],
};

function renderShop(onClose: () => void = () => {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <ShopModal campaignId={1} onClose={onClose} />
    </QueryClientProvider>,
  );
}

describe("ShopModal", () => {
  beforeEach(() => {
    vi.mocked(api.shop).mockResolvedValue(shopView);
  });

  it("shows catalogue and inventory weights plus the carried total", async () => {
    renderShop();

    expect(await screen.findByText("Espada larga")).toBeInTheDocument();
    expect(screen.getByText("3 lb")).toBeInTheDocument();
    expect(screen.getByText("6 lb")).toBeInTheDocument();
    expect(screen.getByText("9 / 150 lb")).toBeInTheDocument();
    expect(screen.queryByText(/Sobrecargado/)).not.toBeInTheDocument();
  });

  it("is a labelled modal that takes focus and closes with Escape", async () => {
    const onClose = vi.fn();
    renderShop(onClose);

    const dialog = await screen.findByRole("dialog", { name: "Mercader" });
    expect(dialog).toHaveAttribute("aria-modal", "true");
    expect(dialog).toHaveFocus();

    await userEvent.keyboard("{Escape}");
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("offers a named close control", async () => {
    const onClose = vi.fn();
    renderShop(onClose);

    await userEvent.click(await screen.findByRole("button", { name: "Cerrar la tienda" }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("warns when the character is over capacity", async () => {
    vi.mocked(api.shop).mockResolvedValue({
      ...shopView,
      carriedWeight: 160,
      encumbered: true,
    });

    renderShop();

    expect(await screen.findByText(/Sobrecargado/)).toBeInTheDocument();
    expect(screen.getByText(/160 \/ 150 lb/)).toBeInTheDocument();
  });
});
