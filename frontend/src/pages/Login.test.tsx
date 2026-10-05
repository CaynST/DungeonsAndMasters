import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Login } from "@/pages/Login";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

function renderLogin() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("Login", () => {
  it("shows login fields and toggles to register", async () => {
    renderLogin();
    expect(screen.getByText("Dungeons & Masters")).toBeInTheDocument();
    expect(screen.getByText("Entrar a la taberna")).toBeInTheDocument();

    await userEvent.click(screen.getByText("Crear cuenta"));
    expect(screen.getByText("Forjar aventurero")).toBeInTheDocument();
    expect(screen.getByText("Nombre para mostrar")).toBeInTheDocument();
  });
});
