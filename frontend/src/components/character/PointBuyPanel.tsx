import { Minus, Plus } from "lucide-react";
import type { AbilityScores } from "@/types/game";
import { cn, formatModifier } from "@/lib/utils";
import { POINT_BUY_COST, pointsRemaining, abilityLabel } from "@/pages/CharacterCreation";

const ABILITIES: (keyof AbilityScores)[] = [
  "strength",
  "dexterity",
  "constitution",
  "intelligence",
  "wisdom",
  "charisma",
];

const MIN = 8;
const MAX = 15;

interface Props {
  scores: AbilityScores;
  onChange: (scores: AbilityScores) => void;
  primaryAbility?: keyof AbilityScores;
}

export function PointBuyPanel({ scores, onChange, primaryAbility }: Props) {
  const remaining = pointsRemaining(scores);

  const adjust = (ability: keyof AbilityScores, delta: number) => {
    const next = scores[ability] + delta;
    if (next < MIN || next > MAX) return;
    const costDelta = (POINT_BUY_COST[next] ?? 0) - (POINT_BUY_COST[scores[ability]] ?? 0);
    if (costDelta > remaining) return;
    onChange({ ...scores, [ability]: next });
  };

  return (
    <div>
      <div className="mb-4 flex items-center justify-between rounded-lg border border-gold/20 bg-gold/5 px-4 py-3">
        <span className="text-sm text-muted">Puntos restantes</span>
        <span className={cn("title-display text-2xl", remaining === 0 ? "text-emerald-soft" : "text-gold")}>
          {remaining}
        </span>
      </div>

      <div className="space-y-2">
        {ABILITIES.map((ability) => {
          const score = scores[ability];
          const isPrimary = primaryAbility === ability;
          return (
            <div
              key={ability}
              className={cn(
                "flex items-center justify-between rounded-lg border px-4 py-2.5",
                isPrimary ? "border-gold/40 bg-gold/5" : "border-parchment/10 bg-surface/50",
              )}
            >
              <div className="flex items-center gap-2">
                <span className="w-28 text-sm text-ink">{abilityLabel(ability)}</span>
                {isPrimary && <span className="text-[10px] uppercase tracking-wider text-gold-soft">principal</span>}
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => adjust(ability, -1)}
                  disabled={score <= MIN}
                  className="flex h-7 w-7 items-center justify-center rounded-md border border-parchment/15 text-muted transition hover:border-crimson/40 hover:text-crimson-soft disabled:opacity-30"
                >
                  <Minus className="h-3.5 w-3.5" />
                </button>
                <div className="w-16 text-center">
                  <span className="title-display text-xl text-ink">{score}</span>
                  <span className="ml-2 text-xs text-gold-soft">{formatModifier(Math.floor((score - 10) / 2))}</span>
                </div>
                <button
                  onClick={() => adjust(ability, 1)}
                  disabled={score >= MAX}
                  className="flex h-7 w-7 items-center justify-center rounded-md border border-parchment/15 text-muted transition hover:border-emerald/40 hover:text-emerald-soft disabled:opacity-30"
                >
                  <Plus className="h-3.5 w-3.5" />
                </button>
                <span className="w-16 text-right text-[11px] text-muted">
                  {POINT_BUY_COST[score] ?? 0} pts
                </span>
              </div>
            </div>
          );
        })}
      </div>

      <p className="mt-3 text-[11px] text-muted">
        Rango permitido: 8 a 15. Subir de 13→14 y 14→15 cuesta 2 puntos en lugar de 1.
      </p>
    </div>
  );
}
