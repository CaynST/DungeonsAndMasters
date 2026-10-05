import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function abilityModifier(score: number): number {
  return Math.floor((score - 10) / 2);
}

export function formatModifier(value: number): string {
  return value >= 0 ? `+${value}` : `${value}`;
}

export function hpPercent(hp: number, maxHp: number): number {
  if (maxHp <= 0) return 0;
  return Math.max(0, Math.min(100, (hp / maxHp) * 100));
}

export function hpColor(hp: number, maxHp: number): string {
  const pct = hpPercent(hp, maxHp);
  if (pct > 60) return "#3E9C6E";
  if (pct > 30) return "#C9A227";
  return "#B0364B";
}

export const EL = {
  objective: "Objetivo",
  round: "Ronda",
  turn: "Turno",
  initiative: "Iniciativa",
  dungeonMaster: "Dungeon Master",
  combat: "Combate",
  explore: "Explorar",
  endTurn: "Terminar turno",
  start: "Comenzar aventura",
} as const;
