import { useQuery } from "@tanstack/react-query";
import { BookOpen } from "lucide-react";
import { api } from "@/lib/api";
import { Spinner } from "@/components/ui/Spinner";
import type { GameEvent } from "@/types/game";

const TONE: Record<string, string> = {
  CAMPAIGN_STARTED: "#c9a227",
  SCENE_CHANGED: "#9b7cff",
  NARRATION: "#c9a227",
  COMBAT_STARTED: "#d0566a",
  COMBAT_ENDED: "#6fd3a0",
  ATTACK: "#e06a7a",
  ATTACK_MISS: "#8a7f6d",
  DEATH: "#d0566a",
  HEAL: "#6fd3a0",
  CHECK: "#c9a227",
  CHECK_REQUESTED: "#c9a227",
  DICE_ROLL: "#d8c39a",
  REWARD: "#c9a227",
  ADVENTURE_ENDED: "#c9a227",
  SHOP_OPENED: "#c9a227",
  PURCHASE: "#c9a227",
  SALE: "#c9a227",
  MOVE: "#8a7f6d",
  TURN_CHANGED: "#8a7f6d",
  NPC_TURN: "#8a7f6d",
  PLAYER_JOINED: "#6fd3a0",
  PLAYER_ACTION: "#c9a227",
};

/** Full chronicle of the campaign, so a latecomer can catch up on the story. */
export function Chronicle({ campaignId }: { campaignId: number }) {
  const events = useQuery({
    queryKey: ["chronicle", campaignId],
    queryFn: () => api.events(campaignId),
  });

  return (
    <div className="panel flex h-full flex-col">
      <div className="flex items-center gap-2 border-b border-[#2b2118] px-4 py-3">
        <BookOpen className="h-4 w-4 text-[#c9a227]" />
        <h3 className="title-display text-sm font-semibold text-ink">Crónica de la aventura</h3>
      </div>
      <div className="min-h-0 flex-1 overflow-y-auto p-4">
        {events.isLoading && (
          <p className="flex items-center gap-2 text-sm text-muted">
            <Spinner /> Recuperando la crónica...
          </p>
        )}
        {events.data && events.data.length === 0 && (
          <p className="text-sm text-muted">La historia aún no ha comenzado.</p>
        )}
        <ol className="relative space-y-3 border-l border-[#2b2118] pl-4">
          {(events.data ?? []).map((event: GameEvent) => (
            <li key={event.id} className="relative">
              <span
                className="absolute -left-[21px] top-1.5 h-2.5 w-2.5 rounded-full border-2 border-[#15110d]"
                style={{ backgroundColor: TONE[event.type] ?? "#8a7f6d" }}
              />
              <p className="text-[10px] uppercase tracking-wider text-muted">{event.type.replace(/_/g, " ")}</p>
              <p className="text-sm leading-relaxed text-ink">
                {event.actor && <span className="font-medium text-[#d8c9a8]">{event.actor}: </span>}
                {event.message}
              </p>
            </li>
          ))}
        </ol>
      </div>
    </div>
  );
}
