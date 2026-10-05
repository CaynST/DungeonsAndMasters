import { useMemo } from "react";
import { motion } from "framer-motion";
import type { Cell, GameEntity, GameMap } from "@/types/game";
import { cn } from "@/lib/utils";
import { useGameStore } from "@/store/useGameStore";

const ICONS: Record<string, string> = {
  torch: "🔥",
  bones: "🦴",
  rune: "✦",
  skull: "☠",
  shield: "🛡",
  crosshair: "🎯",
  sword: "⚔",
  sparkles: "✨",
  dagger: "🗡",
  gem: "💎",
  key: "🗝",
  ghost: "👻",
  flask: "🧪",
};

interface BoardProps {
  map: GameMap;
  entities: GameEntity[];
  activeEntityId?: number | null;
  reachablePaths: Map<string, Cell[]>;
  selectedEntityId: number | null;
  targetableEntityIds: Set<number>;
  onCellClick: (cell: Cell) => void;
  onEntityClick: (entity: GameEntity) => void;
}

export function Board({
  map,
  entities,
  activeEntityId,
  reachablePaths,
  selectedEntityId,
  targetableEntityIds,
  onCellClick,
  onEntityClick,
}: BoardProps) {
  const padding = 1;
  const cell = map.tileSize || 48;
  const width = (map.width + padding * 2) * cell;
  const height = (map.height + padding * 2) * cell;
  const offset = padding * cell;

  const walls = useMemo(() => new Set(map.walls.map(key)), [map.walls]);
  const obstacles = useMemo(() => new Set(map.obstacles.map(key)), [map.obstacles]);
  const doors = useMemo(() => new Map(map.doors.map((door) => [key(door), door])), [map.doors]);

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className="h-full w-full select-none"
      style={{ background: "radial-gradient(circle at 50% 30%, #1b1611, #0d0b09)" }}
    >
      <defs>
        <pattern id="grid" width={cell} height={cell} patternUnits="userSpaceOnUse">
          <path d={`M ${cell} 0 L 0 0 0 ${cell}`} fill="none" stroke="rgba(216,195,154,0.06)" strokeWidth={1} />
        </pattern>
      </defs>

      <g transform={`translate(${offset}, ${offset})`}>
        <rect width={map.width * cell} height={map.height * cell} fill="url(#grid)" />

        {/* Floor tiles */}
        {Array.from({ length: map.height }).map((_, y) =>
          Array.from({ length: map.width }).map((__, x) => {
            const k = `${x},${y}`;
            const isWall = walls.has(k);
            const isObstacle = obstacles.has(k);
            const door = doors.get(k);
            const reachable = reachablePaths.has(k);
            return (
              <g key={k} onClick={() => !isWall && onCellClick({ x, y })} className={!isWall ? "cursor-pointer" : ""}>
                <rect
                  x={x * cell}
                  y={y * cell}
                  width={cell}
                  height={cell}
                  fill={
                    isWall
                      ? "#241a12"
                      : isObstacle
                        ? "#2c2117"
                        : reachable
                          ? "rgba(62,156,110,0.22)"
                          : "rgba(42,29,20,0.35)"
                  }
                  stroke="rgba(216,195,154,0.05)"
                />
                {isWall && (
                  <rect x={x * cell + 2} y={y * cell + 2} width={cell - 4} height={cell - 4} fill="#2f2519" rx={3} />
                )}
                {isObstacle && (
                  <rect x={x * cell + 6} y={y * cell + 6} width={cell - 12} height={cell - 12} fill="#3a2c1e" rx={4} />
                )}
                {door && (
                  <rect
                    x={x * cell + 4}
                    y={y * cell + 4}
                    width={cell - 8}
                    height={cell - 8}
                    rx={4}
                    fill={door.open ? "rgba(201,162,39,0.25)" : "#4a3a20"}
                    stroke="#C9A227"
                    strokeWidth={1.5}
                  />
                )}
              </g>
            );
          }),
        )}

        {/* Decor */}
        {map.decor.map((decor, index) => (
          <text
            key={`decor-${index}`}
            x={decor.x * cell + cell / 2}
            y={decor.y * cell + cell / 2 + 4}
            textAnchor="middle"
            fontSize={cell * 0.5}
            opacity={0.5}
          >
            {ICONS[decor.icon] ?? "✦"}
          </text>
        ))}

        {/* Entities */}
        {entities
          .filter((entity) => entity.alive || entity.kind !== "ENEMY")
          .map((entity) => (
            <Token
              key={entity.id}
              entity={entity}
              cell={cell}
              selected={selectedEntityId === entity.id}
              active={activeEntityId === entity.id}
              targetable={targetableEntityIds.has(entity.id)}
              onClick={() => onEntityClick(entity)}
            />
          ))}
      </g>
    </svg>
  );
}

function Token({
  entity,
  cell,
  selected,
  active,
  targetable,
  onClick,
}: {
  entity: GameEntity;
  cell: number;
  selected: boolean;
  active: boolean;
  targetable: boolean;
  onClick: () => void;
}) {
  const floating = useGameStore((state) => state.floatingTexts.filter((f) => f.entityId === entity.id));
  const dead = !entity.alive;
  const x = entity.x * cell + cell / 2;
  const y = entity.y * cell + cell / 2;

  return (
    <motion.g
      onClick={(event) => {
        event.stopPropagation();
        onClick();
      }}
      initial={{ opacity: 0, scale: 0.6 }}
      animate={{ opacity: dead ? 0.25 : 1, scale: dead ? 0.7 : 1 }}
      transition={{ type: "spring", stiffness: 260, damping: 20 }}
      className={cn("cursor-pointer", targetable && "animate-float")}
    >
      {(active || selected || targetable) && (
        <motion.circle
          cx={x}
          cy={y}
          r={cell * 0.46}
          fill="none"
          stroke={targetable ? "#B0364B" : active ? "#C9A227" : "#E8E1D5"}
          strokeWidth={2.5}
          strokeDasharray={targetable ? "5 4" : undefined}
          animate={{ opacity: [0.5, 1, 0.5] }}
          transition={{ duration: 1.6, repeat: Infinity }}
        />
      )}
      <circle cx={x} cy={y} r={cell * 0.34} fill={entity.color ?? "#C9A227"} opacity={dead ? 0.4 : 1} />
      <circle cx={x} cy={y} r={cell * 0.34} fill="none" stroke="rgba(0,0,0,0.5)" strokeWidth={2} />
      <text x={x} y={y + 4} textAnchor="middle" fontSize={cell * 0.32} fill="#0D0B09" fontWeight={700}>
        {entity.name.charAt(0).toUpperCase()}
      </text>

      {entity.kind !== "ITEM" && entity.maxHp > 1 && (
        <g>
          <rect x={x - cell * 0.36} y={y + cell * 0.36} width={cell * 0.72} height={5} rx={2} fill="rgba(0,0,0,0.6)" />
          <rect
            x={x - cell * 0.36}
            y={y + cell * 0.36}
            width={cell * 0.72 * Math.max(0, entity.hp / entity.maxHp)}
            height={5}
            rx={2}
            fill={entity.kind === "ENEMY" ? "#B0364B" : "#3E9C6E"}
          />
        </g>
      )}

      {floating.map((text) => (
        <motion.text
          key={text.id}
          x={x}
          y={y - cell * 0.4}
          textAnchor="middle"
          fontSize={cell * 0.4}
          fontWeight={800}
          fill={text.kind === "damage" ? "#E06A7A" : "#6FD3A0"}
          initial={{ opacity: 1, y: y - cell * 0.4 }}
          animate={{ opacity: 0, y: y - cell * 1.1 }}
          transition={{ duration: 1.3 }}
        >
          {text.text}
        </motion.text>
      ))}
    </motion.g>
  );
}

function key(cell: Cell) {
  return `${cell.x},${cell.y}`;
}
