/** Roll state reported by the rules engine for an attack event. */
export type RollAdvantage = "ADVANTAGE" | "DISADVANTAGE";

/**
 * Reads the effective advantage/disadvantage from an event payload.
 *
 * Attack events store it as `"ADVANTAGE"`, `"DISADVANTAGE"` or `"NONE"`.
 * Payloads are untrusted persisted JSON, so malformed data simply hides the badge.
 */
export function attackAdvantage(dataJson: string | undefined): RollAdvantage | null {
  if (!dataJson) return null;
  try {
    const data = JSON.parse(dataJson) as { advantage?: unknown };
    if (data.advantage === "ADVANTAGE" || data.advantage === "DISADVANTAGE") {
      return data.advantage;
    }
    return null;
  } catch {
    return null;
  }
}
