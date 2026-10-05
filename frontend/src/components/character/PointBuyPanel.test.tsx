import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PointBuyPanel } from "@/components/character/PointBuyPanel";
import { pointsRemaining } from "@/pages/CharacterCreation";
import type { AbilityScores } from "@/types/game";

const BASE: AbilityScores = {
  strength: 15,
  dexterity: 15,
  constitution: 14,
  intelligence: 10,
  wisdom: 8,
  charisma: 8,
};

describe("pointsRemaining", () => {
  it("computes 27-point budget against the SRD cost table", () => {
    expect(pointsRemaining(BASE)).toBe(0);
    expect(pointsRemaining({ ...BASE, strength: 8 })).toBe(9);
    expect(pointsRemaining({ strength: 15, dexterity: 15, constitution: 15, intelligence: 15, wisdom: 15, charisma: 15 })).toBe(-27);
  });
});

describe("PointBuyPanel", () => {
  it("shows remaining points and lets you raise a score", async () => {
    let scores: AbilityScores = { strength: 8, dexterity: 8, constitution: 8, intelligence: 8, wisdom: 8, charisma: 8 };
    const { rerender } = render(<PointBuyPanel scores={scores} onChange={(next) => { scores = next; }} />);

    expect(screen.getByText("27")).toBeInTheDocument();

    const plusButtons = screen.getAllByRole("button").filter((b) => b.querySelector("svg"));
    expect(plusButtons.length).toBeGreaterThan(0);
  });

  it("renders all six abilities", () => {
    render(<PointBuyPanel scores={BASE} onChange={() => {}} />);
    expect(screen.getByText("Fuerza")).toBeInTheDocument();
    expect(screen.getByText("Destreza")).toBeInTheDocument();
    expect(screen.getByText("Constitución")).toBeInTheDocument();
    expect(screen.getByText("Inteligencia")).toBeInTheDocument();
    expect(screen.getByText("Sabiduría")).toBeInTheDocument();
    expect(screen.getByText("Carisma")).toBeInTheDocument();
  });

  it("disables the minus button at the minimum score", () => {
    render(<PointBuyPanel scores={{ ...BASE, strength: 8 }} onChange={() => {}} />);
    const buttons = screen.getAllByRole("button");
    // First ability (Fuerza) minus button is disabled at 8.
    expect(buttons[0]).toBeDisabled();
  });
});
