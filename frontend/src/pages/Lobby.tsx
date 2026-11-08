import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion, useReducedMotion } from "framer-motion";
import {
  AlertTriangle,
  ArrowLeft,
  Check,
  Clock3,
  Crown,
  Flame,
  Play,
  Plus,
  RefreshCw,
  Repeat,
  UserRound,
  Users,
} from "lucide-react";
import { api, getStoredUser } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Avatar } from "@/components/ui/Avatar";
import { EmptyState } from "@/components/ui/EmptyState";
import { HealthBar } from "@/components/ui/HealthBar";
import { Skeleton } from "@/components/ui/Skeleton";
import { campaignPhaseLabel, campaignStatusLabel, campaignStatusTone } from "@/lib/campaigns";
import { abilityModifier, cn } from "@/lib/utils";
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
  const ruleset = useQuery({
    queryKey: ["ruleset", campaign.data?.edition],
    queryFn: () => api.ruleset(campaign.data!.edition!),
    enabled: Boolean(campaign.data?.edition),
  });

  const join = useMutation({
    mutationFn: (characterId: number) => api.joinCampaign(campaignId, characterId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["campaign", campaignId] }),
  });

  const start = useMutation({
    mutationFn: () => api.startCampaign(campaignId),
    onSuccess: () => navigate(`/campaign/${campaignId}/play`),
  });

  const members = campaign.data?.members ?? [];
  const heroes = characters.data ?? [];
  const isMember = members.some((member) => member.userId === user?.id);
  const readyCount = members.filter((member) => member.ready).length;
  const selectedHero = heroes.find((character) => character.id === selectedCharacter);

  return (
    <div className="relative mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-10">
      <Fireflies />

      {campaign.isLoading ? (
        <LobbySkeleton />
      ) : campaign.isError || !campaign.data ? (
        <div className="relative z-10 mx-auto mt-10 max-w-xl">
          <EmptyState
            icon={AlertTriangle}
            title="No se pudo abrir el lobby"
            description="La taberna no responde o la campaña ya no está disponible. Comprueba la conexión e inténtalo de nuevo."
            action={
              <div className="flex flex-wrap justify-center gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  loading={campaign.isFetching}
                  onClick={() => campaign.refetch()}
                >
                  <RefreshCw className="h-4 w-4" /> Reintentar
                </Button>
                <Button variant="ghost" size="sm" onClick={() => navigate("/")}>
                  <ArrowLeft className="h-4 w-4" /> Volver a la sala
                </Button>
              </div>
            }
          />
        </div>
      ) : (
        <>
          <header className="relative z-10 mb-8 overflow-hidden rounded-xl border border-gold/20 bg-surface/60 shadow-panel">
            <img
              src={campaign.data.image ?? "/images/campaigns/valdros.png"}
              alt=""
              className="absolute inset-0 h-full w-full object-cover opacity-25"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-[#0b0907] via-[#0b0907]/75 to-[#0b0907]/35" />
            <div className="relative px-5 py-8 text-center sm:px-10">
              <div className="mb-2 flex items-center justify-center gap-2 text-gold">
                <Flame className="h-4 w-4 animate-flame" aria-hidden="true" />
                <span className="text-[11px] uppercase tracking-[0.4em] text-muted">El Lobby · Taberna</span>
                <Flame className="h-4 w-4 animate-flame" aria-hidden="true" />
              </div>
              <h1 className="title-display text-3xl font-bold text-parchment text-glow-gold sm:text-4xl">
                {campaign.data.name}
              </h1>
              {campaign.data.setting && (
                <p className="mx-auto mt-2 max-w-2xl text-sm leading-relaxed text-muted">
                  {campaign.data.setting}
                </p>
              )}
              <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
                <Badge tone={campaignStatusTone(campaign.data.status)}>
                  {campaignStatusLabel(campaign.data.status)}
                </Badge>
                <Badge tone="muted">{campaignPhaseLabel(campaign.data.phase)}</Badge>
                {ruleset.data && <Badge tone="arcane">{ruleset.data.displayName}</Badge>}
                <span className="inline-flex items-center gap-1.5 text-xs text-muted">
                  <Users className="h-3.5 w-3.5" aria-hidden="true" />
                  {members.length} aventurero{members.length === 1 ? "" : "s"}
                </span>
              </div>
            </div>
          </header>

          <div className="relative z-10 grid gap-6 lg:grid-cols-[1.45fr_1fr] lg:items-start">
            {/* Party + DM */}
            <section className="panel-wood panel p-5 sm:p-6" aria-labelledby="party-title">
              <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-[10px] font-semibold uppercase tracking-[0.3em] text-gold/75">Quiénes juegan</p>
                  <h2 id="party-title" className="title-display mt-1 text-xl text-parchment">
                    La Compañía
                  </h2>
                  <p className="mt-1 text-xs leading-relaxed text-muted">
                    El Dungeon Master y los aventureros que ya ocupan su asiento.
                  </p>
                </div>
                {members.length > 0 && (
                  <Badge tone={readyCount === members.length ? "emerald" : "muted"}>
                    {readyCount} de {members.length} listos
                  </Badge>
                )}
              </div>

              <div className="flex items-start gap-3 rounded-lg border border-gold/20 bg-black/30 p-3.5 sm:gap-4 sm:p-4">
                <Avatar
                  name={campaign.data.dmName}
                  src={campaign.data.dmAvatar}
                  size="md"
                />
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <Crown className="h-4 w-4 text-gold" aria-hidden="true" />
                    <span className="title-display text-parchment">{campaign.data.dmName}</span>
                    <Badge tone="gold">Narrador</Badge>
                  </div>
                  <p className="mt-1 text-xs leading-relaxed text-muted">
                    Conoce el estado de la partida y decide la historia, las pruebas y los enemigos.
                  </p>
                </div>
              </div>

              <div className="divider my-5" />

              <div className="space-y-2">
                {members.length === 0 && (
                  <EmptyState
                    icon={Users}
                    title="Nadie ha tomado asiento todavía"
                    description="Elige tu héroe y sé el primero en unirte a la mesa."
                    compact
                  />
                )}
                {members.map((member) => (
                  <div
                    key={member.id}
                    className="flex items-center justify-between gap-3 rounded-lg border border-parchment/10 bg-surface/60 px-3 py-2.5"
                  >
                    <div className="flex min-w-0 items-center gap-3">
                      <Avatar name={member.displayName} size="sm" />
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-1.5">
                          <p className="truncate text-sm font-medium text-ink">{member.displayName}</p>
                          {member.userId === user?.id && <Badge tone="gold">Tú</Badge>}
                        </div>
                        <p
                          className={cn(
                            "truncate text-xs",
                            member.characterName ? "text-muted" : "italic text-muted/80",
                          )}
                        >
                          {member.characterName ?? "Sin héroe elegido"}
                        </p>
                      </div>
                    </div>
                    <Badge tone={member.ready ? "emerald" : "muted"}>
                      {member.ready ? <Check className="h-3 w-3" /> : <Clock3 className="h-3 w-3" />}
                      {member.ready ? "Listo" : "Esperando"}
                    </Badge>
                  </div>
                ))}
              </div>
            </section>

            {/* Character selection + start */}
            <div className="space-y-6">
              <section className="panel p-5 sm:p-6" aria-labelledby="choose-hero-title">
                <div className="mb-4">
                  <p className="text-[10px] font-semibold uppercase tracking-[0.3em] text-gold/75">Tu asiento</p>
                  <div className="mt-1 flex items-end justify-between gap-3">
                    <h2 id="choose-hero-title" className="title-display text-xl text-parchment">
                      Elige tu héroe
                    </h2>
                    {heroes.length > 0 && (
                      <span className="shrink-0 text-xs text-muted">
                        {heroes.length} disponible{heroes.length === 1 ? "" : "s"}
                      </span>
                    )}
                  </div>
                </div>

                {characters.isLoading ? (
                  <div className="space-y-2" aria-busy="true">
                    <span className="sr-only">Cargando héroes…</span>
                    {[0, 1, 2].map((index) => (
                      <Skeleton key={index} className="h-16 w-full rounded-lg" />
                    ))}
                  </div>
                ) : characters.isError ? (
                  <EmptyState
                    icon={AlertTriangle}
                    title="No se pudieron cargar tus héroes"
                    description="Comprueba la conexión con la taberna."
                    compact
                    action={
                      <Button
                        variant="secondary"
                        size="sm"
                        loading={characters.isFetching}
                        onClick={() => characters.refetch()}
                      >
                        <RefreshCw className="h-4 w-4" /> Reintentar
                      </Button>
                    }
                  />
                ) : heroes.length === 0 ? (
                  <EmptyState
                    icon={UserRound}
                    title="No tienes héroes todavía"
                    description="Forja un personaje para ocupar tu asiento en esta campaña."
                    compact
                    action={
                      <Button variant="primary" size="sm" onClick={() => navigate("/characters/new")}>
                        <Plus className="h-4 w-4" /> Crear personaje
                      </Button>
                    }
                  />
                ) : (
                  <div className="max-h-[22rem] space-y-2 overflow-y-auto pr-1">
                    {heroes.map((character) => (
                      <CharacterChoice
                        key={character.id}
                        character={character}
                        selected={selectedCharacter === character.id}
                        onSelect={() => setSelectedCharacter(character.id)}
                      />
                    ))}
                  </div>
                )}

                <div className="divider my-4" />

                {isMember ? (
                  <div className="space-y-2">
                    <p className="text-xs leading-relaxed text-muted">
                      Ya formas parte de la compañía. Cuando la mesa esté preparada, abre la aventura.
                    </p>
                    <Button
                      variant="primary"
                      size="lg"
                      className="w-full"
                      loading={start.isPending}
                      onClick={() => start.mutate()}
                    >
                      <Play className="h-4 w-4" />
                      Comenzar aventura
                    </Button>
                    {start.isError && (
                      <p role="alert" className="text-xs text-crimson-soft">
                        No se pudo comenzar la aventura. Inténtalo de nuevo.
                      </p>
                    )}
                  </div>
                ) : (
                  <div className="space-y-2">
                    <p className="text-xs leading-relaxed text-muted">
                      {selectedHero ? (
                        <>
                          Entrarás en la mesa como{" "}
                          <span className="font-medium text-parchment">{selectedHero.name}</span>.
                        </>
                      ) : (
                        "Selecciona un héroe para ocupar tu asiento."
                      )}
                    </p>
                    <Button
                      variant="primary"
                      size="lg"
                      className="w-full"
                      loading={join.isPending}
                      disabled={!selectedCharacter}
                      onClick={() => selectedCharacter && join.mutate(selectedCharacter)}
                    >
                      <Repeat className="h-4 w-4" />
                      Unirme a la partida
                    </Button>
                    {join.isError && (
                      <p role="alert" className="text-xs text-crimson-soft">
                        No se pudo unir el héroe a la campaña. Inténtalo de nuevo.
                      </p>
                    )}
                  </div>
                )}
              </section>

              <Button variant="ghost" className="w-full" onClick={() => navigate("/")}>
                <ArrowLeft className="h-4 w-4" /> Volver a la sala de aventuras
              </Button>
            </div>
          </div>
        </>
      )}
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
      type="button"
      onClick={onSelect}
      aria-pressed={selected}
      className={cn(
        "relative w-full rounded-lg border p-3 text-left transition",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold/60 focus-visible:ring-offset-2 focus-visible:ring-offset-[#1e160f]",
        selected
          ? "border-gold/60 bg-gold/10 shadow-glow"
          : "border-parchment/10 bg-surface/50 hover:border-gold/30 hover:bg-surface/80",
      )}
    >
      <div className="flex items-center gap-3">
        <Avatar
          name={character.name}
          src={character.portrait}
          color={character.color}
          size="sm"
        />
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-1.5">
            <p className="title-display truncate text-sm text-parchment">{character.name}</p>
            {selected && <Check className="h-3.5 w-3.5 shrink-0 text-gold-soft" aria-hidden="true" />}
          </div>
          <p className="truncate text-[11px] uppercase tracking-wider text-muted">
            {character.race} · {character.cls} · Nv {character.level}
          </p>
          <HealthBar value={character.hp} max={character.maxHp} showLabel={false} className="mt-2" />
        </div>
        <div className="shrink-0 text-right text-[11px] text-muted">
          <p>
            CA <span className="font-semibold text-ink">{character.ac}</span>
          </p>
          <p>
            DES{" "}
            <span className="font-semibold text-ink">
              {abilityModifier(character.stats.dexterity) >= 0 ? "+" : ""}
              {abilityModifier(character.stats.dexterity)}
            </span>
          </p>
        </div>
      </div>
    </button>
  );
}

function LobbySkeleton() {
  return (
    <div className="relative z-10" aria-busy="true">
      <span className="sr-only">Cargando el lobby…</span>
      <div className="mb-8 overflow-hidden rounded-xl border border-gold/20 bg-surface/40 px-6 py-10 text-center shadow-panel">
        <Skeleton className="mx-auto h-3 w-40" />
        <Skeleton className="mx-auto mt-4 h-10 w-2/3" />
        <Skeleton className="mx-auto mt-3 h-3 w-1/2" />
        <div className="mt-5 flex justify-center gap-2">
          <Skeleton className="h-5 w-24 rounded-full" />
          <Skeleton className="h-5 w-20 rounded-full" />
          <Skeleton className="h-5 w-16 rounded-full" />
        </div>
      </div>
      <div className="grid gap-6 lg:grid-cols-[1.45fr_1fr]">
        <div className="panel space-y-3 p-6">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-6 w-40" />
          <Skeleton className="h-20 w-full rounded-lg" />
          <Skeleton className="h-14 w-full rounded-lg" />
          <Skeleton className="h-14 w-full rounded-lg" />
        </div>
        <div className="panel space-y-3 p-6">
          <Skeleton className="h-3 w-20" />
          <Skeleton className="h-6 w-36" />
          <Skeleton className="h-16 w-full rounded-lg" />
          <Skeleton className="h-16 w-full rounded-lg" />
          <Skeleton className="h-12 w-full rounded-lg" />
        </div>
      </div>
    </div>
  );
}

function Fireflies() {
  const reduceMotion = useReducedMotion();
  if (reduceMotion) return null;

  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
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
