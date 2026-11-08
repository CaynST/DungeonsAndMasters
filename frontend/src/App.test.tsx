import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import App from "@/App";

describe("App landmarks", () => {
  it("offers a skip link that targets the main landmark", async () => {
    localStorage.clear();
    render(<App />);

    expect(screen.getByRole("link", { name: "Saltar al contenido" })).toHaveAttribute(
      "href",
      "#main-content",
    );
    expect(screen.getByRole("main")).toHaveAttribute("id", "main-content");
    expect(
      await screen.findByRole("heading", { level: 1, name: /Dungeons & Masters/i }),
    ).toBeInTheDocument();
  });
});
