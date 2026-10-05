import type { DiceRoll } from "@/types/game";

export type DiceOutcome = "crit" | "fail" | "max" | "normal";

/** Extracts the number of sides from the first die in an expression like "2d6+3". */
export function primarySides(expression: string | undefined): number {
  const match = /(\d*)d(\d+)/i.exec(expression ?? "");
  return match ? Number(match[2]) : 20;
}

/**
 * Classifies a roll. Defensive by design: check results and malformed payloads may arrive
 * without a `rolls` array, and must never crash the dice tray.
 */
export function outcomeOf(roll: DiceRoll | null | undefined): DiceOutcome {
  const rolls = roll?.rolls ?? [];
  if (roll?.natural20) return "crit";
  if (roll?.natural1) return "fail";
  const allMax = rolls.length > 0 && rolls.every((value) => value === 20);
  return allMax ? "max" : "normal";
}

export function firstFace(roll: DiceRoll | null | undefined): number {
  return roll?.rolls?.[0] ?? roll?.total ?? 0;
}
