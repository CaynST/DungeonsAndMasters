import { Package, Swords } from "lucide-react";
import type { PlayerCharacter } from "@/types/game";
import { AbilityScore } from "@/components/ui/AbilityScore";
import { HealthBar } from "@/components/ui/HealthBar";
import { Badge } from "@/components/ui/Badge";
import { abilityModifier, formatModifier } from "@/lib/utils";

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
          <h3 className="title-display truncate text-lg font-semibold text-parchment">{character.name}</h3>
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

      <div className="my-3 flex items-center justify-around rounded-lg border border-parchment/10 bg-black/25 py-2 text-center">
        <div>
          <p className="text-[10px] uppercase tracking-widest text-muted">CA</p>
          <p className="title-display text-lg text-ink">🛡 {character.ac}</p>
        </div>
        <div className="h-8 w-px bg-parchment/10" />
        <div>
          <p className="text-[10px] uppercase tracking-widest text-muted">Velocidad</p>
          <p className="title-display text-lg text-ink">{character.speed}</p>
        </div>
      </div>

      <div className="grid grid-cols-6 gap-1.5">
        {stats.map(([label, value]) => (
          <AbilityScore key={label} label={label} value={value} modifier={abilityModifier(value)} />
        ))}
      </div>

      {!compact && (
        <>
          <div className="mt-4">
            <p className="mb-2 flex items-center gap-1.5 text-xs uppercase tracking-wider text-muted">
              <Swords className="h-3.5 w-3.5 text-crimson-soft" /> Ataques
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

          <div className="mt-4">
            <p className="mb-2 flex items-center gap-1.5 text-xs uppercase tracking-wider text-muted">
              <Package className="h-3.5 w-3.5 text-gold" /> Inventario
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
                    {consumable && onUseItem && (
                      <button
                        onClick={() => onUseItem(item.name)}
                        className="rounded border border-[#3e9c6e]/40 px-1.5 py-0.5 text-[10px] text-[#6fd3a0] transition hover:bg-[#3e9c6e]/15"
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
