import { describe, expect, it } from "vitest";
import { FIGHTING_STYLE_OPTIONS } from "@/pages/CharacterCreation";

describe("FIGHTING_STYLE_OPTIONS", () => {
  it("offers every fighting style supported by the engine", () => {
    expect(FIGHTING_STYLE_OPTIONS.map((option) => option.value)).toEqual([
      "defense",
      "archery",
      "dueling",
      "great-weapon",
      "protection",
    ]);
  });

  it("describes great weapon and protection mechanics", () => {
    const greatWeapon = FIGHTING_STYLE_OPTIONS.find((option) => option.value === "great-weapon");
    const protection = FIGHTING_STYLE_OPTIONS.find((option) => option.value === "protection");

    expect(greatWeapon?.label).toContain("Gran arma");
    expect(greatWeapon?.label).toContain("1 o 2");
    expect(protection?.label).toContain("Protección");
    expect(protection?.label).toContain("desventaja");
  });
});
