import { describe, expect, it } from "vitest";
import { abilityModifier, formatModifier, hpPercent } from "@/lib/utils";

describe("utils", () => {
  it("computes ability modifiers", () => {
    expect(abilityModifier(8)).toBe(-1);
    expect(abilityModifier(10)).toBe(0);
    expect(abilityModifier(14)).toBe(2);
    expect(abilityModifier(20)).toBe(5);
  });

  it("formats modifiers with sign", () => {
    expect(formatModifier(3)).toBe("+3");
    expect(formatModifier(-1)).toBe("-1");
    expect(formatModifier(0)).toBe("+0");
  });

  it("clamps hp percentage", () => {
    expect(hpPercent(10, 20)).toBe(50);
    expect(hpPercent(-5, 20)).toBe(0);
    expect(hpPercent(40, 20)).toBe(100);
    expect(hpPercent(1, 0)).toBe(0);
  });
});
