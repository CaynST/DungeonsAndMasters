import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Login } from "@/pages/Login";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const { loginMock } = vi.hoisted(() => ({ loginMock: vi.fn() }));

vi.mock("@/lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api")>();
  return {
    ...actual,
    setSession: vi.fn(),
    api: {
      ...actual.api,
      login: loginMock,
      register: vi.fn(),
    },
  };
});

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
  beforeEach(() => {
    loginMock.mockReset();
  });

  it("shows login fields and toggles to register", async () => {
    renderLogin();
    expect(screen.getByText("Dungeons & Masters")).toBeInTheDocument();
    expect(screen.getByText("Entrar a la taberna")).toBeInTheDocument();

    await userEvent.click(screen.getByText("Crear cuenta"));
    expect(screen.getByText("Forjar aventurero")).toBeInTheDocument();
    expect(screen.getByText("Nombre para mostrar")).toBeInTheDocument();
  });

  it("associates labels and exposes the selected access mode", async () => {
    renderLogin();

    expect(screen.getByLabelText("Usuario")).toBeInTheDocument();
    expect(screen.getByLabelText("Contraseña")).toBeInTheDocument();

    const loginMode = screen.getByRole("button", { name: "Entrar" });
    const registerMode = screen.getByRole("button", { name: "Crear cuenta" });
    expect(loginMode).toHaveAttribute("aria-pressed", "true");
    expect(registerMode).toHaveAttribute("aria-pressed", "false");

    await userEvent.click(registerMode);
    expect(registerMode).toHaveAttribute("aria-pressed", "true");
    expect(loginMode).toHaveAttribute("aria-pressed", "false");
  });

  it("announces authentication errors and marks the password field invalid", async () => {
    loginMock.mockRejectedValueOnce(new Error("Usuario o contraseña incorrectos"));
    renderLogin();

    await userEvent.type(screen.getByLabelText("Usuario"), "kael");
    await userEvent.type(screen.getByLabelText("Contraseña"), "secreto");
    await userEvent.click(screen.getByRole("button", { name: "Entrar a la taberna" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Usuario o contraseña incorrectos");
    expect(screen.getByLabelText("Contraseña")).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText("Contraseña")).toHaveAttribute("aria-describedby", "login-error");
  });
});
