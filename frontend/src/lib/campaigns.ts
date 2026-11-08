import type { CampaignStatus, GamePhase } from "@/types/game";

/** Badge tones accepted by `components/ui/Badge` for campaign status. */
export type CampaignStatusTone = "gold" | "emerald" | "muted";

const STATUS_LABEL: Record<CampaignStatus, string> = {
  LOBBY: "En el lobby",
  ACTIVE: "En curso",
  FINISHED: "Completada",
};

const STATUS_TONE: Record<CampaignStatus, CampaignStatusTone> = {
  LOBBY: "gold",
  ACTIVE: "emerald",
  FINISHED: "muted",
};

const PHASE_LABEL: Record<GamePhase, string> = {
  INTRO: "Introducción",
  EXPLORATION: "Exploración",
  SOCIAL: "Social",
  COMBAT: "Combate",
  BOSS: "Jefe final",
  COMPLETE: "Completada",
};

/** Spanish label for a campaign status, shared by Home, Lobby and campaign cards. */
export function campaignStatusLabel(status: CampaignStatus): string {
  return STATUS_LABEL[status] ?? status;
}

/** Badge tone matching the campaign status. */
export function campaignStatusTone(status: CampaignStatus): CampaignStatusTone {
  return STATUS_TONE[status] ?? "muted";
}

/** Spanish label for the narrative phase shown on cards and the lobby header. */
export function campaignPhaseLabel(phase: GamePhase): string {
  return PHASE_LABEL[phase] ?? phase;
}
