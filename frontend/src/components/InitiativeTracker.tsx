import { motion } from "framer-motion";
import { Swords } from "lucide-react";
import type { CombatState, GameEntity } from "@/types/game";
import { cn, hpColor, hpPercent } from "@/lib/utils";

interface Props {
  combat: CombatState;
  entities: GameEntity[];
}

export function InitiativeTracker({ combat, entities }: Props) {
  const current = combat.participants[combat.currentTurnIndex];

  return (
    <div className="panel p-4">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="title-display flex items-center gap-2 text-sm text-parchment">
          <Swords className="h-4 w-4 text-crimson-soft" /> Iniciativa
        </h3>
        <span className="rounded-full border border-gold/30 bg-gold/10 px-2.5 py-0.5 text-[11px] font-semibold text-gold-soft">
          Ronda {combat.round}
        </span>
      </div>

      <ol className="space-y-1.5">
        {combat.participants.map((participant, index) => {
          const entity = entities.find((item) => item.id === participant.entityId);
          const isCurrent = index === combat.currentTurnIndex;
          const dead = !participant.alive;
          return (
            <motion.li
              key={participant.entityId}
              layout
              className={cn(
                "flex items-center gap-2 rounded-lg border px-3 py-2 text-sm transition",
                isCurrent
                  ? "border-gold/60 bg-gold/10 shadow-glow"
                  : "border-parchment/10 bg-surface/40",
                dead && "opacity-40",
              )}
            >
              <span
                className={cn(
                  "flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold",
                  participant.kind === "ENEMY" ? "bg-crimson/30 text-crimson-soft" : "bg-emerald/25 text-emerald-soft",
                )}
              >
                {participant.initiative}
              </span>
              <div className="min-w-0 flex-1">
                <p className={cn("truncate", isCurrent && "font-semibold text-parchment")}>
                  {participant.name}
                  {dead && " ✕"}
                </p>
                {entity && entity.maxHp > 1 && (
                  <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-black/50">
                    <div
                      className="h-full rounded-full hp-bar"
                      style={{
                        width: `${hpPercent(entity.hp, entity.maxHp)}%`,
                        backgroundColor: hpColor(entity.hp, entity.maxHp),
                      }}
                    />
                  </div>
                )}
              </div>
              {isCurrent && <span className="h-2 w-2 animate-pulse rounded-full bg-gold" />}
            </motion.li>
          );
        })}
      </ol>

      {current && (
        <p className="mt-3 text-center text-xs text-muted">
          Turno de <span className="font-medium text-gold-soft">{current.name}</span>
        </p>
      )}
    </div>
  );
}
