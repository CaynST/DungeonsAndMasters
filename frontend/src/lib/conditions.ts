import type { GameEntity } from "@/types/game";

export type ConditionTone = "gold" | "crimson" | "emerald" | "arcane" | "muted";

export interface ConditionMeta {
  label: string;
  description: string;
  tone: ConditionTone;
  color: string;
}

export interface ActiveConditionView extends ConditionMeta {
  code: string;
  rounds?: number;
  level?: number;
}

/** Player-facing metadata for the 5e conditions. Codes match the backend. */
export const CONDITION_META: Record<string, ConditionMeta> = {
  BLINDED: {
    label: "Cegado",
    description: "No puede ver: sus ataques tienen desventaja y los ataques contra él, ventaja.",
    tone: "gold",
    color: "#c9a227",
  },
  CHARMED: {
    label: "Encantado",
    description: "No puede atacar a quien lo encantó.",
    tone: "arcane",
    color: "#9b7cff",
  },
  DEAFENED: {
    label: "Sordo",
    description: "No puede oír.",
    tone: "muted",
    color: "#8b8175",
  },
  EXHAUSTION: {
    label: "Agotamiento",
    description: "Cada nivel impone -2 a las pruebas de d20; el nivel 2 reduce la velocidad.",
    tone: "crimson",
    color: "#d0566a",
  },
  FRIGHTENED: {
    label: "Asustado",
    description: "Desventaja en pruebas y ataques mientras vea la fuente del miedo.",
    tone: "arcane",
    color: "#9b7cff",
  },
  GRAPPLED: {
    label: "Agarrado",
    description: "Su velocidad es 0.",
    tone: "gold",
    color: "#c9a227",
  },
  INCAPACITATED: {
    label: "Incapacitado",
    description: "No puede realizar acciones ni reacciones.",
    tone: "crimson",
    color: "#d0566a",
  },
  INVISIBLE: {
    label: "Invisible",
    description: "Los ataques contra él tienen desventaja y los suyos, ventaja.",
    tone: "emerald",
    color: "#6fd3a0",
  },
  PARALYZED: {
    label: "Paralizado",
    description: "Incapacitado y sin movimiento; los ataques contra él tienen ventaja y los cuerpo a cuerpo son críticos.",
    tone: "arcane",
    color: "#9b7cff",
  },
  PETRIFIED: {
    label: "Petrificado",
    description: "Incapacitado y sin movimiento; resistencia a todo daño.",
    tone: "muted",
    color: "#8b8175",
  },
  POISONED: {
    label: "Envenenado",
    description: "Desventaja en ataques y pruebas.",
    tone: "emerald",
    color: "#6fd3a0",
  },
  PRONE: {
    label: "Derribado",
    description: "Solo puede arrastrarse; los ataques cuerpo a cuerpo contra él tienen ventaja y los suyos, desventaja.",
    tone: "gold",
    color: "#c9a227",
  },
  RESTRAINED: {
    label: "Apresado",
    description: "Velocidad 0; ataques contra él con ventaja; sus ataques con desventaja.",
    tone: "gold",
    color: "#c9a227",
  },
  STUNNED: {
    label: "Aturdido",
    description: "Incapacitado y sin movimiento; los ataques contra él tienen ventaja.",
    tone: "arcane",
    color: "#9b7cff",
  },
  UNCONSCIOUS: {
    label: "Inconsciente",
    description: "Incapacitado y derribado; los ataques cuerpo a cuerpo contra él son críticos.",
    tone: "crimson",
    color: "#d0566a",
  },
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

/** Reads the persisted conditions array from an entity's free-form data map. */
export function entityConditions(entity?: GameEntity | null): ActiveConditionView[] {
  const raw = entity?.data?.conditions;
  if (!Array.isArray(raw)) return [];
  return raw.flatMap((item): ActiveConditionView[] => {
    const code = typeof item === "string"
      ? item
      : isRecord(item) && typeof item.code === "string"
        ? item.code
        : null;
    if (!code) return [];
    const meta = CONDITION_META[code];
    if (!meta) return [];
    const rounds = isRecord(item) && typeof item.rounds === "number" ? item.rounds : undefined;
    const level = isRecord(item) && typeof item.level === "number" ? item.level : undefined;
    return [{ code, ...meta, rounds, level }];
  });
}

export function hasCondition(entity: GameEntity | undefined | null, code: string): boolean {
  return entityConditions(entity).some((condition) => condition.code === code);
}

/** True for conditions that forbid actions and reactions. */
export function isIncapacitated(entity: GameEntity | undefined | null): boolean {
  return entityConditions(entity).some((condition) =>
    ["INCAPACITATED", "PARALYZED", "PETRIFIED", "STUNNED", "UNCONSCIOUS"].includes(condition.code),
  );
}
