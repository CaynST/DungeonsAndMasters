import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { HealthBar } from "@/components/ui/HealthBar";

describe("HealthBar", () => {
  it("exposes HP as a labelled progressbar even without the visual label", () => {
    render(<HealthBar value={12} max={20} showLabel={false} />);

    const bar = screen.getByRole("progressbar", { name: "Puntos de golpe" });
    expect(bar).toHaveAttribute("aria-valuenow", "12");
    expect(bar).toHaveAttribute("aria-valuemax", "20");
    expect(bar).toHaveAttribute("aria-valuetext", "12 de 20");
    expect(screen.queryByText("HP")).not.toBeInTheDocument();
  });

  it("clamps values and keeps a safe maximum when data is missing", () => {
    render(<HealthBar value={30} max={0} showLabel={false} />);

    const bar = screen.getByRole("progressbar", { name: "Puntos de golpe" });
    expect(bar).toHaveAttribute("aria-valuemax", "0");
    expect(bar).toHaveAttribute("aria-valuenow", "0");
  });
});
