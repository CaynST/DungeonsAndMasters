import { describe, expect, it } from "vitest";
import { entityConditions, hasCondition, isIncapacitated } from "@/lib/conditions";
import type { GameEntity } from "@/types/game";

function entity(data: Record<string, unknown>): GameEntity {
  return {
    id: 1,
    sceneId: 1,
    mapId: 1,
    kind: "PLAYER",
    name: "Kael",
    x: 1,
    y: 1,
    hp: 20,
    maxHp: 20,
    ac: 15,
    speed: 6,
    alive: true,
    data,
  };
}

describe("conditions", () => {
  it("reads map and legacy string entries", () => {
    const conditions = entityConditions(entity({
      conditions: [
        { code: "POISONED", rounds: 2 },
        "PRONE",
      ],
    }));

    expect(conditions.map((condition) => condition.code)).toEqual(["POISONED", "PRONE"]);
    expect(conditions[0].rounds).toBe(2);
    expect(conditions[0].label).toBe("Envenenado");
  });

  it("ignores unknown codes and malformed data", () => {
    expect(entityConditions(entity({ conditions: [{ code: "BANANA" }, 42] }))).toEqual([]);
    expect(entityConditions(entity({ conditions: "not-a-list" }))).toEqual([]);
    expect(entityConditions(undefined)).toEqual([]);
  });

  it("exposes exhaustion level and helpers", () => {
    const target = entity({
      conditions: [{ code: "EXHAUSTION", level: 3 }, { code: "PARALYZED" }],
    });

    const exhaustion = entityConditions(target).find((condition) => condition.code === "EXHAUSTION");
    expect(exhaustion?.level).toBe(3);
    expect(hasCondition(target, "PARALYZED")).toBe(true);
    expect(isIncapacitated(target)).toBe(true);
    expect(isIncapacitated(entity({ conditions: [{ code: "PRONE" }] }))).toBe(false);
  });
});
