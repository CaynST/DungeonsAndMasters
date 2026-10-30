import { describe, expect, it } from "vitest";
import { classIdOf } from "@/lib/classes";

describe("classIdOf", () => {
  it("maps English and Spanish class names", () => {
    expect(classIdOf("Fighter")).toBe("fighter");
    expect(classIdOf("Guerrero")).toBe("fighter");
    expect(classIdOf("Bárbaro")).toBe("barbarian");
    expect(classIdOf("Pícaro")).toBe("rogue");
    expect(classIdOf("Mago")).toBe("wizard");
    expect(classIdOf("Clérigo")).toBe("cleric");
  });

  it("prefers explicit creation data", () => {
    expect(classIdOf("Fighter", { classId: "rogue" })).toBe("rogue");
  });

  it("returns null for unknown or missing classes", () => {
    expect(classIdOf("Pescador")).toBeNull();
    expect(classIdOf(undefined)).toBeNull();
  });
});
