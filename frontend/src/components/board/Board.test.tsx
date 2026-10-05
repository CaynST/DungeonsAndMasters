import { describe, expect, it, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { StrictMode } from "react";
import { Board } from "@/components/board/Board";
import { useGameStore } from "@/store/useGameStore";
import type { GameEntity, GameMap } from "@/types/game";

const map: GameMap = {
  id: 1,
  name: "Test",
  width: 6,
  height: 6,
  tileSize: 48,
  walls: [{ x: 0, y: 0 }],
  doors: [],
  obstacles: [{ x: 3, y: 3 }],
  decor: [{ x: 2, y: 2, icon: "torch", kind: "torch" }],
};

const entities: GameEntity[] = [
  {
    id: 1,
    sceneId: 1,
    mapId: 1,
    kind: "PLAYER",
    name: "Kael",
    x: 1,
    y: 1,
    hp: 28,
    maxHp: 28,
    ac: 16,
    speed: 6,
    alive: true,
    data: {},
  },
  {
    id: 2,
    sceneId: 1,
    mapId: 1,
    kind: "ENEMY",
    name: "Goblin",
    x: 4,
    y: 4,
    hp: 7,
    maxHp: 7,
    ac: 13,
    speed: 6,
    alive: true,
    data: {},
  },
];

function renderBoard() {
  return render(
    <StrictMode>
      <Board
        map={map}
        entities={entities}
        activeEntityId={1}
        reachablePaths={new Map()}
        selectedEntityId={null}
        targetableEntityIds={new Set()}
        onCellClick={() => {}}
        onEntityClick={() => {}}
      />
    </StrictMode>,
  );
}

describe("Board", () => {
  beforeEach(() => {
    useGameStore.setState({ floatingTexts: [] });
  });

  it("renders a token per entity without crashing under StrictMode", () => {
    renderBoard();
    // Tokens render the first letter of each entity name.
    expect(screen.getByText("K")).toBeInTheDocument();
    expect(screen.getByText("G")).toBeInTheDocument();
  });

  it("does not loop when floating texts change (regression: getSnapshot caching)", () => {
    // This reproduces the infinite-loop crash: the floating-texts selector must
    // return a stable reference. Rendering under StrictMode exercises double-invoke.
    const { rerender } = renderBoard();
    useGameStore.setState({
      floatingTexts: [{ id: 1, entityId: 1, text: "-5", kind: "damage" }],
    });
    rerender(
      <StrictMode>
        <Board
          map={map}
          entities={entities}
          activeEntityId={1}
          reachablePaths={new Map()}
          selectedEntityId={null}
          targetableEntityIds={new Set()}
          onCellClick={() => {}}
          onEntityClick={() => {}}
        />
      </StrictMode>,
    );
    expect(screen.getByText("-5")).toBeInTheDocument();
  });
});
