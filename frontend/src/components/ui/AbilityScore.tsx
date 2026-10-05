import { cn } from "@/lib/utils";

export function AbilityScore({
  label,
  value,
  modifier,
  className,
}: {
  label: string;
  value: number;
  modifier: number;
  className?: string;
}) {
  const mod = modifier >= 0 ? `+${modifier}` : `${modifier}`;
  return (
    <div
      className={cn(
        "flex flex-col items-center rounded-lg border border-parchment/10 bg-black/30 px-2 py-1.5",
        className,
      )}
    >
      <span className="text-[10px] font-semibold uppercase tracking-widest text-muted">{label}</span>
      <span className="title-display text-lg leading-none text-ink">{value}</span>
      <span className="text-[11px] text-gold-soft">{mod}</span>
    </div>
  );
}
