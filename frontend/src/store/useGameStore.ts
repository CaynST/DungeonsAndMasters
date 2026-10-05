import { create } from "zustand";
import type { AttackOutcome, DiceRoll, GameEntity } from "@/types/game";

export interface PendingCheck {
  skill: string;
  dc: number;
  prompt?: string;
}

export interface FloatingText {
  id: number;
  entityId: number;
  text: string;
  kind: "damage" | "heal";
}

interface DiceRequest {
  id: number;
  expression: string;
  result: DiceRoll;
  purpose?: string;
  onComplete?: () => void;
}

interface GameUiState {
  selectedEntityId: number | null;
  hoveredCell: { x: number; y: number } | null;
  pendingCheck: PendingCheck | null;
  reachableCells: { x: number; y: number }[];
  diceRequest: DiceRequest | null;
  lastRoll: DiceRoll | null;
  lastAttack: AttackOutcome | null;
  floatingTexts: FloatingText[];
  dmThinking: boolean;
  activeEntityId: number | null;

  selectEntity: (id: number | null) => void;
  setHoveredCell: (cell: { x: number; y: number } | null) => void;
  setPendingCheck: (check: PendingCheck | null) => void;
  setReachableCells: (cells: { x: number; y: number }[]) => void;
  setActiveEntity: (id: number | null) => void;
  requestDice: (expression: string, result: DiceRoll, purpose?: string, onComplete?: () => void) => void;
  clearDice: () => void;
  setLastAttack: (attack: AttackOutcome | null) => void;
  setDmThinking: (thinking: boolean) => void;
  addFloatingText: (entityId: number, text: string, kind: "damage" | "heal") => void;
  clearFloatingText: (id: number) => void;
}

let floatingCounter = 0;

export const useGameStore = create<GameUiState>((set, get) => ({
  selectedEntityId: null,
  hoveredCell: null,
  pendingCheck: null,
  reachableCells: [],
  diceRequest: null,
  lastRoll: null,
  lastAttack: null,
  floatingTexts: [],
  dmThinking: false,
  activeEntityId: null,

  selectEntity: (id) => set({ selectedEntityId: id }),
  setHoveredCell: (cell) => set({ hoveredCell: cell }),
  setPendingCheck: (check) => set({ pendingCheck: check }),
  setReachableCells: (cells) => set({ reachableCells: cells }),
  setActiveEntity: (id) => set({ activeEntityId: id }),

  requestDice: (expression, result, purpose, onComplete) =>
    set({
      diceRequest: { id: Date.now(), expression, result, purpose, onComplete },
      lastRoll: result,
    }),

  clearDice: () => {
    const request = get().diceRequest;
    request?.onComplete?.();
    set({ diceRequest: null });
  },

  setLastAttack: (attack) => set({ lastAttack: attack }),
  setDmThinking: (thinking) => set({ dmThinking: thinking }),

  addFloatingText: (entityId, text, kind) => {
    const id = ++floatingCounter;
    set((state) => ({ floatingTexts: [...state.floatingTexts, { id, entityId, text, kind }] }));
    setTimeout(() => get().clearFloatingText(id), 1400);
  },

  clearFloatingText: (id) =>
    set((state) => ({ floatingTexts: state.floatingTexts.filter((f) => f.id !== id) })),
}));

export function entityById(entities: GameEntity[], id: number | null | undefined) {
  if (id == null) return undefined;
  return entities.find((entity) => entity.id === id);
}
