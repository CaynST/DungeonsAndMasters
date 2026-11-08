import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface EmptyStateProps {
  /** Lucide icon shown inside the brass medallion. */
  icon: LucideIcon;
  title: string;
  description?: string;
  /** Optional call to action; keep it to one primary button per state. */
  action?: ReactNode;
  /** Tighter paddings for empty slots inside an existing panel. */
  compact?: boolean;
  className?: string;
}

/**
 * Shared "nothing here yet" panel. It keeps empty states warm and intentional instead of
 * leaving a blank grid: a brass medallion, a short title and an optional next step.
 */
export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  compact = false,
  className,
}: EmptyStateProps) {
  return (
    <div
      role="status"
      className={cn(
        "flex flex-col items-center justify-center rounded-lg border border-dashed border-gold/25 bg-black/20 text-center",
        compact ? "gap-2 px-4 py-6" : "gap-3 px-6 py-10",
        className,
      )}
    >
      <span className="flex h-11 w-11 items-center justify-center rounded-full border border-gold/25 bg-gold/10 text-gold shadow-glow">
        <Icon className="h-5 w-5" aria-hidden="true" />
      </span>
      <div className="space-y-1">
        <p className="title-display text-sm font-semibold text-parchment">{title}</p>
        {description && (
          <p className="mx-auto max-w-md text-xs leading-relaxed text-muted">{description}</p>
        )}
      </div>
      {action && <div className="mt-1">{action}</div>}
    </div>
  );
}
