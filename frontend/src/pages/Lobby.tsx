import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { Check, Clock3, Crown, Flame, Play, Repeat } from "lucide-react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { HealthBar } from "@/components/ui/HealthBar";
import { abilityModifier } from "@/lib/utils";
import { getStoredUser } from "@/lib/api";
import type { PlayerCharacter } from "@/types/game";

export function Lobby() {
  const { id } = useParams();
  const campaignId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = getStoredUser();
  const [selectedCharacter, setSelectedCharacter] = useState<number | null>(null);

  const campaign = useQuery({ queryKey: ["campaign", campaignId], queryFn: () => api.campaign(campaignId) });
  const characters = useQuery({ queryKey: ["characters"], queryFn: api.characters });

  const join = useMutation({
    mutationFn: (characterId: number) => api.joinCampaign(campaignId, characterId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["campaign", campaignId] }),
  });

  const start = useMutation({
    mutationFn: () => api.startCampaign(campaignId),
    onSuccess: () => navigate(`/campaign/${campaignId}/play`),
  });

  const isMember = campaign.data?.members.some((member) => member.userId === user?.id) ?? false;

  return (
    <div className="relative mx-auto max-w-6xl px-6 py-8">
      <Fireflies />

      <header className="relative z-10 mb-8 text-center">
        <div className="mb-2 flex items-center justify-center gap-2 text-gold">
          <Flame className="h-5 w-5 animate-flame" />
          <span className="text-xs uppercase tracking-[0.4em] text-muted">El Lobby · Taberna</span>
          <Flame className="h-5 w-5 animate-flame" />
        </div>
        <h1 className="title-display text-4xl font-bold text-parchment text-glow-gold">
          {campaign.data?.name ?? "Cargando..."}
        </h1>
        <p className="mx-auto mt-2 max-w-2xl text-sm text-muted">{campaign.data?.setting}</p>
      </header>

      <div className="relative z-10 grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        {/* Party + DM */}
        <div className="panel-wood panel p-6">
          <h2 className="title-display mb-4 text-lg text-parchment">La Compañía</h2>
          <div className="mb-5 flex items-start gap-4 rounded-lg border border-gold/20 bg-black/30 p-4">
            <Avatar name={campaign.data?.dmName ?? "DM"} />
            <div>
              <div className="flex items-center gap-2">
                <Crown className="h-4 w-4 text-gold" />
                <span className="title-display text-parchment">{campaign.data?.dmName ?? "El Dungeon Master"}</span>
              </div>
              <p className="mt-1 text-xs text-muted">
                Narrador. Conoce el estado de la partida y decide la historia, las pruebas y los enemigos.
              </p>
            </div>
          </div>

          <div className="space-y-2">
            {(campaign.data?.members ?? []).length === 0 && (
              <p className="rounded-lg border border-dashed border-parchment/15 p-4 text-center text-sm text-muted">
                Nadie se ha unido todavía. Elige tu héroe y toma asiento.
              </p>
            )}
            {(campaign.data?.members ?? []).map((member) => (
              <div
                key={member.id}
                className="flex items-center justify-between rounded-lg border border-parchment/10 bg-surface/60 px-4 py-3"
              >
                <div className="flex items-center gap-3">
                  <Avatar name={member.displayName} small />
                  <div>
                    <p className="text-sm font-medium text-ink">{member.displayName}</p>
                    <p className="text-xs text-muted">{member.characterName ?? "sin personaje"}</p>
                  </div>
                </div>
                <Badge tone={member.ready ? "emerald" : "muted"}>
                  {member.ready ? <Check className="h-3 w-3" /> : <Clock3 className="h-3 w-3" />}
                  {member.ready ? "Listo" : "Esperando"}
                </Badge>
              </div>
            ))}
          </div>
        </div>

        {/* Character selection + start */}
        <div className="space-y-6">
          <div className="panel p-6">
            <h2 className="title-display mb-3 text-lg text-parchment">Elige tu héroe</h2>
            <div className="space-y-2">
              {(characters.data ?? []).map((character) => (
                <CharacterChoice
                  key={character.id}
                  character={character}
                  selected={selectedCharacter === character.id}
                  onSelect={() => setSelectedCharacter(character.id)}
                />
              ))}
            </div>

            {isMember ? (
              <div className="mt-4">
                <Button
                  variant="arcane"
                  size="lg"
                  className="w-full"
                  onClick={() => start.mutate()}
                  disabled={start.isPending}
                >
                  <Play className="h-4 w-4" />
                  {start.isPending ? "Abriendo la cripta..." : "Comenzar aventura"}
                </Button>
                {start.isError && (
                  <p className="mt-2 text-xs text-crimson-soft">{(start.error as Error).message}</p>
                )}
              </div>
            ) : (
              <div className="mt-4">
                <Button
                  variant="primary"
                  size="lg"
                  className="w-full"
                  disabled={!selectedCharacter || join.isPending}
                  onClick={() => selectedCharacter && join.mutate(selectedCharacter)}
                >
                  <Repeat className="h-4 w-4" />
                  {join.isPending ? "Uniéndose..." : "Unirme a la partida"}
                </Button>
                {!selectedCharacter && (
                  <p className="mt-2 text-center text-xs text-muted">Selecciona un héroe para continuar.</p>
                )}
              </div>
            )}
          </div>

          <Button variant="ghost" className="w-full" onClick={() => navigate("/")}>
            ← Volver a la sala de aventuras
          </Button>
        </div>
      </div>
    </div>
  );
}

function CharacterChoice({
  character,
  selected,
  onSelect,
}: {
  character: PlayerCharacter;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      onClick={onSelect}
      className={`w-full rounded-lg border p-3 text-left transition ${
        selected
          ? "border-gold/60 bg-gold/10 shadow-glow"
          : "border-parchment/10 bg-surface/50 hover:border-gold/30"
      }`}
    >
      <div className="flex items-center gap-3">
        <Avatar name={character.name} color={character.color} small />
        <div className="min-w-0 flex-1">
          <p className="title-display text-sm text-parchment">{character.name}</p>
          <p className="text-[11px] uppercase tracking-wider text-muted">
            {character.race} · {character.cls}
          </p>
          <HealthBar value={character.hp} max={character.maxHp} showLabel={false} className="mt-2" />
        </div>
        <div className="text-right text-xs text-muted">
          <p>
            CA <span className="font-semibold text-ink">{character.ac}</span>
          </p>
          <p>
            DES <span className="font-semibold text-ink">{abilityModifier(character.stats.dexterity) >= 0 ? "+" : ""}{abilityModifier(character.stats.dexterity)}</span>
          </p>
        </div>
      </div>
    </button>
  );
}

function Avatar({ name, color, small = false }: { name: string; color?: string; small?: boolean }) {
  const size = small ? "h-10 w-10 text-sm" : "h-12 w-12 text-base";
  return (
    <span
      className={`title-display flex ${size} shrink-0 items-center justify-center rounded-full border-2 font-bold`}
      style={{ borderColor: color ?? "#C9A227", color: color ?? "#C9A227" }}
    >
      {name.charAt(0)}
    </span>
  );
}

function Fireflies() {
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden">
      {Array.from({ length: 14 }).map((_, index) => (
        <motion.span
          key={index}
          className="absolute h-1.5 w-1.5 rounded-full bg-gold/60"
          style={{ left: `${(index * 37) % 100}%`, top: `${(index * 53) % 100}%` }}
          animate={{ y: [0, -14, 0], opacity: [0.2, 0.8, 0.2] }}
          transition={{ duration: 4 + (index % 5), repeat: Infinity, delay: index * 0.3 }}
        />
      ))}
    </div>
  );
}
