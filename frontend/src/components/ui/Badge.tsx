import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

type Tone = "gold" | "crimson" | "emerald" | "arcane" | "muted";

const tones: Record<Tone, string> = {
  gold: "bg-gold/15 text-gold-soft border-gold/30",
  crimson: "bg-crimson/15 text-crimson-soft border-crimson/30",
  emerald: "bg-emerald/15 text-emerald-soft border-emerald/30",
  arcane: "bg-arcane/15 text-arcane-soft border-arcane/30",
  muted: "bg-white/5 text-muted border-white/10",
};

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: Tone;
}

export function Badge({ className, tone = "muted", ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[11px] font-medium uppercase tracking-wider",
        tones[tone],
        className,
      )}
      {...props}
    />
  );
}
