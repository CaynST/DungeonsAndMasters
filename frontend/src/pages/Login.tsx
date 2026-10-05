import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Sword, Sparkles, Dices } from "lucide-react";
import { api, setSession } from "@/lib/api";
import { Button } from "@/components/ui/Button";

export function Login() {
  const navigate = useNavigate();
  const location = useLocation() as { state?: { from?: string } };
  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");

  const mutation = useMutation({
    mutationFn: () =>
      mode === "login"
        ? api.login(username, password)
        : api.register(username, password, displayName || username),
    onSuccess: (auth) => {
      setSession(auth);
      navigate(location.state?.from ?? "/", { replace: true });
    },
  });

  const errorMessage = mutation.error instanceof Error ? mutation.error.message : null;

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mb-3 flex justify-center gap-3 text-gold">
            <Sword className="h-7 w-7" />
            <Dices className="h-7 w-7" />
            <Sparkles className="h-7 w-7" />
          </div>
          <h1 className="title-display text-3xl font-bold text-parchment text-glow-gold">
            Dungeons &amp; Masters
          </h1>
          <p className="mt-1 text-xs uppercase tracking-[0.35em] text-muted">
            La Cripta de Valdros
          </p>
        </div>

        <div className="panel p-6">
          <div className="mb-5 flex rounded-lg border border-parchment/10 bg-black/30 p-1">
            {(["login", "register"] as const).map((value) => (
              <button
                key={value}
                onClick={() => setMode(value)}
                className={`flex-1 rounded-md py-2 text-sm font-medium transition ${
                  mode === value ? "bg-gold/90 text-[#241a06]" : "text-muted hover:text-ink"
                }`}
              >
                {value === "login" ? "Entrar" : "Crear cuenta"}
              </button>
            ))}
          </div>

          <form
            className="space-y-4"
            onSubmit={(event) => {
              event.preventDefault();
              mutation.mutate();
            }}
          >
            <Field label="Usuario">
              <input
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                placeholder="kael_elfuerte"
                autoComplete="username"
                className="input"
                required
                minLength={3}
              />
            </Field>

            {mode === "register" && (
              <Field label="Nombre para mostrar">
                <input
                  value={displayName}
                  onChange={(event) => setDisplayName(event.target.value)}
                  placeholder="Kael"
                  className="input"
                />
              </Field>
            )}

            <Field label="Contraseña">
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="••••••••"
                autoComplete={mode === "login" ? "current-password" : "new-password"}
                className="input"
                required
                minLength={4}
              />
            </Field>

            {errorMessage && (
              <p className="rounded-md border border-crimson/30 bg-crimson/10 px-3 py-2 text-sm text-crimson-soft">
                {errorMessage}
              </p>
            )}

            <Button type="submit" variant="primary" size="lg" className="w-full" disabled={mutation.isPending}>
              {mutation.isPending ? "Invocando..." : mode === "login" ? "Entrar a la taberna" : "Forjar aventurero"}
            </Button>
          </form>
        </div>

        <p className="mt-4 text-center text-xs text-muted">
          Prototipo — el Dungeon Master responde con proveedor Mock si no hay API configurada.
        </p>
      </div>

      <style>{`
        .input {
          width: 100%;
          height: 2.75rem;
          border-radius: 0.5rem;
          border: 1px solid rgba(216,195,154,0.15);
          background: rgba(0,0,0,0.35);
          padding: 0 0.85rem;
          color: #E8E1D5;
          outline: none;
        }
        .input:focus { border-color: rgba(201,162,39,0.55); box-shadow: 0 0 0 3px rgba(201,162,39,0.12); }
      `}</style>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-muted">{label}</span>
      {children}
    </label>
  );
}
