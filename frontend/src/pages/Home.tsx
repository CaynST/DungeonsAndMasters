import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Camera, LogOut, Plus, Scroll, Shield, Swords, Trash2, User } from "lucide-react";
import { api, clearSession, getStoredUser } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { AbilityScore } from "@/components/ui/AbilityScore";
import { HealthBar } from "@/components/ui/HealthBar";
import { CampaignCard } from "@/components/campaign/CampaignCard";
import { abilityModifier } from "@/lib/utils";
import type { PlayerCharacter } from "@/types/game";

export function Home() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = getStoredUser();
  const [newName, setNewName] = useState("");
  const [newEdition, setNewEdition] = useState("dnd-5e-2014");
  const [showCreate, setShowCreate] = useState(false);

  const campaigns = useQuery({ queryKey: ["campaigns"], queryFn: api.campaigns });
  const characters = useQuery({ queryKey: ["characters"], queryFn: api.characters });

  const createCampaign = useMutation({
    mutationFn: () => api.createCampaign(newName || "La Cripta de Valdros", newEdition),
    onSuccess: (campaign) => {
      queryClient.invalidateQueries({ queryKey: ["campaigns"] });
      navigate(`/campaign/${campaign.id}/lobby`);
    },
  });

  const logout = () => {
    clearSession();
    navigate("/login", { replace: true });
  };

  const deleteCharacter = useMutation({
    mutationFn: (id: number) => api.deleteCharacter(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["characters"] }),
  });

  const uploadPortrait = useMutation({
    mutationFn: ({ id, file }: { id: number; file: File }) => api.uploadPortrait(id, file),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["characters"] }),
  });

  return (
    <div className="mx-auto max-w-7xl px-6 py-8">
      <header className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="title-display text-3xl font-bold text-parchment text-glow-gold">Sala de Aventuras</h1>
          <p className="mt-1 text-sm text-muted">
            Bienvenido, <span className="text-ink">{user?.displayName ?? "aventurero"}</span>. Elige tu campaña.
          </p>
        </div>
        <div className="flex items-center gap-3">
          {user?.role === "ADMIN" && (
            <Button variant="ghost" size="sm" onClick={() => navigate("/admin")}>
              <Shield className="h-4 w-4" /> Administración
            </Button>
          )}
          <Badge tone="arcane">
            <User className="h-3 w-3" /> {user?.username ?? "invitado"}
          </Badge>
          <Button variant="ghost" size="sm" onClick={logout}>
            <LogOut className="h-4 w-4" /> Salir
          </Button>
        </div>
      </header>

      <section className="mb-10">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="title-display flex items-center gap-2 text-xl text-parchment">
            <Scroll className="h-5 w-5 text-gold" /> Campañas
          </h2>
          <Button variant="primary" size="sm" onClick={() => setShowCreate((value) => !value)}>
            <Plus className="h-4 w-4" /> Crear campaña
          </Button>
        </div>

        {showCreate && (
          <div className="panel mb-5 flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
            <input
              value={newName}
              onChange={(event) => setNewName(event.target.value)}
              placeholder="Nombre de la campaña (por defecto: La Cripta de Valdros)"
              className="h-10 flex-1 rounded-lg border border-parchment/15 bg-black/35 px-3 text-sm text-ink outline-none focus:border-gold/50"
            />
            <select
              value={newEdition}
              onChange={(event) => setNewEdition(event.target.value)}
              className="h-10 rounded-lg border border-parchment/15 bg-black/35 px-3 text-sm text-ink outline-none focus:border-gold/50"
            >
              <option value="dnd-5e-2014">D&D 5e (2014)</option>
              <option value="dnd-5e-2024">D&D 5e (2024)</option>
            </select>
            <Button
              variant="primary"
              onClick={() => createCampaign.mutate()}
              loading={createCampaign.isPending}
            >
              Crear y entrar
            </Button>
          </div>
        )}

        {campaigns.isLoading ? (
          <SkeletonRow />
        ) : campaigns.data && campaigns.data.length > 0 ? (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {campaigns.data.map((campaign) => (
              <CampaignCard key={campaign.id} campaign={campaign} />
            ))}
          </div>
        ) : (
          <div className="panel p-8 text-center text-sm text-muted">
            Aún no hay campañas. Crea la primera y desciende a la Cripta de Valdros.
          </div>
        )}
      </section>

      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="title-display flex items-center gap-2 text-xl text-parchment">
            <Swords className="h-5 w-5 text-crimson-soft" /> Tus héroes
          </h2>
          <Button variant="primary" size="sm" onClick={() => navigate("/characters/new")}>
            <Plus className="h-4 w-4" /> Crear personaje
          </Button>
        </div>
        <p className="mb-4 text-sm text-muted">
          Los personajes se construyen con las reglas del SRD (raza, clase, trasfondo, puntos).
        </p>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {characters.isLoading
            ? [0, 1, 2, 3].map((index) => <div key={index} className="panel h-64 animate-shimmer" />)
            : (characters.data ?? []).map((character) => (
                <HeroCard
                  key={character.id}
                  character={character}
                  onDelete={
                    character.predefined
                      ? undefined
                      : () => deleteCharacter.mutate(character.id)
                  }
                  onUploadPortrait={
                    character.predefined
                      ? undefined
                      : (file) => uploadPortrait.mutate({ id: character.id, file })
                  }
                />
              ))}
        </div>
      </section>
    </div>
  );
}

function HeroCard({
  character,
  onDelete,
  onUploadPortrait,
}: {
  character: PlayerCharacter;
  onDelete?: () => void;
  onUploadPortrait?: (file: File) => void;
}) {
  const fileRef = useRef<HTMLInputElement>(null);
  return (
    <div className="panel relative overflow-hidden p-0">
      {(onDelete || onUploadPortrait) && (
        <div className="absolute right-2 top-2 z-10 flex gap-1">
          {onUploadPortrait && (
            <>
              <button
                onClick={() => fileRef.current?.click()}
                title="Cambiar retrato"
                className="rounded-md border border-[#3a2a1a] bg-[#100d0a]/85 p-1.5 text-muted transition hover:border-[#c9a227]/50 hover:text-[#e0b84a]"
              >
                <Camera className="h-3.5 w-3.5" />
              </button>
              <input
                ref={fileRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) onUploadPortrait(file);
                  event.target.value = "";
                }}
              />
            </>
          )}
          {onDelete && (
            <button
              onClick={onDelete}
              title="Borrar personaje"
              className="rounded-md border border-[#3a2a1a] bg-[#100d0a]/85 p-1.5 text-muted transition hover:border-[#d0566a]/50 hover:text-[#d0566a]"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      )}
      <div
        className="h-28 w-full"
        style={{
          background: `radial-gradient(120px 80px at 50% 0%, ${character.color ?? "#C9A227"}55, transparent), linear-gradient(180deg, #211A14, #17130F)`,
        }}
      >
        <div className="flex h-full items-center justify-center">
          {character.portrait ? (
            <img
              src={character.portrait}
              alt={character.name}
              className="h-20 w-20 rounded-full border-2 object-cover"
              style={{ borderColor: character.color ?? "#C9A227" }}
            />
          ) : (
            <span
              className="title-display flex h-16 w-16 items-center justify-center rounded-full border text-2xl font-bold"
              style={{ borderColor: character.color, color: character.color }}
            >
              {character.name.charAt(0)}
            </span>
          )}
        </div>
      </div>
      <div className="space-y-3 p-4">
        <div>
          <h3 className="title-display text-lg font-semibold text-parchment">{character.name}</h3>
          <p className="text-xs uppercase tracking-wider text-muted">
            {character.race} · {character.cls} · Nv {character.level}
          </p>
        </div>
        <HealthBar value={character.hp} max={character.maxHp} />
        <div className="flex items-center justify-between text-xs text-muted">
          <span>
            CA <span className="font-semibold text-ink">{character.ac}</span>
          </span>
          <span>
            VEL <span className="font-semibold text-ink">{character.speed}</span>
          </span>
        </div>
        <div className="grid grid-cols-3 gap-1.5">
          {[
            ["FUE", character.stats.strength],
            ["DES", character.stats.dexterity],
            ["CON", character.stats.constitution],
            ["INT", character.stats.intelligence],
            ["SAB", character.stats.wisdom],
            ["CAR", character.stats.charisma],
          ].map(([label, value]) => (
            <AbilityScore key={label} label={label as string} value={value as number} modifier={abilityModifier(value as number)} />
          ))}
        </div>
      </div>
    </div>
  );
}

function SkeletonRow() {
  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {[0, 1, 2].map((index) => (
        <div key={index} className="panel h-64 animate-shimmer" />
      ))}
    </div>
  );
}
