import { useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { SkipForward } from "lucide-react";
import type { CampaignIntro } from "@/types/game";

interface Props {
  intro: CampaignIntro;
  onDone: () => void;
}

/**
 * Opening cinematic: "érase una vez" premise lines, then the party, then the epic title.
 * Click or the skip button jumps straight to the game.
 */
export function CinematicIntro({ intro, onDone }: Props) {
  const steps = useMemo(() => {
    const list: string[] = [...(intro.premise.length ? intro.premise : ["Érase una vez..."])];
    list.push("__party__");
    list.push("__title__");
    return list;
  }, [intro.premise]);

  const [step, setStep] = useState(0);
  const current = steps[step];

  useEffect(() => {
    const delay = current === "__title__" ? 4600 : current === "__party__" ? 3600 : 2800;
    const timer = setTimeout(() => {
      if (step < steps.length - 1) {
        setStep((value) => value + 1);
      } else {
        onDone();
      }
    }, delay);
    return () => clearTimeout(timer);
  }, [step, steps.length, current, onDone]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#070604]">
      <button
        onClick={onDone}
        className="absolute right-5 top-5 inline-flex items-center gap-2 rounded-md border border-parchment/15 px-3 py-1.5 text-xs text-muted transition hover:text-ink"
      >
        <SkipForward className="h-3.5 w-3.5" /> Saltar
      </button>

      <div className="w-full max-w-3xl px-8 text-center">
        <AnimatePresence mode="wait">
          {current !== "__party__" && current !== "__title__" && (
            <motion.p
              key={`premise-${step}`}
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -14 }}
              transition={{ duration: 0.7 }}
              className="mx-auto max-w-2xl text-balance text-2xl font-light italic leading-relaxed text-[#d8c9a8] md:text-3xl"
            >
              {current}
            </motion.p>
          )}

          {current === "__party__" && (
            <motion.div
              key="party"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.6 }}
            >
              <p className="mb-6 text-xs uppercase tracking-[0.4em] text-[#8a7f6d]">La compañía</p>
              <div className="flex flex-wrap items-start justify-center gap-6">
                {intro.party.map((member, index) => (
                  <motion.div
                    key={member.name}
                    initial={{ opacity: 0, y: 16 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.35 }}
                    className="w-36"
                  >
                    <span
                      className="title-display mx-auto flex h-20 w-20 items-center justify-center rounded-full border-2 text-3xl font-bold"
                      style={{ borderColor: member.color ?? "#c9a227", color: member.color ?? "#c9a227" }}
                    >
                      {member.name.charAt(0)}
                    </span>
                    <p className="mt-3 font-semibold text-[#e8e1d5]">{member.name}</p>
                    <p className="text-xs uppercase tracking-wider text-[#8a7f6d]">
                      {member.race} · {member.cls}
                    </p>
                  </motion.div>
                ))}
                {intro.party.length === 0 && (
                  <p className="text-sm text-muted">Un grupo de aventureros sin nombre...</p>
                )}
              </div>
            </motion.div>
          )}

          {current === "__title__" && (
            <motion.div
              key="title"
              initial={{ opacity: 0, scale: 0.92 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 1.04 }}
              transition={{ duration: 1.1 }}
            >
              <motion.h1
                initial={{ letterSpacing: "0.4em", opacity: 0 }}
                animate={{ letterSpacing: "0.06em", opacity: 1 }}
                transition={{ duration: 1.4 }}
                className="epic-title text-4xl font-black uppercase text-[#e0b84a] text-glow-gold md:text-6xl"
              >
                {intro.title}
              </motion.h1>
              {intro.villain && (
                <motion.p
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 1.2 }}
                  className="mt-5 text-sm uppercase tracking-[0.3em] text-[#8a7f6d]"
                >
                  Contra {intro.villain}
                </motion.p>
              )}
              {intro.objective && (
                <motion.p
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 1.6 }}
                  className="mx-auto mt-3 max-w-xl text-sm text-[#c8bda7]"
                >
                  {intro.objective}
                </motion.p>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
