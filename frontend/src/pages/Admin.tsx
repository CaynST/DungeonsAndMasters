import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Shield, Trash2 } from "lucide-react";
import { api, getStoredUser } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { cn } from "@/lib/utils";

type Tab = "users" | "campaigns" | "characters";

export function Admin() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = getStoredUser();
  const [tab, setTab] = useState<Tab>("users");

  const users = useQuery({ queryKey: ["admin", "users"], queryFn: api.adminUsers });
  const campaigns = useQuery({ queryKey: ["admin", "campaigns"], queryFn: api.adminCampaigns });
  const characters = useQuery({ queryKey: ["admin", "characters"], queryFn: api.adminCharacters });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["admin"] });
    queryClient.invalidateQueries({ queryKey: ["campaigns"] });
    queryClient.invalidateQueries({ queryKey: ["characters"] });
  };

  const removeUser = useMutation({ mutationFn: api.adminDeleteUser, onSuccess: invalidate });
  const removeCampaign = useMutation({ mutationFn: api.adminDeleteCampaign, onSuccess: invalidate });
  const removeCharacter = useMutation({ mutationFn: api.adminDeleteCharacter, onSuccess: invalidate });

  const tabItems = [
    ["users", `Usuarios (${users.data?.length ?? 0})`],
    ["campaigns", `Campañas (${campaigns.data?.length ?? 0})`],
    ["characters", `Personajes (${characters.data?.length ?? 0})`],
  ] as const;

  const handleTabKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
    const keys = ["ArrowRight", "ArrowLeft", "Home", "End"];
    if (!keys.includes(event.key)) return;
    event.preventDefault();

    let nextIndex = index;
    if (event.key === "ArrowRight") nextIndex = (index + 1) % tabItems.length;
    else if (event.key === "ArrowLeft") nextIndex = (index - 1 + tabItems.length) % tabItems.length;
    else if (event.key === "Home") nextIndex = 0;
    else nextIndex = tabItems.length - 1;

    const nextTab = tabItems[nextIndex][0];
    setTab(nextTab);
    document.getElementById(`admin-tab-${nextTab}`)?.focus();
  };

  if (user?.role !== "ADMIN") {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="panel p-8 text-center">
          <Shield className="mx-auto mb-3 h-8 w-8 text-[#d0566a]" aria-hidden="true" />
          <h1 className="title-display text-lg text-ink">Acceso restringido</h1>
          <p className="mt-1 text-ink">Solo los administradores pueden acceder aquí.</p>
          <Button className="mt-4" type="button" onClick={() => navigate("/")}>
            Volver
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-6 py-8">
      <header className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Shield className="h-6 w-6 text-[#c9a227]" aria-hidden="true" />
          <div>
            <h1 className="title-display text-2xl font-bold text-parchment">Administración</h1>
            <p className="text-xs text-muted">Gestiona usuarios, campañas y personajes del sistema.</p>
          </div>
        </div>
        <Button variant="ghost" type="button" onClick={() => navigate("/")}>
          <ArrowLeft className="h-4 w-4" /> Volver
        </Button>
      </header>

      <div
        role="tablist"
        aria-label="Secciones de administración"
        className="mb-4 flex gap-1 rounded-lg border border-[#2b2118] bg-[#100d0a] p-1"
      >
        {tabItems.map(([id, label], index) => (
          <button
            key={id}
            id={`admin-tab-${id}`}
            type="button"
            role="tab"
            aria-selected={tab === id}
            aria-controls="admin-panel"
            tabIndex={tab === id ? 0 : -1}
            onClick={() => setTab(id)}
            onKeyDown={(event) => handleTabKeyDown(event, index)}
            className={cn(
              "flex-1 rounded-md px-3 py-2 text-sm transition",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-[#100d0a]",
              tab === id ? "bg-[#c9a227]/15 text-[#e0b84a]" : "text-muted hover:text-ink",
            )}
          >
            {label}
          </button>
        ))}
      </div>

      <div
        role="tabpanel"
        id="admin-panel"
        aria-labelledby={`admin-tab-${tab}`}
        tabIndex={0}
        className="panel p-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold/70"
      >
        {tab === "users" && (
          <Table
            loading={users.isLoading}
            caption="Usuarios registrados"
            headers={["Usuario", "Rol", "Personajes", ""]}
            rows={(users.data ?? []).map((u) => [
              <span className="text-ink">{u.displayName} <span className="text-muted">@{u.username}</span></span>,
              <Badge tone={u.role === "ADMIN" ? "gold" : "muted"}>{u.role}</Badge>,
              String(u.characterCount),
              <DeleteButton
                disabled={u.id === user?.id}
                pending={removeUser.isPending}
                onConfirm={() => removeUser.mutate(u.id)}
              />,
            ])}
          />
        )}

        {tab === "campaigns" && (
          <Table
            loading={campaigns.isLoading}
            caption="Campañas del sistema"
            headers={["Campaña", "Edición", "Estado", "Miembros", "Dueño", ""]}
            rows={(campaigns.data ?? []).map((c) => [
              <span className="text-ink">{c.name}</span>,
              c.edition,
              <Badge tone={c.status === "ACTIVE" ? "emerald" : "muted"}>{c.status}</Badge>,
              String(c.memberCount),
              c.owner,
              <DeleteButton pending={removeCampaign.isPending} onConfirm={() => removeCampaign.mutate(c.id)} />,
            ])}
          />
        )}

        {tab === "characters" && (
          <Table
            loading={characters.isLoading}
            caption="Personajes del sistema"
            headers={["Personaje", "Raza / Clase", "Nv", "HP", "Dueño", ""]}
            rows={(characters.data ?? []).map((c) => [
              <span className="text-ink">{c.name}</span>,
              `${c.race} / ${c.cls}`,
              String(c.level),
              `${c.hp}/${c.maxHp}`,
              c.owner,
              c.predefined ? (
                <span className="text-xs text-muted">predefinido</span>
              ) : (
                <DeleteButton pending={removeCharacter.isPending} onConfirm={() => removeCharacter.mutate(c.id)} />
              ),
            ])}
          />
        )}
      </div>
    </div>
  );
}

function Table({
  loading,
  caption,
  headers,
  rows,
}: {
  loading: boolean;
  caption: string;
  headers: string[];
  rows: React.ReactNode[][];
}) {
  if (loading) {
    return (
      <p role="status" className="flex items-center gap-2 p-4 text-sm text-muted">
        <Spinner /> Cargando...
      </p>
    );
  }
  if (rows.length === 0) {
    return (
      <p role="status" className="p-4 text-sm text-muted">
        No hay datos.
      </p>
    );
  }
  return (
    <table className="w-full text-left text-sm">
      <caption className="sr-only">{caption}</caption>
      <thead>
        <tr className="border-b border-[#2b2118] text-[11px] uppercase tracking-wider text-muted">
          {headers.map((header, index) => (
            <th key={index} scope="col" className="px-3 py-2 font-medium">
              {header}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, rowIndex) => (
          <tr key={rowIndex} className="border-b border-[#1f1810] last:border-0">
            {row.map((cell, cellIndex) => (
              <td key={cellIndex} className="px-3 py-2">
                {cell}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function DeleteButton({
  onConfirm,
  pending,
  disabled,
}: {
  onConfirm: () => void;
  pending: boolean;
  disabled?: boolean;
}) {
  const [confirming, setConfirming] = useState(false);
  if (disabled) {
    return <span className="text-xs text-muted">tú</span>;
  }
  if (!confirming) {
    return (
      <Button size="sm" variant="ghost" type="button" onClick={() => setConfirming(true)}>
        <Trash2 className="h-3.5 w-3.5" /> Borrar
      </Button>
    );
  }
  return (
    <span role="group" aria-label="Confirmar borrado" className="inline-flex gap-1">
      <Button size="sm" variant="danger" type="button" loading={pending} autoFocus onClick={onConfirm}>
        Confirmar
      </Button>
      <Button size="sm" variant="ghost" type="button" onClick={() => setConfirming(false)}>
        No
      </Button>
    </span>
  );
}
