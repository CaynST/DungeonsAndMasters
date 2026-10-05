import { Dices, Footprints, MessageSquare, MoveRight, Store, Swords } from "lucide-react";
import type { Attack, PlayerCharacter } from "@/types/game";
import { Button } from "@/components/ui/Button";
import { formatModifier } from "@/lib/utils";

interface Props {
  character?: PlayerCharacter;
  canAct: boolean;
  isMyTurn: boolean;
  selectedTargetName?: string;
  selectedAttack?: Attack;
  onSelectAttack: (attack: Attack) => void;
  onAttack: () => void;
  onEndTurn: () => void;
  onDialogue: () => void;
  onQuickRoll: (expression: string) => void;
  onDiceRequest: () => void;
  onOpenShop: () => void;
  mode: "move" | "attack";
  onToggleMode: (mode: "move" | "attack") => void;
}

export function ActionBar({
  character,
  canAct,
  isMyTurn,
  selectedTargetName,
  selectedAttack,
  onSelectAttack,
  onAttack,
  onEndTurn,
  onDialogue,
  onQuickRoll,
  onDiceRequest,
  onOpenShop,
  mode,
  onToggleMode,
}: Props) {
  return (
    <div className="panel flex flex-col gap-3 p-3 lg:flex-row lg:items-center lg:justify-between">
      <div className="flex flex-wrap items-center gap-2">
        <span className="mr-1 text-[11px] uppercase tracking-wider text-muted">
          {character?.name ?? "Héroe"}
        </span>

        <Button
          variant={mode === "move" ? "primary" : "secondary"}
          size="sm"
          disabled={!canAct}
          onClick={() => onToggleMode("move")}
        >
          <Footprints className="h-4 w-4" /> Mover
        </Button>

        <Button
          variant={mode === "attack" ? "danger" : "secondary"}
          size="sm"
          disabled={!canAct}
          onClick={() => onToggleMode("attack")}
        >
          <Swords className="h-4 w-4" /> Atacar
        </Button>

        <div className="mx-1 h-6 w-px bg-parchment/10" />

        {(character?.attacks ?? []).map((attack) => (
          <button
            key={attack.name}
            onClick={() => onSelectAttack(attack)}
            disabled={!canAct}
            className={`rounded-md border px-2.5 py-1.5 text-xs transition disabled:opacity-40 ${
              selectedAttack?.name === attack.name
                ? "border-crimson/60 bg-crimson/15 text-crimson-soft"
                : "border-parchment/10 bg-surface/60 text-muted hover:border-crimson/40"
            }`}
          >
            ⚔ {attack.name} <span className="opacity-70">{formatModifier(attack.attackBonus)}</span>
          </button>
        ))}

        <Button variant="arcane" size="sm" disabled={!canAct || !selectedTargetName} onClick={onAttack}>
          Confirmar ataque
          {selectedTargetName && <span className="text-white/80">→ {selectedTargetName}</span>}
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Button variant="ghost" size="sm" onClick={onDiceRequest}>
          <Dices className="h-4 w-4" /> Dados
        </Button>
        {[("1d20"), ("2d6+3"), ("1d8+3")].map((expression) => (
          <button
            key={expression}
            onClick={() => onQuickRoll(expression)}
            className="rounded-md border border-parchment/10 bg-black/30 px-2 py-1 text-[11px] text-muted transition hover:border-gold/40 hover:text-gold-soft"
          >
            {expression}
          </button>
        ))}
        <Button variant="ghost" size="sm" onClick={onDialogue}>
          <MessageSquare className="h-4 w-4" /> Hablar
        </Button>
        <Button variant="ghost" size="sm" onClick={onOpenShop}>
          <Store className="h-4 w-4" /> Tienda
        </Button>
        <Button variant="secondary" size="sm" disabled={!isMyTurn} onClick={onEndTurn}>
          <MoveRight className="h-4 w-4" /> Terminar turno
        </Button>
      </div>
    </div>
  );
}
