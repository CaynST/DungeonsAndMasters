import { describe, expect, it, vi } from "vitest";
import type { ComponentProps } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ActionBar } from "@/components/actions/ActionBar";
import type { PlayerCharacter } from "@/types/game";

const kael: PlayerCharacter = {
  id: 1,
  name: "Kael",
  cls: "Fighter",
  race: "Human",
  level: 1,
  hp: 28,
  maxHp: 28,
  ac: 16,
  speed: 6,
  stats: { strength: 14, dexterity: 16, constitution: 13, intelligence: 10, wisdom: 12, charisma: 8 },
  attacks: [{ name: "Longsword", attackBonus: 5, damageExpression: "1d8+3", range: 1, ranged: false }],
  inventory: [
    { name: "Antorcha", description: "Ilumina 6 casillas.", quantity: 1 },
    { name: "Poción de Curación", description: "Recupera 2d4+2 HP.", quantity: 1 },
  ],
  predefined: true,
};

function renderBar(overrides: Partial<ComponentProps<typeof ActionBar>> = {}) {
  const handlers = {
    onSelectAttack: vi.fn(),
    onAttack: vi.fn(),
    onEndTurn: vi.fn(),
    onDialogue: vi.fn(),
    onQuickRoll: vi.fn(),
    onDiceRequest: vi.fn(),
    onOpenShop: vi.fn(),
    onToggleMode: vi.fn(),
    onCombatAction: vi.fn(),
    onStandUp: vi.fn(),
    onSecondWind: vi.fn(),
    onRage: vi.fn(),
    onBardicInspiration: vi.fn(),
    onUseInspiration: vi.fn(),
    onCastSpell: vi.fn(),
    onHelp: vi.fn(),
    onHide: vi.fn(),
    onSearch: vi.fn(),
    onReady: vi.fn(),
    onUseObject: vi.fn(),
    onInteract: vi.fn(),
    onShortRest: vi.fn(),
    onLongRest: vi.fn(),
  };
  render(
    <ActionBar
      character={kael}
      canAct
      isMyTurn
      mode="move"
      {...handlers}
      {...overrides}
    />,
  );
  return handlers;
}

async function openActions() {
  const user = userEvent.setup();
  await user.click(screen.getByRole("button", { name: /^acciones$/i }));
  return user;
}

describe("ActionBar combat actions", () => {
  it("opens a compact menu and runs Search and free interaction", async () => {
    const handlers = renderBar();
    const user = await openActions();

    expect(screen.getByText("Acciones de combate")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /buscar \(percepción\)/i }));
    expect(handlers.onSearch).toHaveBeenCalledWith("perception");

    await user.click(screen.getByRole("button", { name: /^acciones$/i }));
    await user.click(screen.getByRole("button", { name: /interactuar/i }));
    expect(handlers.onInteract).toHaveBeenCalledTimes(1);
  }, 30000);

  it("requires a selected ally before allowing Help", async () => {
    const handlers = renderBar();
    const user = await openActions();

    expect(screen.getByRole("button", { name: /ayudar/i })).toBeDisabled();
    await user.click(screen.getByRole("button", { name: /ayudar/i }));
    expect(handlers.onHelp).not.toHaveBeenCalled();
  }, 30000);

  it("enables Help once an ally is selected", async () => {
    const handlers = renderBar({ selectedAllyName: "Lyra" });
    const user = await openActions();

    await user.click(screen.getByRole("button", { name: /ayudar/i }));
    expect(handlers.onHelp).toHaveBeenCalledTimes(1);
  }, 30000);

  it("offers non-potion inventory items and hides potions", async () => {
    const handlers = renderBar();
    await openActions();

    expect(screen.getByRole("button", { name: /antorcha/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /poción/i })).not.toBeInTheDocument();
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: /antorcha/i }));
    expect(handlers.onUseObject).toHaveBeenCalledWith("Antorcha");
  }, 30000);

  it("shows one-use stances and disables repeating them", async () => {
    renderBar({ hidden: true, readied: true, objectInteractionUsed: true });
    await openActions();

    expect(screen.getByRole("button", { name: /oculto/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /preparado/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /interacción usada/i })).toBeDisabled();
  }, 30000);
});

describe("ActionBar rests", () => {
  it("opens the rest menu and requests short and long rests", async () => {
    const handlers = renderBar();
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: /^descanso$/i }));
    expect(screen.getByRole("button", { name: /descanso corto/i })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /descanso corto/i }));
    expect(handlers.onShortRest).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole("button", { name: /^descanso$/i }));
    await user.click(screen.getByRole("button", { name: /descanso largo/i }));
    expect(handlers.onLongRest).toHaveBeenCalledTimes(1);
  }, 30000);

  it("disables both rests during combat", async () => {
    renderBar({ inCombat: true });
    await userEvent.setup().click(screen.getByRole("button", { name: /^descanso$/i }));

    expect(screen.getByRole("button", { name: /descanso corto/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /descanso largo/i })).toBeDisabled();
    expect(screen.getByText(/no puedes descansar durante un combate/i)).toBeInTheDocument();
  }, 30000);

  it("disables a short rest when no hit dice remain", async () => {
    renderBar({ hitDiceRemaining: 0, hitDiceMax: 1 });
    await userEvent.setup().click(screen.getByRole("button", { name: /^descanso$/i }));

    expect(screen.getByRole("button", { name: /descanso corto/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /descanso largo/i })).toBeEnabled();
  }, 30000);
});
