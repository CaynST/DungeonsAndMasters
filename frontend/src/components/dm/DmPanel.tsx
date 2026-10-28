import { useEffect, useRef, useState } from "react";
import { motion } from "framer-motion";
import { BookOpen, Dices, ScrollText, Sparkles, User } from "lucide-react";
import type { GameEvent } from "@/types/game";
import { useGameStore, type PendingCheck } from "@/store/useGameStore";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";

interface Props {
  dmName: string;
  dmAvatar?: string;
  narration: string | null;
  events: GameEvent[];
  providerId?: string;
  spotlightName?: string;
  busy?: boolean;
  pendingCheck?: PendingCheck | null;
  onRollCheck: (skill: string, dc: number) => void;
  onSay: (text: string) => void;
}

const EVENT_TONE: Record<string, string> = {
  ATTACK: "#E06A7A",
  ATTACK_MISS: "#9E9485",
  DEATH: "#B0364B",
  HEAL: "#6FD3A0",
  CHECK: "#C9A227",
  CHECK_REQUESTED: "#C9A227",
  COMBAT_STARTED: "#E06A7A",
  COMBAT_ENDED: "#6FD3A0",
  SCENE_CHANGED: "#A88CFF",
  DICE_ROLL: "#D8C39A",
  REWARD: "#C9A227",
  ADVENTURE_ENDED: "#C9A227",
};

export function DmPanel({ dmName, dmAvatar, narration, events, providerId, spotlightName, busy, pendingCheck, onRollCheck, onSay }: Props) {
  const dmThinking = useGameStore((state) => state.dmThinking);
  const scrollRef = useRef<HTMLDivElement>(null);
  const [text, setText] = useState("");

  useEffect(() => {
    scrollRef.current?.scrollTo?.({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [events.length, narration, pendingCheck]);

  return (
    <div className="panel-wood panel flex h-full flex-col">
      <div className="flex items-center gap-3 border-b border-gold/15 p-4">
        <div className="relative">
          {dmAvatar ? (
            <img
              src={dmAvatar}
              alt=""
              className="h-11 w-11 rounded-full border-2 border-gold object-cover"
            />
          ) : (
            <span className="title-display flex h-11 w-11 items-center justify-center rounded-full border-2 border-gold bg-black/40 text-lg font-bold text-gold">
              DM
            </span>
          )}
          {dmThinking && (
            <motion.span
              className="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full bg-arcane-soft"
              animate={{ scale: [1, 1.4, 1], opacity: [0.6, 1, 0.6] }}
              transition={{ duration: 1.2, repeat: Infinity }}
            />
          )}
        </div>
        <div className="min-w-0 flex-1">
          <p className="title-display truncate text-sm text-parchment">{dmName}</p>
          <p className="text-[11px] text-muted">
            {dmThinking ? "Tejiendo la historia..." : "Narrador de la partida"}
          </p>
        </div>
        {providerId && <Badge tone="gold">{providerId}</Badge>}
      </div>

      <div ref={scrollRef} className="flex-1 space-y-3 overflow-y-auto p-4">
        {events.length === 0 && (
          <div className="rounded-lg border border-[#3b2a80]/40 bg-[#7C5CFF]/5 p-3">
            <p className="text-[15px] leading-relaxed text-ink">
              {narration ?? "El Dungeon Master aguarda vuestro primer movimiento."}
            </p>
          </div>
        )}

        <div>
          <p className="mb-2 flex items-center gap-1.5 text-[11px] uppercase tracking-wider text-muted">
            <ScrollText className="h-3.5 w-3.5" /> Registro
          </p>
          <ul className="space-y-1.5">
            {events.slice(-20).map((event) => {
              if (event.type === "PLAYER_ACTION") {
                return (
                  <motion.li
                    key={event.id}
                    initial={{ opacity: 0, x: 8 }}
                    animate={{ opacity: 1, x: 0 }}
                    className="ml-6 rounded-lg border border-[#c9a227]/40 bg-[#c9a227]/5 p-3"
                  >
                    <p className="mb-1 flex items-center gap-1.5 text-[10px] uppercase tracking-wider text-[#e0b84a]">
                      <User className="h-3 w-3" /> {event.actor ?? "Tú"}
                    </p>
                    <p className="text-[15px] leading-relaxed text-ink">{event.message}</p>
                  </motion.li>
                );
              }
              return event.type === "NARRATION" ? (
                <motion.li
                  key={event.id}
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="rounded-lg border border-[#3b2a80]/40 bg-[#7C5CFF]/5 p-3"
                >
                  <p className="mb-1 flex items-center gap-1.5 text-[10px] uppercase tracking-wider text-[#a88cff]">
                    <BookOpen className="h-3 w-3" /> Dungeon Master
                  </p>
                  <p className="text-[15px] leading-relaxed text-ink">{event.message}</p>
                </motion.li>
              ) : (
                <motion.li
                  key={event.id}
                  initial={{ opacity: 0, x: -8 }}
                  animate={{ opacity: 1, x: 0 }}
                  className="flex gap-2 rounded-md bg-black/20 px-2.5 py-1.5 text-xs leading-relaxed"
                >
                  <span
                    className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full"
                    style={{ backgroundColor: EVENT_TONE[event.type] ?? "#9E9485" }}
                  />
                  <span className="text-muted">
                    {event.actor && <span className="text-parchment">{event.actor}: </span>}
                    {event.message}
                  </span>
                </motion.li>
              );
            })}
            {events.length === 0 && <li className="text-xs text-muted">El registro aún está en blanco.</li>}
          </ul>
        </div>

        {pendingCheck && (
          <motion.div
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            className="rounded-lg border border-gold/40 bg-gold/10 p-3"
          >
            <p className="mb-1 flex items-center gap-1.5 text-[11px] uppercase tracking-wider text-gold-soft">
              <Sparkles className="h-3.5 w-3.5" /> Prueba solicitada
            </p>
            <p className="text-sm text-ink">
              {pendingCheck.prompt ?? `Prueba de ${pendingCheck.skill}`}{" "}
              <span className="text-gold-soft">· CD {pendingCheck.dc}</span>
            </p>
            <Button
              variant="primary"
              size="sm"
              className="mt-2 w-full"
              onClick={() => onRollCheck(pendingCheck.skill, pendingCheck.dc)}
            >
              <Dices className="h-4 w-4" /> Tirar dado
            </Button>
          </motion.div>
        )}
      </div>

      <form
        className="border-t border-gold/15 p-3"
        onSubmit={(event) => {
          event.preventDefault();
          const value = text.trim();
          if (!value || busy || pendingCheck) return;
          onSay(value);
          setText("");
        }}
      >
        <p className="mb-1.5 text-[11px] text-muted">
          {pendingCheck ? (
            <span className="text-gold-soft">Resuelve la prueba antes de actuar.</span>
          ) : spotlightName ? (
            <>
              Dile al Dungeon Master qué hace <span className="text-parchment">{spotlightName}</span>:
            </>
          ) : (
            "Dile al Dungeon Master qué hacéis:"
          )}
        </p>
        <div className="flex items-end gap-2">
          <textarea
            value={text}
            onChange={(event) => setText(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                const value = text.trim();
                if (!value || busy || pendingCheck) return;
                onSay(value);
                setText("");
              }
            }}
            rows={2}
            disabled={busy || Boolean(pendingCheck)}
            placeholder="Ej: registro el altar en busca de trampas, o le pregunto al espectro por el relicario..."
            className="min-h-[2.75rem] flex-1 resize-none rounded-md border border-parchment/15 bg-black/40 px-3 py-2 text-sm text-ink outline-none focus:border-gold/50 disabled:opacity-50"
          />
          <Button type="submit" variant="primary" size="sm" loading={busy} disabled={!text.trim() || Boolean(pendingCheck)}>
            Enviar
          </Button>
        </div>
      </form>
    </div>
  );
}
