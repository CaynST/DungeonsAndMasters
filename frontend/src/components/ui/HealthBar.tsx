import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { cn } from "@/lib/utils";

interface Props {
  value: number;
  max: number;
  className?: string;
  showLabel?: boolean;
}

export function HealthBar({ value, max, className, showLabel = true }: Props) {
  const pct = max <= 0 ? 0 : Math.max(0, Math.min(100, (value / max) * 100));
  const tone = pct > 60 ? "#3E9C6E" : pct > 30 ? "#C9A227" : "#B0364B";
  return (
    <div className={cn("w-full", className)}>
      {showLabel && (
        <div className="mb-1 flex items-center justify-between text-[11px] text-muted">
          <span>HP</span>
          <span className="font-medium text-ink">
            {value} / {max}
          </span>
        </div>
      )}
      <div className="h-2 w-full overflow-hidden rounded-full bg-black/50">
        <div className="hp-bar h-full rounded-full" style={{ width: `${pct}%`, backgroundColor: tone }} />
      </div>
    </div>
  );
}

// Keep a tiny helper exported so components can prefetch characters.
export function useCharactersQuery() {
  return useQuery({ queryKey: ["characters"], queryFn: api.characters });
}
