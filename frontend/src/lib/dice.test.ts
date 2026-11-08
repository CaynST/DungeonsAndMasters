import { describe, expect, it } from "vitest";
import { DICE_SIDES, firstFace, outcomeOf, primarySides } from "@/lib/dice";

describe("dice helpers", () => {
  it("parses the die type from an expression", () => {
    expect(primarySides("1d20")).toBe(20);
    expect(primarySides("2d6+3")).toBe(6);
    expect(primarySides("1d8")).toBe(8);
    expect(primarySides("1d100")).toBe(100);
    expect(primarySides(undefined)).toBe(20);
    expect(primarySides("garbage")).toBe(20);
  });

  it("offers a d100 in the quick-roll menu", () => {
    expect(DICE_SIDES).toContain(100);
  });

  it("classifies outcomes", () => {
    expect(outcomeOf({ expression: "1d20", rolls: [20], modifier: 0, total: 20, natural20: true, natural1: false })).toBe("crit");
    expect(outcomeOf({ expression: "1d20", rolls: [1], modifier: 0, total: 1, natural20: false, natural1: true })).toBe("fail");
    expect(outcomeOf({ expression: "1d6", rolls: [4], modifier: 0, total: 4, natural20: false, natural1: false })).toBe("normal");
    expect(outcomeOf({ expression: "1d100", rolls: [100], modifier: 0, total: 100, natural20: false, natural1: false })).toBe("max");
  });

  it("does not crash on a roll without a rolls array (regression)", () => {
    const checkLike = { expression: "1d20", total: 15 } as never;
    expect(() => outcomeOf(checkLike)).not.toThrow();
    expect(outcomeOf(checkLike)).toBe("normal");
    expect(outcomeOf(undefined)).toBe("normal");
    expect(firstFace(checkLike)).toBe(15);
  });
});
