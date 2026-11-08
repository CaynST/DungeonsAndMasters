import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { type ReactNode, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  AlertTriangle,
  Camera,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  DoorOpen,
  Flame,
  LayoutGrid,
  LogOut,
  Plus,
  RefreshCw,
  Scroll,
  Shield,
  Sparkles,
  Swords,
  Trash2,
  UserRound,
  Users,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { api, clearSession, getStoredUser } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { AbilityScore } from "@/components/ui/AbilityScore";
import { Avatar } from "@/components/ui/Avatar";
import { EmptyState } from "@/components/ui/EmptyState";
import { HealthBar } from "@/components/ui/HealthBar";
import { Skeleton } from "@/components/ui/Skeleton";
import { CampaignCard } from "@/components/campaign/CampaignCard";
import { campaignPhaseLabel, campaignStatusLabel, campaignStatusTone } from "@/lib/campaigns";
import { abilityModifier, cn } from "@/lib/utils";
import type { CampaignStatusFilter, PlayerCharacter } from "@/types/game";

const CAMPAIGN_PAGE_SIZE = 6;
const CAMPAIGN_FILTERS: { value: CampaignStatusFilter; label: string; icon: LucideIcon }[] = [
  { value: "ALL", label: "Todas", icon: LayoutGrid },
  { value: "ACTIVE", label: "En curso", icon: Swords },
  { value: "LOBBY", label: "En lobby", icon: DoorOpen },
  { value: "FINISHED", label: "Completadas", icon: CheckCircle2 },
];

const FIELD_CLASS =
  "h-10 w-full rounded-lg border border-parchment/15 bg-black/35 px-3 text-sm text-ink outline-none transition focus:border-gold/50 focus:ring-2 focus:ring-gold/15";

export function Home() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = getStoredUser();
  const [newName, setNewName] = useState("");
  const [newEdition, setNewEdition] = useState("dnd-5e-2014");
  const [showCreate, setShowCreate] = useState(false);
  const [campaignPage, setCampaignPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<CampaignStatusFilter>("ALL");
  const createPanelRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (!showCreate) return;
    const panel = createPanelRef.current;
    if (panel && typeof panel.scrollIntoView === "function") {
      const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false;
      panel.scrollIntoView({ behavior: reduceMotion ? "auto" : "smooth", block: "nearest" });
    }
  }, [showCreate]);

  const campaigns = useQuery({
    queryKey: ["campaigns", statusFilter, campaignPage],
    queryFn: () =>
      api.campaignsPage({
        page: campaignPage,
        size: CAMPAIGN_PAGE_SIZE,
        status: statusFilter === "ALL" ? undefined : statusFilter,
      }),
  });
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
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-10">
      <header className="mb-10 flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <p className="flex items-center gap-2 text-[10px] font-semibold uppercase tracking-[0.34em] text-gold/75">
            <Sparkles className="h-3.5 w-3.5" aria-hidden="true" />
            Mesa de aventuras
          </p>
          <h1 className="title-display mt-2 text-3xl font-bold text-parchment text-glow-gold sm:text-4xl">
            Sala de Aventuras
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-relaxed text-muted">
            Bienvenido, <span className="font-medium text-ink">{user?.displayName ?? "aventurero"}</span>.
            Elige una campaña para sentarte a la mesa o forja un nuevo héroe.
          </p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
          <div className="flex items-center gap-2 self-start rounded-full border border-parchment/10 bg-black/30 py-1 pl-1 pr-3">
            <Avatar name={user?.displayName ?? user?.username ?? "Aventurero"} size="xs" />
            <div className="leading-tight">
              <p className="text-xs font-medium text-parchment">{user?.username ?? "invitado"}</p>
              <p className="text-[10px] uppercase tracking-wider text-muted">
                {user?.role === "ADMIN" ? "Administrador" : "Aventurero"}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-1">
            {user?.role === "ADMIN" && (
              <Button variant="ghost" size="sm" onClick={() => navigate("/admin")}>
                <Shield className="h-4 w-4" /> Administración
              </Button>
            )}
            <Button variant="ghost" size="sm" onClick={logout}>
              <LogOut className="h-4 w-4" /> Salir
            </Button>
          </div>
        </div>
      </header>

      {/* Featured campaign: the centrepiece of the table */}
      {statusFilter === "ALL" && campaigns.data && campaigns.data.content.length > 0 && (
        (() => {
          const featured = campaigns.data.content.find((c) => c.status === "ACTIVE") ?? campaigns.data.content[0];
          return (
            <section
              aria-labelledby="featured-campaign-title"
              className="group relative mb-12 overflow-hidden rounded-xl border border-gold/25 shadow-panel"
            >
              <img
                src={featured.image ?? "/images/campaigns/valdros.png"}
                alt=""
                className="absolute inset-0 h-full w-full object-cover opacity-60 transition-transform duration-700 group-hover:scale-[1.02]"
              />
              <div className="absolute inset-0 bg-gradient-to-r from-[#0b0907] via-[#0b0907]/85 to-[#0b0907]/10" />
              <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-[#0b0907]/80 to-transparent" />
              <div className="relative flex min-h-[18rem] max-w-3xl flex-col justify-end p-6 sm:p-8 md:p-10">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="flex items-center gap-2 text-[11px] uppercase tracking-[0.32em] text-gold-soft">
                    <Flame className="h-4 w-4 animate-flame" aria-hidden="true" />
                    Campaña destacada
                  </p>
                  <Badge tone={campaignStatusTone(featured.status)}>
                    {campaignStatusLabel(featured.status)}
                  </Badge>
                </div>
                <h2
                  id="featured-campaign-title"
                  className="title-display mt-3 text-3xl font-bold text-[#f5eee1] text-glow-gold md:text-4xl"
                >
                  {featured.name}
                </h2>
                <p className="mt-3 line-clamp-2 max-w-2xl text-sm leading-relaxed text-[#c8bda7]">
                  {featured.setting ?? "Una historia espera a que alguien ocupe su asiento."}
                </p>
                <div className="mt-6 flex flex-wrap items-center justify-between gap-4 border-t border-gold/15 pt-4">
                  <div className="flex items-center gap-3">
                    {featured.members.length > 0 && (
                      <span className="flex -space-x-2">
                        {featured.members.slice(0, 4).map((member) => (
                          <Avatar
                            key={member.id}
                            name={member.displayName}
                            size="xs"
                            className="ring-2 ring-[#0b0907]"
                          />
                        ))}
                      </span>
                    )}
                    <span className="inline-flex items-center gap-1.5 text-xs text-[#b3a793]">
                      <Users className="h-3.5 w-3.5" aria-hidden="true" />
                      {featured.members.length} aventurero{featured.members.length === 1 ? "" : "s"}
                      <span aria-hidden="true">·</span>
                      {campaignPhaseLabel(featured.phase)}
                    </span>
                  </div>
                  <Button variant="primary" onClick={() => navigate(`/campaign/${featured.id}/lobby`)}>
                    <Swords className="h-4 w-4" /> Entrar en la campaña
                  </Button>
                </div>
              </div>
            </section>
          );
        })()
      )}

      <section className="mb-12" aria-labelledby="campaigns-title">
        <SectionHeading
          icon={Scroll}
          eyebrow="Elige tu mesa"
          title="Campañas"
          description="Retoma una historia en curso o abre una nueva campaña para tu grupo."
          action={
            <Button
              variant="primary"
              size="sm"
              aria-expanded={showCreate}
              aria-controls="create-campaign"
              onClick={() => setShowCreate((value) => !value)}
            >
              <Plus className="h-4 w-4" /> Crear campaña
            </Button>
          }
          titleId="campaigns-title"
        />

        <div className="mb-4 flex flex-col gap-3 rounded-lg border border-parchment/10 bg-black/20 p-2 sm:flex-row sm:items-center">
          <div className="flex flex-wrap items-center gap-1" role="group" aria-label="Filtrar campañas por estado">
            {CAMPAIGN_FILTERS.map((filter) => {
              const Icon = filter.icon;
              return (
                <button
                  key={filter.value}
                  type="button"
                  aria-pressed={statusFilter === filter.value}
                  onClick={() => {
                    setStatusFilter(filter.value);
                    setCampaignPage(0);
                  }}
                  className={cn(
                    "inline-flex h-8 items-center gap-1.5 rounded-md border px-2.5 text-xs font-medium transition",
                    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold/60 focus-visible:ring-offset-2 focus-visible:ring-offset-[#0b0907]",
                    statusFilter === filter.value
                      ? "border-gold/50 bg-gold/15 text-gold-soft shadow-[inset_0_1px_0_rgba(255,255,255,0.08)]"
                      : "border-transparent text-muted hover:bg-white/5 hover:text-ink",
                  )}
                >
                  <Icon className="h-3.5 w-3.5" aria-hidden="true" />
                  {filter.label}
                </button>
              );
            })}
          </div>
          {campaigns.data && (
            <span className="text-xs text-muted sm:ml-auto sm:whitespace-nowrap">
              {campaigns.data.totalElements} campaña{campaigns.data.totalElements === 1 ? "" : "s"} en total
            </span>
          )}
        </div>

        {showCreate && (
          <form
            id="create-campaign"
            ref={createPanelRef}
            onSubmit={(event) => {
              event.preventDefault();
              createCampaign.mutate();
            }}
            className="panel mb-5 p-4"
          >
            <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto_auto] sm:items-end">
              <label className="block">
                <span className="mb-1.5 block text-[10px] font-semibold uppercase tracking-[0.22em] text-muted">
                  Nombre de la campaña
                </span>
                <input
                  value={newName}
                  onChange={(event) => setNewName(event.target.value)}
                  placeholder="La Cripta de Valdros"
                  className={FIELD_CLASS}
                />
              </label>
              <label className="block">
                <span className="mb-1.5 block text-[10px] font-semibold uppercase tracking-[0.22em] text-muted">
                  Edición
                </span>
                <select
                  value={newEdition}
                  onChange={(event) => setNewEdition(event.target.value)}
                  className={FIELD_CLASS}
                >
                  <option value="dnd-5e-2014">D&D 5e (2014)</option>
                  <option value="dnd-5e-2024">D&D 5e (2024)</option>
                </select>
              </label>
              <Button type="submit" variant="primary" loading={createCampaign.isPending}>
                Crear y entrar
              </Button>
            </div>
            {createCampaign.isError && (
              <p role="alert" className="mt-3 text-xs text-crimson-soft">
                No se pudo crear la campaña. Inténtalo de nuevo.
              </p>
            )}
          </form>
        )}

        {campaigns.isLoading ? (
          <CampaignGridSkeleton />
        ) : campaigns.isError ? (
          <EmptyState
            icon={AlertTriangle}
            title="No se pudo abrir el archivo de campañas"
            description="Comprueba la conexión con la taberna e inténtalo de nuevo."
            action={
              <Button variant="secondary" size="sm" loading={campaigns.isFetching} onClick={() => campaigns.refetch()}>
                <RefreshCw className="h-4 w-4" /> Reintentar
              </Button>
            }
          />
        ) : campaigns.data && campaigns.data.content.length > 0 ? (
          <>
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {campaigns.data.content.map((campaign) => (
                <CampaignCard key={campaign.id} campaign={campaign} />
              ))}
            </div>
            {campaigns.data.totalPages > 1 && (
              <nav
                aria-label="Paginación de campañas"
                className="mt-6 flex items-center justify-between gap-3 rounded-lg border border-parchment/10 bg-black/20 px-3 py-2"
              >
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={campaigns.data.first}
                  onClick={() => setCampaignPage((page) => Math.max(0, page - 1))}
                >
                  <ChevronLeft className="h-4 w-4" /> Anterior
                </Button>
                <span className="text-xs text-muted">
                  Página {campaigns.data.page + 1} de {campaigns.data.totalPages}
                </span>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={campaigns.data.last}
                  onClick={() => setCampaignPage((page) => page + 1)}
                >
                  Siguiente <ChevronRight className="h-4 w-4" />
                </Button>
              </nav>
            )}
          </>
        ) : (
          <EmptyState
            icon={statusFilter === "ALL" ? Scroll : DoorOpen}
            title={statusFilter === "ALL" ? "La mesa está vacía" : "Sin campañas en este estado"}
            description={
              statusFilter === "ALL"
                ? "Aún no hay campañas. Crea la primera y desciende a la Cripta de Valdros."
                : "Prueba otro filtro o vuelve a ver todas las campañas disponibles."
            }
            action={
              statusFilter === "ALL" ? (
                <Button variant="primary" size="sm" onClick={() => setShowCreate(true)}>
                  <Plus className="h-4 w-4" /> Crear la primera campaña
                </Button>
              ) : (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    setStatusFilter("ALL");
                    setCampaignPage(0);
                  }}
                >
                  <LayoutGrid className="h-4 w-4" /> Ver todas
                </Button>
              )
            }
          />
        )}
      </section>

      <section aria-labelledby="heroes-title">
        <SectionHeading
          icon={Swords}
          eyebrow="Tu cuaderno de personajes"
          title="Tus héroes"
          description="Los personajes se construyen con las reglas del SRD (raza, clase, trasfondo, puntos)."
          action={
            <Button variant="primary" size="sm" onClick={() => navigate("/characters/new")}>
              <Plus className="h-4 w-4" /> Crear personaje
            </Button>
          }
          titleId="heroes-title"
        />

        {characters.isLoading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4" aria-busy="true">
            <span className="sr-only">Cargando héroes…</span>
            {[0, 1, 2, 3].map((index) => (
              <HeroCardSkeleton key={index} />
            ))}
          </div>
        ) : characters.isError ? (
          <EmptyState
            icon={AlertTriangle}
            title="No se pudieron recuperar tus héroes"
            description="La taberna no responde. Comprueba la conexión e inténtalo de nuevo."
            action={
              <Button variant="secondary" size="sm" loading={characters.isFetching} onClick={() => characters.refetch()}>
                <RefreshCw className="h-4 w-4" /> Reintentar
              </Button>
            }
          />
        ) : (characters.data ?? []).length === 0 ? (
          <EmptyState
            icon={UserRound}
            title="Aún no has forjado ningún héroe"
            description="Elige especie, clase y trasfondo, reparte las características y toma asiento en tu próxima aventura."
            action={
              <Button variant="primary" size="sm" onClick={() => navigate("/characters/new")}>
                <Plus className="h-4 w-4" /> Crear mi primer héroe
              </Button>
            }
          />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {(characters.data ?? []).map((character) => (
              <HeroCard
                key={character.id}
                character={character}
                onDelete={
                  character.predefined ? undefined : () => deleteCharacter.mutate(character.id)
                }
                onUploadPortrait={
                  character.predefined
                    ? undefined
                    : (file) => uploadPortrait.mutate({ id: character.id, file })
                }
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function SectionHeading({
  icon: Icon,
  eyebrow,
  title,
  description,
  action,
  titleId,
}: {
  icon: LucideIcon;
  eyebrow: string;
  title: string;
  description?: string;
  action?: ReactNode;
  titleId?: string;
}) {
  return (
    <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        <p className="flex items-center gap-2 text-[10px] font-semibold uppercase tracking-[0.3em] text-gold/75">
          <Icon className="h-3.5 w-3.5" aria-hidden="true" />
          {eyebrow}
        </p>
        <h2 id={titleId} className="title-display mt-1 text-2xl text-parchment">
          {title}
        </h2>
        {description && <p className="mt-1 max-w-2xl text-sm leading-relaxed text-muted">{description}</p>}
      </div>
      {action}
    </div>
  );
}

function CampaignGridSkeleton() {
  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3" aria-busy="true">
      <span className="sr-only">Cargando campañas…</span>
      {[0, 1, 2, 3, 4, 5].map((index) => (
        <div
          key={index}
          className="overflow-hidden rounded-xl border border-parchment/10 bg-surface/60 shadow-panel"
        >
          <Skeleton className="h-40 w-full rounded-none" />
          <div className="space-y-3 p-4">
            <Skeleton className="h-3.5 w-4/5" />
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-3 w-2/3" />
            <div className="flex items-center justify-between gap-3 border-t border-parchment/10 pt-3">
              <Skeleton className="h-6 w-24" />
              <Skeleton className="h-3 w-12" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function HeroCardSkeleton() {
  return (
    <div className="panel overflow-hidden p-0">
      <div className="flex h-32 items-end justify-center pb-2">
        <Skeleton className="h-20 w-20 rounded-full" />
      </div>
      <div className="space-y-3 p-4">
        <Skeleton className="mx-auto h-4 w-2/3" />
        <Skeleton className="mx-auto h-3 w-1/2" />
        <Skeleton className="h-2 w-full rounded-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-12 w-full" />
      </div>
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
    <article className="panel group relative flex flex-col overflow-hidden p-0">
      {(onDelete || onUploadPortrait) && (
        <div className="absolute right-2 top-2 z-10 flex gap-1 transition sm:opacity-0 sm:group-hover:opacity-100 sm:group-focus-within:opacity-100">
          {onUploadPortrait && (
            <>
              <button
                type="button"
                onClick={() => fileRef.current?.click()}
                title="Cambiar retrato"
                aria-label={`Cambiar el retrato de ${character.name}`}
                className="rounded-md border border-[#3a2a1a] bg-[#100d0a]/85 p-1.5 text-muted transition hover:border-[#c9a227]/50 hover:text-[#e0b84a] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold/60"
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
              type="button"
              onClick={onDelete}
              title="Borrar personaje"
              aria-label={`Borrar a ${character.name}`}
              className="rounded-md border border-[#3a2a1a] bg-[#100d0a]/85 p-1.5 text-muted transition hover:border-[#d0566a]/50 hover:text-[#d0566a] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-crimson/60"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      )}

      <div
        className="relative h-32 w-full"
        style={{
          background: `radial-gradient(140px 90px at 50% 0%, ${character.color ?? "#C9A227"}55, transparent), linear-gradient(180deg, #211A14, #17130F)`,
        }}
      >
        <div className="absolute left-3 top-3 flex flex-wrap gap-1.5">
          <Badge tone={character.predefined ? "gold" : "emerald"}>
            {character.predefined ? "Pregenerado" : "Héroe"}
          </Badge>
          <Badge tone="muted">Nv {character.level}</Badge>
        </div>
        <div className="absolute inset-x-0 bottom-2 flex justify-center">
          <Avatar
            name={character.name}
            src={character.portrait}
            color={character.color}
            size="xl"
            className="shadow-panel"
          />
        </div>
      </div>

      <div className="flex flex-1 flex-col gap-3 px-4 pb-4 pt-2">
        <div className="text-center">
          <h3 className="title-display text-lg font-semibold text-parchment">{character.name}</h3>
          <p className="text-[11px] uppercase tracking-wider text-muted">
            {character.race} · {character.cls}
          </p>
        </div>

        <HealthBar value={character.hp} max={character.maxHp} />

        <div className="grid grid-cols-2 gap-2">
          <div className="stat-chip flex items-center justify-between px-2.5 py-1.5 text-[11px] text-muted">
            <span>CA</span>
            <span className="font-semibold text-ink">{character.ac}</span>
          </div>
          <div className="stat-chip flex items-center justify-between px-2.5 py-1.5 text-[11px] text-muted">
            <span>VEL</span>
            <span className="font-semibold text-ink">{character.speed}</span>
          </div>
        </div>

        <div className="divider" />

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
    </article>
  );
}
