import { BookOpen, Package, Swords } from "lucide-react";
import type { PlayerCharacter } from "@/types/game";
import { AbilityScore } from "@/components/ui/AbilityScore";
import { HealthBar } from "@/components/ui/HealthBar";
import { Badge } from "@/components/ui/Badge";
import { abilityModifier, formatModifier, formatWeight } from "@/lib/utils";

export function CharacterSheet({
  character,
  compact = false,
  onUseItem,
}: {
  character: PlayerCharacter;
  compact?: boolean;
  onUseItem?: (item: string) => void;
}) {
  const stats: [string, number][] = [
    ["FUE", character.stats.strength],
    ["DES", character.stats.dexterity],
    ["CON", character.stats.constitution],
    ["INT", character.stats.intelligence],
    ["SAB", character.stats.wisdom],
    ["CAR", character.stats.charisma],
  ];
  const languages = stringList(character.creationData?.languages);
  const armorProficiencies = stringList(character.creationData?.armorProficiencies);
  const weaponProficiencies = stringList(character.creationData?.weaponProficiencies);
  const toolProficiencies = stringList(character.creationData?.toolProficiencies);
  const hasProficiencies =
    languages.length + armorProficiencies.length + weaponProficiencies.length + toolProficiencies.length > 0;
  // The backend is authoritative; the fallback keeps legacy payloads usable.
  const carriedWeight =
    character.carriedWeight ??
    character.inventory.reduce((total, item) => total + (item.weight ?? 0) * item.quantity, 0);
  const carryingCapacity = character.carryingCapacity ?? character.stats.strength * 15;
  const encumbered = character.encumbered ?? carriedWeight > carryingCapacity;

  return (
    <div className="panel p-4">
      <div className="mb-3 flex items-center gap-3">
        <span
          className="title-display flex h-11 w-11 items-center justify-center rounded-full border-2 text-lg font-bold"
          style={{ borderColor: character.color ?? "#C9A227", color: character.color ?? "#C9A227" }}
        >
          {character.name.charAt(0)}
        </span>
        <div className="min-w-0">
          <h2 className="title-display truncate text-lg font-semibold text-parchment">{character.name}</h2>
          <p className="text-[11px] uppercase tracking-wider text-muted">
            {character.race} · {character.cls}
            {typeof character.creationData?.subclass === "string" && ` · ${character.creationData.subclass}`}
          </p>
        </div>
        <Badge tone="arcane" className="ml-auto">
          Nv {character.level}
        </Badge>
      </div>

      <HealthBar value={character.hp} max={character.maxHp} />

      <div className="my-3 grid grid-cols-3 items-center rounded-lg border border-parchment/10 bg-black/25 py-2 text-center">
        <div>
          <p className="text-[10px] uppercase tracking-widest text-muted">CA</p>
          <p className="title-display text-lg text-ink">
            <span aria-hidden="true">🛡</span> {character.ac}
          </p>
        </div>
        <div className="border-x border-parchment/10">
          <p className="text-[10px] uppercase tracking-widest text-muted">Velocidad</p>
          <p className="title-display text-lg text-ink">{character.speed}</p>
        </div>
        <div>
          <p className="text-[10px] uppercase tracking-widest text-muted">Carga</p>
          <p className="title-display text-sm text-ink">
            {formatWeight(carriedWeight)} / {carryingCapacity} lb
          </p>
        </div>
      </div>

      {encumbered && (
        <p
          role="status"
          className="mb-3 rounded-md border border-[#B0364B]/40 bg-[#B0364B]/10 px-3 py-1.5 text-[11px] text-[#e7a3ad]"
        >
          Sobrecargado: {formatWeight(carriedWeight)} lb supera tu capacidad de {carryingCapacity} lb. La velocidad no
          cambia todavía.
        </p>
      )}

      <div className="grid grid-cols-6 gap-1.5">
        {stats.map(([label, value]) => (
          <AbilityScore key={label} label={label} value={value} modifier={abilityModifier(value)} />
        ))}
      </div>

      {!compact && (
        <>
          <div className="mt-4">
            <p className="mb-2 flex items-center gap-1.5 text-xs uppercase tracking-wider text-muted">
              <Swords className="h-3.5 w-3.5 text-crimson-soft" aria-hidden="true" /> Ataques
            </p>
            <ul className="space-y-1">
              {character.attacks.map((attack) => (
                <li
                  key={attack.name}
                  className="flex items-center justify-between rounded-md border border-parchment/10 bg-surface/50 px-3 py-1.5 text-sm"
                >
                  <span className="text-ink">{attack.name}</span>
                  <span className="text-xs text-muted">
                    {formatModifier(attack.attackBonus)} · {attack.damageExpression}
                    {attack.ranged && ` · ${attack.range} casillas`}
                  </span>
                </li>
              ))}
            </ul>
          </div>

          {hasProficiencies && (
            <div className="mt-4">
              <p className="mb-2 flex items-center gap-1.5 text-xs uppercase tracking-wider text-muted">
                <BookOpen className="h-3.5 w-3.5 text-arcane-soft" aria-hidden="true" /> Idiomas y competencias
              </p>
              <div className="space-y-1.5">
                <ProficiencyGroup label="Idiomas" items={languages} />
                <ProficiencyGroup label="Armaduras" items={armorProficiencies} />
                <ProficiencyGroup label="Armas" items={weaponProficiencies} />
                <ProficiencyGroup label="Herramientas" items={toolProficiencies} />
              </div>
            </div>
          )}

          <div className="mt-4">
            <p className="mb-2 flex items-center gap-1.5 text-xs uppercase tracking-wider text-muted">
              <Package className="h-3.5 w-3.5 text-gold" aria-hidden="true" /> Inventario
            </p>
            <div className="flex flex-wrap gap-1.5">
              {character.inventory.length === 0 && <span className="text-xs text-muted">Vacío</span>}
              {character.inventory.map((item) => {
                const consumable = /poción|pocion|elixir|vial/i.test(item.name);
                return (
                  <span
                    key={item.name}
                    className="inline-flex items-center gap-1.5 rounded-md border border-parchment/10 bg-black/30 px-2 py-1 text-xs text-muted"
                    title={item.description}
                  >
                    {item.name}
                    {item.quantity > 1 && <span className="text-[10px] opacity-70">x{item.quantity}</span>}
                    {typeof item.weight === "number" && item.weight > 0 && (
                      <span className="text-[10px] opacity-70">{formatWeight(item.weight * item.quantity)} lb</span>
                    )}
                    {consumable && onUseItem && (
                      <button
                        type="button"
                        aria-label={`Usar ${item.name}`}
                        onClick={() => onUseItem(item.name)}
                        className="inline-flex min-h-6 min-w-6 items-center justify-center rounded border border-[#3e9c6e]/40 px-2 py-1 text-[10px] text-[#6fd3a0] transition hover:bg-[#3e9c6e]/15 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-soft/70 focus-visible:ring-offset-2 focus-visible:ring-offset-[#1e160f]"
                      >
                        Usar
                      </button>
                    )}
                  </span>
                );
              })}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function ProficiencyGroup({ label, items }: { label: string; items: string[] }) {
  if (items.length === 0) return null;
  return (
    <div className="flex flex-wrap items-baseline gap-1.5">
      <span className="text-[10px] uppercase tracking-widest text-muted">{label}</span>
      {items.map((item) => (
        <span
          key={item}
          className="rounded border border-parchment/10 bg-black/30 px-2 py-0.5 text-xs text-muted"
          title={item}
        >
          {item}
        </span>
      ))}
    </div>
  );
}

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
}
