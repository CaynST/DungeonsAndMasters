import { cn } from "@/lib/utils";

/**
 * Shimmering placeholder for loading states. Decorative by default; wrap the surrounding
 * region with `aria-busy` and an `sr-only` label so the wait is announced once, not per block.
 */
export function Skeleton({ className }: { className?: string }) {
  return <div aria-hidden="true" className={cn("animate-shimmer rounded-md bg-parchment/10", className)} />;
}
