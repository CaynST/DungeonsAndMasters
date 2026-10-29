import { useState } from "react";
import { Dices, Footprints, Lock, MessageSquare, MoveRight, Store, Swords } from "lucide-react";
import type { Attack, PlayerCharacter } from "@/types/game";
import { Button } from "@/components/ui/Button";
import { formatModifier } from "@/lib/utils";

interface Props {
  character?: PlayerCharacter;
  canAct: boolean;
  isMyTurn: boolean;
  busy?: boolean;
  checkPending?: boolean;
  downed?: boolean;
  movementUsed?: number;
  movementMax?: number;
  actionUsed?: boolean;
  selectedTargetName?: string;
  selectedAttack?: Attack;
  onSelectAttack: (attack: Attack) => void;
  onAttack: () => void;
  onEndTurn: () => void;
  onDialogue: () => void;
  onQuickRoll: (expression: string) => void;
  onDiceRequest: () => void;
  onOpenShop: () => void;
  shopOpen?: boolean;
  mode: "move" | "attack";
  onToggleMode: (mode: "move" | "attack") => void;
}

export function ActionBar({
  character,
  canAct,
  isMyTurn,
  busy,
  checkPending,
  downed,
  movementUsed,
  movementMax,
  actionUsed,
  selectedTargetName,
  selectedAttack,
  onSelectAttack,
  onAttack,
  onEndTurn,
  onDialogue,
  onQuickRoll,
  onDiceRequest,
  onOpenShop,
  shopOpen,
  mode,
  onToggleMode,
}: Props) {
  // While an action is in flight, a check is unresolved or the hero is downed, everything is locked.
  const locked = Boolean(busy) || Boolean(checkPending) || Boolean(downed);
  const canActNow = canAct && !locked;
  const [diceOpen, setDiceOpen] = useState(false);

  return (
    <div className="panel flex flex-col gap-2 p-3">
      {downed ? (
        <div className="flex items-center gap-2 rounded-md border border-[#d0566a]/40 bg-[#d0566a]/10 px-3 py-1.5 text-xs text-[#e8b7c0]">
          <Lock className="h-3.5 w-3.5" />
          Estás moribundo. Solo puedes tirar una salvación de muerte.
        </div>
      ) : checkPending ? (
        <div className="flex items-center gap-2 rounded-md border border-gold/40 bg-gold/10 px-3 py-1.5 text-xs text-gold-soft">
          <Lock className="h-3.5 w-3.5" />
          El Dungeon Master os ha pedido una prueba. Resolvedla antes de actuar.
        </div>
      ) : null}

      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-wrap items-center gap-2">
          <span className="mr-1 text-[11px] uppercase tracking-wider text-muted">
            {character?.name ?? "Héroe"}
          </span>

          <span className="stat-chip mr-1 inline-flex items-center gap-1.5 px-2 py-1 text-[11px]">
            <Footprints className="h-3 w-3 text-[#9b7cff]" />
            <span className="text-muted">Mov</span>
            <span className="font-semibold text-ink">
              {Math.max(0, (movementMax ?? 0) - (movementUsed ?? 0))}
            </span>
            <span className="text-muted">/{movementMax ?? 0}</span>
          </span>
          <span className="stat-chip mr-1 inline-flex items-center gap-1.5 px-2 py-1 text-[11px]">
            <Swords className="h-3 w-3 text-[#d0566a]" />
            <span className="text-muted">Acción</span>
            <span className={actionUsed ? "font-semibold text-[#d0566a]" : "font-semibold text-[#6fd3a0]"}>
              {actionUsed ? "usada" : "libre"}
            </span>
          </span>

          <Button
            variant={mode === "move" ? "primary" : "secondary"}
            size="sm"
            disabled={!canActNow}
            onClick={() => onToggleMode("move")}
          >
            <Footprints className="h-4 w-4" /> Mover
          </Button>

          <Button
            variant={mode === "attack" ? "danger" : "secondary"}
            size="sm"
            disabled={!canActNow}
            onClick={() => onToggleMode("attack")}
          >
            <Swords className="h-4 w-4" /> Atacar
          </Button>

          <div className="mx-1 h-6 w-px bg-parchment/10" />

          {(character?.attacks ?? []).map((attack) => (
            <button
              key={attack.name}
              onClick={() => onSelectAttack(attack)}
              disabled={!canActNow}
              className={`rounded-md border px-2.5 py-1.5 text-xs transition disabled:opacity-40 ${
                selectedAttack?.name === attack.name
                  ? "border-crimson/60 bg-crimson/15 text-crimson-soft"
                  : "border-parchment/10 bg-surface/60 text-muted hover:border-crimson/40"
              }`}
            >
              ⚔ {attack.name} <span className="opacity-70">{formatModifier(attack.attackBonus)}</span>
            </button>
          ))}

          <Button
            variant="danger"
            size="sm"
            disabled={!canActNow || !selectedTargetName}
            onClick={onAttack}
          >
            Confirmar ataque
            {selectedTargetName && <span className="text-white/80">→ {selectedTargetName}</span>}
          </Button>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <div className="relative">
            <Button variant="ghost" size="sm" disabled={locked} onClick={() => setDiceOpen((value) => !value)}>
              <Dices className="h-4 w-4" /> Dados
            </Button>
            {diceOpen && (
              <div className="absolute bottom-full left-0 z-30 mb-2 grid grid-cols-3 gap-1 rounded-lg border border-[#3a2a1a] bg-[#1a130d] p-2 shadow-lg">
                {[4, 6, 8, 10, 12, 20].map((sides) => (
                  <button
                    key={sides}
                    onClick={() => {
                      onQuickRoll(`1d${sides}`);
                      setDiceOpen(false);
                    }}
                    className="title-display h-10 w-12 rounded-md border border-[#3a2a1a] bg-[#100d0a] text-sm text-[#e8e1d5] transition hover:border-[#c9a227]/60 hover:text-[#e0b84a]"
                  >
                    d{sides}
                  </button>
                ))}
              </div>
            )}
          </div>
          <Button variant="ghost" size="sm" disabled={locked} onClick={onDialogue}>
            <MessageSquare className="h-4 w-4" /> Hablar
          </Button>
          <Button variant="ghost" size="sm" disabled={!shopOpen || locked} onClick={onOpenShop}>
            <Store className="h-4 w-4" /> Tienda
          </Button>
          <Button variant="secondary" size="sm" disabled={!isMyTurn || locked} onClick={onEndTurn}>
            <MoveRight className="h-4 w-4" /> Terminar turno
          </Button>
        </div>
      </div>
    </div>
  );
}
