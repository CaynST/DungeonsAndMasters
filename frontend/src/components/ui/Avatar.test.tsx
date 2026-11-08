import { describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { Avatar } from "@/components/ui/Avatar";

describe("Avatar", () => {
  it("renders the portrait image when one is available", () => {
    const { container } = render(
      <Avatar name="Kael" src="/images/heroes/kael.png" color="#C9A227" size="md" />,
    );
    const image = container.querySelector("img");
    expect(image).not.toBeNull();
    expect(image).toHaveAttribute("src", "/images/heroes/kael.png");
  });

  it("falls back to initials when there is no portrait", () => {
    render(<Avatar name="Kael" />);
    expect(screen.getByText("K")).toBeInTheDocument();
  });

  it("uses two initials for display names", () => {
    render(<Avatar name="El Dungeon Master" />);
    expect(screen.getByText("EM")).toBeInTheDocument();
  });

  it("replaces a broken portrait with the initials fallback", () => {
    const { container } = render(
      <Avatar name="Lyra" src="/images/heroes/missing.png" />,
    );
    fireEvent.error(container.querySelector("img")!);
    expect(container.querySelector("img")).toBeNull();
    expect(screen.getByText("L")).toBeInTheDocument();
  });
});
