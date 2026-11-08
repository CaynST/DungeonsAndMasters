import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Button } from "@/components/ui/Button";

describe("Button", () => {
  it("keeps its accessible name and busy state while loading", () => {
    render(<Button loading>Guardar héroe</Button>);

    const button = screen.getByRole("button", { name: "Guardar héroe" });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute("aria-busy", "true");
  });
});
