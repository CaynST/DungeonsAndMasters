/** Class ids shared by feature gating. Prefers persisted creation data, then display name. */
const CLASS_IDS: Record<string, string> = {
  fighter: "fighter",
  guerrero: "fighter",
  barbarian: "barbarian",
  "bárbaro": "barbarian",
  barbaro: "barbarian",
  rogue: "rogue",
  "pícaro": "rogue",
  picaro: "rogue",
  wizard: "wizard",
  mago: "wizard",
  cleric: "cleric",
  "clérigo": "cleric",
  clerigo: "cleric",
  bard: "bard",
  bardo: "bard",
  monk: "monk",
  monje: "monk",
  paladin: "paladin",
  "paladín": "paladin",
  ranger: "ranger",
  explorador: "ranger",
  sorcerer: "sorcerer",
  hechicero: "sorcerer",
  warlock: "warlock",
  brujo: "warlock",
  druid: "druid",
  druida: "druid",
};

export function classIdOf(
  cls?: string,
  creationData?: Record<string, unknown>,
): string | null {
  const explicit = creationData && typeof creationData.classId === "string"
    ? creationData.classId.trim().toLowerCase()
    : "";
  if (explicit) return explicit;
  if (!cls) return null;
  return CLASS_IDS[cls.trim().toLowerCase()] ?? null;
}
