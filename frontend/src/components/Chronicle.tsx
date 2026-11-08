import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, BookOpen, RefreshCw } from "lucide-react";
import { api } from "@/lib/api";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Skeleton } from "@/components/ui/Skeleton";
import type { GameEvent } from "@/types/game";

const TONE: Record<string, string> = {
  CAMPAIGN_STARTED: "#c9a227",
  SCENE_CHANGED: "#9b7cff",
  NARRATION: "#c9a227",
  COMBAT_STARTED: "#d0566a",
  COMBAT_ENDED: "#6fd3a0",
  ATTACK: "#e06a7a",
  ATTACK_MISS: "#8a7f6d",
  ATTACK_BLOCKED: "#e06a7a",
  DEATH: "#d0566a",
  HEAL: "#6fd3a0",
  CHECK: "#c9a227",
  CHECK_REQUESTED: "#c9a227",
  DICE_ROLL: "#d8c39a",
  REWARD: "#c9a227",
  ADVENTURE_ENDED: "#c9a227",
  SHOP_OPENED: "#c9a227",
  PURCHASE: "#c9a227",
  SALE: "#c9a227",
  MOVE: "#8a7f6d",
  TURN_CHANGED: "#8a7f6d",
  NPC_TURN: "#8a7f6d",
  PLAYER_JOINED: "#6fd3a0",
  PLAYER_ACTION: "#c9a227",
  SURPRISED: "#d8c39a",
  SURPRISE_APPLIED: "#d8c39a",
};

const EVENT_LABEL: Record<string, string> = {
  CAMPAIGN_STARTED: "Campaña iniciada",
  SCENE_CHANGED: "Nueva escena",
  NARRATION: "Narración",
  COMBAT_STARTED: "Comienza el combate",
  COMBAT_ENDED: "Fin del combate",
  ATTACK: "Ataque",
  ATTACK_MISS: "Ataque fallido",
  ATTACK_BLOCKED: "Ataque bloqueado",
  DEATH: "Caída",
  HEAL: "Curación",
  CHECK: "Prueba",
  CHECK_REQUESTED: "Prueba solicitada",
  DICE_ROLL: "Tirada",
  REWARD: "Recompensa",
  ADVENTURE_ENDED: "Fin de la aventura",
  SHOP_OPENED: "Tienda abierta",
  PURCHASE: "Compra",
  SALE: "Venta",
  MOVE: "Movimiento",
  TURN_CHANGED: "Cambio de turno",
  NPC_TURN: "Turno de PNJ",
  PLAYER_JOINED: "Aventurero unido",
  PLAYER_ACTION: "Acción",
  SURPRISED: "Sorpresa",
  SURPRISE_APPLIED: "Emboscada",
};

const DAY_FORMAT = new Intl.DateTimeFormat("es-ES", {
  weekday: "long",
  day: "numeric",
  month: "long",
});

const TIME_FORMAT = new Intl.DateTimeFormat("es-ES", {
  hour: "2-digit",
  minute: "2-digit",
});

interface ChronicleDay {
  key: string;
  label: string;
  events: GameEvent[];
}

function eventLabel(type: string): string {
  return (
    EVENT_LABEL[type] ??
    type
      .toLowerCase()
      .replace(/_/g, " ")
      .replace(/^\p{Ll}/u, (letter) => letter.toUpperCase())
  );
}

function timeLabel(createdAt: string): string | null {
  const date = new Date(createdAt);
  return Number.isNaN(date.getTime()) ? null : TIME_FORMAT.format(date);
}

function groupByDay(events: GameEvent[]): ChronicleDay[] {
  const groups: ChronicleDay[] = [];
  for (const event of events) {
    const date = new Date(event.createdAt);
    const valid = !Number.isNaN(date.getTime());
    const key = valid
      ? `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
      : "sin-fecha";
    const label = valid ? DAY_FORMAT.format(date) : "Sin fecha";
    const current = groups[groups.length - 1];
    if (current?.key === key) {
      current.events.push(event);
    } else {
      groups.push({ key, label: label.charAt(0).toUpperCase() + label.slice(1), events: [event] });
    }
  }
  return groups;
}

/** Full chronicle of the campaign, so a latecomer can catch up on the story. */
export function Chronicle({ campaignId }: { campaignId: number }) {
  const events = useQuery({
    queryKey: ["chronicle", campaignId],
    queryFn: () => api.events(campaignId),
  });

  const entries = events.data ?? [];
  const days = useMemo(() => groupByDay(events.data ?? []), [events.data]);

  return (
    <div className="panel flex h-full min-h-0 flex-col overflow-hidden">
      <div className="flex items-center justify-between gap-3 border-b border-[#2b2118] bg-black/10 px-4 py-3">
        <div className="flex min-w-0 items-center gap-2.5">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-gold/25 bg-gold/10 text-gold">
            <BookOpen className="h-4 w-4" aria-hidden="true" />
          </span>
          <div className="min-w-0">
            <h2 className="title-display truncate text-sm font-semibold text-ink">Crónica de la aventura</h2>
            <p className="text-[10px] uppercase tracking-[0.2em] text-muted">Lo que ha quedado escrito</p>
          </div>
        </div>
        {!events.isLoading && !events.isError && (
          <Badge tone="muted">
            {entries.length} entrada{entries.length === 1 ? "" : "s"}
          </Badge>
        )}
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-4">
        {events.isLoading ? (
          <div className="space-y-3" aria-busy="true">
            <span className="sr-only">Recuperando la crónica…</span>
            {[0, 1, 2, 3].map((index) => (
              <div key={index} className="rounded-lg border border-parchment/5 bg-black/10 p-3">
                <Skeleton className="h-2.5 w-24" />
                <Skeleton className="mt-2 h-3 w-full" />
                <Skeleton className="mt-1.5 h-3 w-4/5" />
              </div>
            ))}
          </div>
        ) : events.isError ? (
          <EmptyState
            icon={AlertTriangle}
            title="No se pudo recuperar la crónica"
            description="El archivo de la aventura no responde. Inténtalo de nuevo."
            compact
            action={
              <Button
                variant="secondary"
                size="sm"
                loading={events.isFetching}
                onClick={() => events.refetch()}
              >
                <RefreshCw className="h-4 w-4" /> Reintentar
              </Button>
            }
          />
        ) : entries.length === 0 ? (
          <EmptyState
            icon={BookOpen}
            title="La crónica está en blanco"
            description="La historia empezará a escribirse con la primera escena de la aventura."
            compact
          />
        ) : (
          <div className="space-y-5">
            {days.map((day) => (
              <section key={day.key} aria-label={day.label}>
                <div className="mb-3 flex items-center gap-3">
                  <span className="h-px flex-1 bg-gold/15" aria-hidden="true" />
                  <span className="text-[10px] font-semibold uppercase tracking-[0.2em] text-gold-soft">
                    {day.label}
                  </span>
                  <span className="h-px flex-1 bg-gold/15" aria-hidden="true" />
                </div>
                <ol className="relative space-y-2 border-l border-[#2b2118] pl-4">
                  {day.events.map((event) => {
                    const tone = TONE[event.type] ?? "#8a7f6d";
                    const time = timeLabel(event.createdAt);
                    return (
                      <li key={event.id} className="relative">
                        <span
                          className="absolute -left-[21px] top-3 h-2.5 w-2.5 rounded-full border-2 border-[#15110d]"
                          style={{ backgroundColor: tone, boxShadow: `0 0 8px ${tone}66` }}
                          aria-hidden="true"
                        />
                        <div className="rounded-lg border border-parchment/5 bg-black/15 px-3 py-2.5 transition hover:border-gold/20 hover:bg-black/25">
                          <div className="flex items-center justify-between gap-2">
                            <span className="text-[10px] font-medium uppercase tracking-[0.16em] text-muted">
                              {eventLabel(event.type)}
                            </span>
                            {time && (
                              <time
                                dateTime={event.createdAt}
                                className="shrink-0 text-[10px] tabular-nums text-muted"
                              >
                                {time}
                              </time>
                            )}
                          </div>
                          <p className="mt-1 text-sm leading-relaxed text-ink">
                            {event.actor && (
                              <span className="font-medium text-[#d8c9a8]">{event.actor}: </span>
                            )}
                            {event.message}
                          </p>
                        </div>
                      </li>
                    );
                  })}
                </ol>
              </section>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
