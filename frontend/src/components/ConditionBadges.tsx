import type { GameEntity } from "@/types/game";
import { entityConditions, type ActiveConditionView } from "@/lib/conditions";
import { Badge } from "@/components/ui/Badge";
import { cn } from "@/lib/utils";

interface Props {
  entity?: GameEntity | null;
  conditions?: ActiveConditionView[];
  compact?: boolean;
  className?: string;
}

/** Compact badges for a creature's active conditions. */
export function ConditionBadges({ entity, conditions, compact, className }: Props) {
  const list = conditions ?? entityConditions(entity);
  if (list.length === 0) return null;
  return (
    <div className={cn("flex flex-wrap gap-1", className)}>
      {list.map((condition) => (
        <Badge
          key={condition.code}
          tone={condition.tone}
          title={condition.description}
          className={cn("normal-case tracking-normal", compact && "px-1.5 py-0 text-[10px]")}
        >
          {condition.label}
          {condition.code === "EXHAUSTION" && condition.level ? ` ${condition.level}` : ""}
          {condition.rounds != null ? ` · ${condition.rounds}` : ""}
        </Badge>
      ))}
    </div>
  );
}
