import { useState } from "react";
import {
  ArrowUpFromLine,
  BookOpen,
  Dices,
  EyeOff,
  Flame,
  Footprints,
  Hand,
  HandHelping,
  HeartPulse,
  Lock,
  MessageSquare,
  Moon,
  MoveRight,
  Music,
  Package,
  Search,
  Shield,
  Sparkles,
  Store,
  Swords,
  Timer,
  Undo2,
  Wind,
} from "lucide-react";
import type { Attack, PlayerCharacter, Spell } from "@/types/game";
import { Button } from "@/components/ui/Button";
import { DICE_SIDES } from "@/lib/dice";
import { formatModifier } from "@/lib/utils";

interface Props {
  character?: PlayerCharacter;
  canAct: boolean;
  isMyTurn: boolean;
  busy?: boolean;
  checkPending?: boolean;
  downed?: boolean;
  incapacitated?: boolean;
  prone?: boolean;
  movementUsed?: number;
  movementMax?: number;
  actionUsed?: boolean;
  bonusUsed?: boolean;
  classId?: string | null;
  secondWindUsed?: boolean;
  raging?: boolean;
  ragesUsed?: number;
  bardicInspirationUses?: number;
  inspirationDie?: number;
  inspirationPending?: boolean;
  spells?: Spell[];
  spellSlotsMax?: number;
  spellSlotsUsed?: number;
  concentrationName?: string | null;
  concentrationRounds?: number;
  selectedTargetName?: string;
  selectedAllyName?: string;
  hidden?: boolean;
  readied?: boolean;
  objectInteractionUsed?: boolean;
  inCombat?: boolean;
  hitDiceRemaining?: number;
  hitDiceMax?: number;
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
  onCombatAction: (type: "DASH" | "DODGE" | "DISENGAGE") => void;
  onStandUp: () => void;
  onSecondWind: () => void;
  onRage: () => void;
  onBardicInspiration: () => void;
  onUseInspiration: () => void;
  onCastSpell: (spellId: string) => void;
  onHelp: () => void;
  onHide: () => void;
  onSearch: (skill: "perception" | "investigation") => void;
  onReady: () => void;
  onUseObject: (item: string) => void;
  onInteract: () => void;
  onShortRest: () => void;
  onLongRest: () => void;
}

export function ActionBar({
  character,
  canAct,
  isMyTurn,
  busy,
  checkPending,
  downed,
  incapacitated,
  prone,
  movementUsed,
  movementMax,
  actionUsed,
  bonusUsed,
  classId,
  secondWindUsed,
  raging,
  ragesUsed,
  bardicInspirationUses,
  inspirationDie,
  inspirationPending,
  spells = [],
  spellSlotsMax = 0,
  spellSlotsUsed = 0,
  concentrationName,
  concentrationRounds,
  selectedTargetName,
  selectedAllyName,
  hidden,
  readied,
  objectInteractionUsed,
  inCombat,
  hitDiceRemaining = 1,
  hitDiceMax = 1,
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
  onCombatAction,
  onStandUp,
  onSecondWind,
  onRage,
  onBardicInspiration,
  onUseInspiration,
  onCastSpell,
  onHelp,
  onHide,
  onSearch,
  onReady,
  onUseObject,
  onInteract,
  onShortRest,
  onLongRest,
}: Props) {
  // While an action is in flight, a check is unresolved, the hero is downed or incapacitated,
  // everything is locked.
  const locked = Boolean(busy) || Boolean(checkPending) || Boolean(downed) || Boolean(incapacitated);
  const canActNow = canAct && !locked;
  const [diceOpen, setDiceOpen] = useState(false);
  const [spellOpen, setSpellOpen] = useState(false);
  const [actionsOpen, setActionsOpen] = useState(false);
  const [restOpen, setRestOpen] = useState(false);
  // Potions keep their own USE_ITEM flow; Use object lists every other item.
  const usableItems = (character?.inventory ?? []).filter(
    (item) => !/poción|pocion|elixir|vial/i.test(item.name),
  );
  const runAction = (callback: () => void) => {
    setActionsOpen(false);
    callback();
  };
  const runRest = (callback: () => void) => {
    setRestOpen(false);
    callback();
  };

  return (
    <div className="panel flex flex-col gap-2 p-3">
      {downed ? (
        <div className="flex items-center gap-2 rounded-md border border-[#d0566a]/40 bg-[#d0566a]/10 px-3 py-1.5 text-xs text-[#e8b7c0]">
          <Lock className="h-3.5 w-3.5" />
          Estás moribundo. Solo puedes tirar una salvación de muerte.
        </div>
      ) : incapacitated ? (
        <div className="flex items-center gap-2 rounded-md border border-[#d0566a]/40 bg-[#d0566a]/10 px-3 py-1.5 text-xs text-[#e8b7c0]">
          <Lock className="h-3.5 w-3.5" />
          Estás incapacitado: no puedes realizar acciones ni reacciones.
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
          <span className="stat-chip mr-1 inline-flex items-center gap-1.5 px-2 py-1 text-[11px]">
            <Sparkles className="h-3 w-3 text-[#e0b84a]" />
            <span className="text-muted">Adicional</span>
            <span className={bonusUsed ? "font-semibold text-[#d0566a]" : "font-semibold text-[#6fd3a0]"}>
              {bonusUsed ? "usada" : "libre"}
            </span>
          </span>
          {concentrationName && (
            <span className="stat-chip mr-1 inline-flex items-center gap-1.5 px-2 py-1 text-[11px]">
              <Sparkles className="h-3 w-3 text-[#9b7cff]" />
              <span className="text-muted">Conc.</span>
              <span className="font-semibold text-[#c9a6ff]">
                {concentrationName} ({concentrationRounds ?? 0})
              </span>
            </span>
          )}

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

          <Button
            variant="secondary"
            size="sm"
            disabled={!canActNow}
            title="Correr: dobla tu movimiento este turno"
            onClick={() => onCombatAction("DASH")}
          >
            <Wind className="h-4 w-4" /> Correr
          </Button>
          <Button
            variant="secondary"
            size="sm"
            disabled={!canActNow}
            title="Esquivar: los ataques contra ti tienen desventaja"
            onClick={() => onCombatAction("DODGE")}
          >
            <Shield className="h-4 w-4" /> Esquivar
          </Button>
          <Button
            variant="secondary"
            size="sm"
            disabled={!canActNow}
            title="Retirada: tu movimiento no provoca ataques de oportunidad"
            onClick={() => onCombatAction("DISENGAGE")}
          >
            <Undo2 className="h-4 w-4" /> Retirada
          </Button>

          <div className="relative">
            <Button
              variant="secondary"
              size="sm"
              disabled={locked}
              title="Ayudar, ocultarse, buscar, prepararse, usar un objeto o interactuar"
              aria-expanded={actionsOpen}
              onClick={() => setActionsOpen((value) => !value)}
            >
              <HandHelping className="h-4 w-4" /> Acciones
            </Button>
            {actionsOpen && (
              <div className="absolute bottom-full left-0 z-30 mb-2 max-h-[70dvh] w-[min(23rem,calc(100vw-2rem))] overflow-y-auto rounded-lg border border-[#3a2a1a] bg-[#1a130d] p-2 shadow-lg">
                <div className="mb-2 flex items-center justify-between px-1 text-[10px] uppercase tracking-wider text-muted">
                  <span>Acciones de combate</span>
                  <span>Interacción: 1/turno</span>
                </div>
                <div className="grid grid-cols-1 gap-1 sm:grid-cols-2">
                  <button
                    type="button"
                    disabled={!canActNow || Boolean(objectInteractionUsed)}
                    title="Interacción gratuita con un objeto: no consume tu acción"
                    onClick={() => runAction(onInteract)}
                    className={ACTION_ITEM}
                  >
                    <Hand className="h-3.5 w-3.5 text-[#e0b84a]" />
                    {objectInteractionUsed ? "Interacción usada" : "Interactuar"}
                  </button>
                  <button
                    type="button"
                    disabled={!canActNow || Boolean(actionUsed) || !selectedAllyName}
                    title={selectedAllyName
                      ? `Ayudar a ${selectedAllyName}: ventaja en su próximo ataque`
                      : "Selecciona un aliado en el tablero para ayudarlo"}
                    onClick={() => runAction(onHelp)}
                    className={ACTION_ITEM}
                  >
                    <HandHelping className="h-3.5 w-3.5 text-[#6fd3a0]" /> Ayudar
                  </button>
                  <button
                    type="button"
                    disabled={!canActNow || Boolean(actionUsed) || Boolean(hidden)}
                    title="Ocultarse: solo si no hay enemigos a 5 pies; ventaja en tu próximo ataque"
                    onClick={() => runAction(onHide)}
                    className={ACTION_ITEM}
                  >
                    <EyeOff className="h-3.5 w-3.5 text-[#9b7cff]" />
                    {hidden ? "Oculto" : "Ocultarse"}
                  </button>
                  <button
                    type="button"
                    disabled={!canActNow || Boolean(actionUsed) || Boolean(readied)}
                    title="Prepararse: ataca con tu reacción cuando un enemigo entre en tu alcance"
                    onClick={() => runAction(onReady)}
                    className={ACTION_ITEM}
                  >
                    <Timer className="h-3.5 w-3.5 text-[#e0b84a]" />
                    {readied ? "Preparado" : "Prepararse"}
                  </button>
                  <button
                    type="button"
                    disabled={!canActNow || Boolean(actionUsed)}
                    title="Buscar con Percepción (CD 12)"
                    onClick={() => runAction(() => onSearch("perception"))}
                    className={ACTION_ITEM}
                  >
                    <Search className="h-3.5 w-3.5 text-[#c9a6ff]" /> Buscar (Percepción)
                  </button>
                  <button
                    type="button"
                    disabled={!canActNow || Boolean(actionUsed)}
                    title="Buscar con Investigación (CD 12)"
                    onClick={() => runAction(() => onSearch("investigation"))}
                    className={ACTION_ITEM}
                  >
                    <Search className="h-3.5 w-3.5 text-[#c9a6ff]" /> Buscar (Investigación)
                  </button>
                </div>
                <div className="mt-2 border-t border-parchment/10 pt-2">
                  <p className="mb-1 px-1 text-[10px] uppercase tracking-wider text-muted">Usar objeto</p>
                  {usableItems.length === 0 ? (
                    <p className="px-1 text-[11px] text-muted">Sin objetos utilizables.</p>
                  ) : (
                    <div className="flex flex-wrap gap-1">
                      {usableItems.map((item) => (
                        <button
                          key={item.name}
                          type="button"
                          disabled={!canActNow || Boolean(actionUsed)}
                          title={item.description}
                          onClick={() => runAction(() => onUseObject(item.name))}
                          className="inline-flex items-center gap-1 rounded-md border border-parchment/10 bg-surface/60 px-2 py-1 text-[11px] text-muted transition hover:border-gold/40 hover:text-ink disabled:cursor-not-allowed disabled:opacity-40"
                        >
                          <Package className="h-3 w-3 text-gold" />
                          {item.name}
                          {item.quantity > 1 && <span className="opacity-70">x{item.quantity}</span>}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          {prone && (
            <Button
              variant="secondary"
              size="sm"
              disabled={!canActNow}
              title="Levantarse cuesta la mitad de tu velocidad"
              onClick={onStandUp}
            >
              <ArrowUpFromLine className="h-4 w-4" /> Levantarse
            </Button>
          )}
          {classId === "fighter" && (
            <Button
              variant="secondary"
              size="sm"
              disabled={!canActNow || Boolean(bonusUsed) || Boolean(secondWindUsed)}
              title={secondWindUsed
                ? "Ya has usado Segundo aliento; necesitas un descanso"
                : "Segundo aliento: 1d10 + nivel PG (acción adicional, 1 uso)"}
              onClick={onSecondWind}
            >
              <HeartPulse className="h-4 w-4" /> Segundo aliento
            </Button>
          )}
          {classId === "barbarian" && (
            <Button
              variant={raging ? "danger" : "secondary"}
              size="sm"
              disabled={!canActNow || Boolean(bonusUsed) || Boolean(raging) || (ragesUsed ?? 0) >= 2}
              title="Furia: +2 al daño cuerpo a cuerpo y resistencia física (acción adicional, 2 usos)"
              onClick={onRage}
            >
              <Flame className="h-4 w-4" /> {raging ? "Furia activa" : `Furia (${2 - (ragesUsed ?? 0)})`}
            </Button>
          )}
          {classId === "bard" && (
            <Button
              variant="secondary"
              size="sm"
              disabled={!canActNow || Boolean(bonusUsed) || (bardicInspirationUses ?? 0) <= 0}
              title="Inspiración bárdica: un aliado a 12 casillas gana un d6 (acción adicional)"
              onClick={onBardicInspiration}
            >
              <Music className="h-4 w-4" /> Inspirar ({bardicInspirationUses ?? 0})
            </Button>
          )}
          {Boolean(inspirationDie) && (
            <Button
              variant={inspirationPending ? "danger" : "secondary"}
              size="sm"
              disabled={!canActNow || Boolean(inspirationPending)}
              title="Usa el d6 de inspiración en tu próxima tirada de ataque, prueba o salvación"
              onClick={onUseInspiration}
            >
              <Sparkles className="h-4 w-4" />
              {inspirationPending ? "Inspiración lista" : `Inspiración d${inspirationDie}`}
            </Button>
          )}

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
          {spells.length > 0 && (
            <div className="relative">
              <Button
                variant="ghost"
                size="sm"
                disabled={locked}
                onClick={() => setSpellOpen((value) => !value)}
              >
                <BookOpen className="h-4 w-4" /> Conjuros
                <span className="ml-1 text-[10px] text-muted">
                  {Math.max(0, spellSlotsMax - spellSlotsUsed)}/{spellSlotsMax}
                </span>
              </Button>
              {spellOpen && (
                <div className="absolute bottom-full left-0 z-30 mb-2 w-[min(20rem,calc(100vw-2rem))] rounded-lg border border-[#3a2a1a] bg-[#1a130d] p-2 shadow-lg lg:left-auto lg:right-0">
                  <div className="mb-2 flex items-center justify-between px-1 text-[10px] uppercase tracking-wider text-muted">
                    <span>Conjuros</span>
                    <span>
                      Espacios {Math.max(0, spellSlotsMax - spellSlotsUsed)}/{spellSlotsMax}
                    </span>
                  </div>
                  <div className="max-h-72 space-y-1 overflow-y-auto">
                    {spells.map((spell) => {
                      const bonusSpell = spell.bonusAction;
                      const used = bonusSpell ? bonusUsed : actionUsed;
                      const noSlot = spell.level > 0 && spellSlotsUsed >= spellSlotsMax;
                      const disabled = !canActNow || Boolean(used) || noSlot;
                      return (
                        <button
                          key={spell.id}
                          disabled={disabled}
                          title={spell.material ? `Componente material: ${spell.material}` : undefined}
                          onClick={() => {
                            onCastSpell(spell.id);
                            setSpellOpen(false);
                          }}
                          className="w-full rounded-md border border-parchment/10 bg-surface/60 px-2.5 py-2 text-left transition hover:border-gold/40 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                          <div className="flex items-center justify-between gap-2">
                            <span className="text-sm text-ink">{spell.name}</span>
                            <span className="shrink-0 text-[10px] uppercase tracking-wider text-muted">
                              {spell.level === 0 ? "Truco" : `Nv ${spell.level}`}
                              {spell.bonusAction ? " · Adicional" : ""}
                            </span>
                          </div>
                          <p className="mt-0.5 text-[11px] leading-snug text-muted">{spell.description}</p>
                          <p className="mt-1 text-[10px] text-[#9b7cff]">
                            {spell.range} casillas · {spellKindLabel(spell)}
                            {spell.components ? ` · ${spell.components}` : ""}
                            {noSlot ? " · Sin espacios" : ""}
                          </p>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          )}
          <div className="relative">
            <Button
              variant="ghost"
              size="sm"
              disabled={locked}
              title="Descanso corto o largo (solo fuera de combate)"
              aria-expanded={restOpen}
              onClick={() => setRestOpen((value) => !value)}
            >
              <Moon className="h-4 w-4" /> Descanso
            </Button>
            {restOpen && (
              <div className="absolute bottom-full left-0 z-30 mb-2 w-[min(19rem,calc(100vw-2rem))] rounded-lg border border-[#3a2a1a] bg-[#1a130d] p-2 shadow-lg">
                <div className="mb-2 flex items-center justify-between px-1 text-[10px] uppercase tracking-wider text-muted">
                  <span>Descanso</span>
                  <span>
                    Dados de golpe {hitDiceRemaining}/{hitDiceMax}
                  </span>
                </div>
                <div className="grid grid-cols-1 gap-1">
                  <button
                    type="button"
                    disabled={!canActNow || Boolean(inCombat) || hitDiceRemaining <= 0}
                    title="Descanso corto: gasta un dado de golpe y recupera su tirada + CON"
                    onClick={() => runRest(onShortRest)}
                    className={ACTION_ITEM}
                  >
                    <HeartPulse className="h-3.5 w-3.5 text-[#6fd3a0]" />
                    <span className="flex flex-col">
                      <span>Descanso corto</span>
                      <span className="text-[10px] text-muted">
                        Gasta 1 dado de golpe · no recupera espacios de conjuro
                      </span>
                    </span>
                  </button>
                  <button
                    type="button"
                    disabled={!canActNow || Boolean(inCombat)}
                    title="Descanso largo: recupera PG, espacios de conjuro y rasgos de clase"
                    onClick={() => runRest(onLongRest)}
                    className={ACTION_ITEM}
                  >
                    <Moon className="h-3.5 w-3.5 text-[#9b7cff]" />
                    <span className="flex flex-col">
                      <span>Descanso largo</span>
                      <span className="text-[10px] text-muted">
                        PG al máximo, recursos y la mitad de los dados de golpe
                      </span>
                    </span>
                  </button>
                </div>
                {inCombat && (
                  <p className="mt-2 border-t border-parchment/10 px-1 pt-2 text-[11px] text-[#e8b7c0]">
                    No puedes descansar durante un combate.
                  </p>
                )}
                {!inCombat && hitDiceRemaining <= 0 && (
                  <p className="mt-2 border-t border-parchment/10 px-1 pt-2 text-[11px] text-muted">
                    No te quedan dados de golpe: necesitas un descanso largo.
                  </p>
                )}
              </div>
            )}
          </div>
          <div className="relative">
            <Button variant="ghost" size="sm" disabled={locked} onClick={() => setDiceOpen((value) => !value)}>
              <Dices className="h-4 w-4" /> Dados
            </Button>
            {diceOpen && (
              <div className="absolute bottom-full left-0 z-30 mb-2 grid grid-cols-3 gap-1 rounded-lg border border-[#3a2a1a] bg-[#1a130d] p-2 shadow-lg">
                {DICE_SIDES.map((sides) => (
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
          <Button variant="secondary" size="sm" disabled={!isMyTurn || Boolean(busy)} onClick={onEndTurn}>
            <MoveRight className="h-4 w-4" /> Terminar turno
          </Button>
        </div>
      </div>
    </div>
  );
}

const ACTION_ITEM =
  "inline-flex items-center gap-1.5 rounded-md border border-parchment/10 bg-surface/60 px-2 py-1.5 text-left text-xs text-ink transition hover:border-gold/40 disabled:cursor-not-allowed disabled:opacity-40";

const ABILITY_LABELS: Record<string, string> = {
  STRENGTH: "FUE",
  DEXTERITY: "DES",
  CONSTITUTION: "CON",
  INTELLIGENCE: "INT",
  WISDOM: "SAB",
  CHARISMA: "CAR",
};

function spellKindLabel(spell: Spell): string {
  switch (spell.kind) {
    case "ATTACK":
      return "ataque de conjuro";
    case "SAVE":
      return `salvación ${ABILITY_LABELS[spell.saveAbility ?? ""] ?? spell.saveAbility ?? ""}`.trim();
    case "AUTO":
      return "impacto automático";
    case "HEAL":
      return "curación";
    case "BUFF":
      return "mejora";
    case "AREA":
      return "área";
    default:
      return "";
  }
}
