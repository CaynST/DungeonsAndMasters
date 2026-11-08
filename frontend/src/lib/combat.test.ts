import { describe, expect, it } from "vitest";
import { attackAdvantage } from "@/lib/combat";

describe("attackAdvantage", () => {
  it("reads advantage and disadvantage from the event payload", () => {
    expect(attackAdvantage(JSON.stringify({ advantage: "ADVANTAGE" }))).toBe("ADVANTAGE");
    expect(attackAdvantage(JSON.stringify({ advantage: "DISADVANTAGE" }))).toBe("DISADVANTAGE");
  });

  it("ignores neutral and unknown values", () => {
    expect(attackAdvantage(JSON.stringify({ advantage: "NONE" }))).toBeNull();
    expect(attackAdvantage("{}")).toBeNull();
    expect(attackAdvantage(undefined)).toBeNull();
  });

  it("does not crash on malformed payloads (regression)", () => {
    expect(attackAdvantage("not-json")).toBeNull();
  });
});
