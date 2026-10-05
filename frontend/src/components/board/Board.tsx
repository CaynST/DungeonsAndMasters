import { useMemo } from "react";
import { motion } from "framer-motion";
import type { Cell, GameEntity, GameMap } from "@/types/game";
import { cn } from "@/lib/utils";
import { useGameStore } from "@/store/useGameStore";

const DECOR_ICONS: Record<string, string> = {
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
  tree: "🌲",
  rock: "🪨",
  rubble: "🧱",
  barrel: "🛢",
  reed: "🌾",
  club: "🏏",
  paw: "🐾",
  bug: "🕷",
  flame: "🔥",
};

interface Theme {
  floorA: string;
  floorB: string;
  wall: string;
  wallEdge: string;
  obstacle: string;
  accent: string;
  fog: string;
}

const THEMES: Record<string, Theme> = {
  crypt: { floorA: "#1a140f", floorB: "#1d1611", wall: "#241a12", wallEdge: "#33261a", obstacle: "#3a2c1e", accent: "#c9a227", fog: "#0c0a08" },
  forest: { floorA: "#141a12", floorB: "#171f14", wall: "#1e2a1a", wallEdge: "#2c3d26", obstacle: "#24361f", accent: "#7bbf6a", fog: "#0a0f09" },
  cave: { floorA: "#16130f", floorB: "#1a1611", wall: "#241f19", wallEdge: "#342c22", obstacle: "#3a3228", accent: "#a88cff", fog: "#0b0908" },
  ruins: { floorA: "#1a1712", floorB: "#1e1a14", wall: "#2a241b", wallEdge: "#3a3225", obstacle: "#3a3225", accent: "#d8c39a", fog: "#0c0b08" },
  dungeon: { floorA: "#161210", floorB: "#1a1512", wall: "#221a13", wallEdge: "#33261a", obstacle: "#33261a", accent: "#c9a227", fog: "#0b0908" },
  swamp: { floorA: "#121710", floorB: "#151c12", wall: "#1c2416", wallEdge: "#2a3620", obstacle: "#24301c", accent: "#6fd3a0", fog: "#090d08" },
};

function themeFor(name?: string): Theme {
  return THEMES[name ?? "crypt"] ?? THEMES.crypt;
}

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
  const theme = themeFor(map.theme);

  const uniqueWalls = useMemo(() => dedupe(map.walls), [map.walls]);
  const uniqueObstacles = useMemo(() => dedupe(map.obstacles), [map.obstacles]);
  const walls = useMemo(() => new Set(uniqueWalls.map(key)), [uniqueWalls]);
  const obstacles = useMemo(() => new Set(uniqueObstacles.map(key)), [uniqueObstacles]);
  const doors = useMemo(() => new Map(map.doors.map((door) => [key(door), door])), [map.doors]);

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className="h-full w-full select-none"
      style={{ background: theme.fog }}
    >
      <defs>
        <pattern id="board-grid" width={cell} height={cell} patternUnits="userSpaceOnUse">
          <path d={`M ${cell} 0 L 0 0 0 ${cell}`} fill="none" stroke="rgba(255,255,255,0.035)" strokeWidth={1} />
        </pattern>
        <radialGradient id="torch-glow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor={theme.accent} stopOpacity="0.55" />
          <stop offset="100%" stopColor={theme.accent} stopOpacity="0" />
        </radialGradient>
        <radialGradient id="board-vignette" cx="50%" cy="45%" r="72%">
          <stop offset="55%" stopColor="#000" stopOpacity="0" />
          <stop offset="100%" stopColor={theme.fog} stopOpacity="0.85" />
        </radialGradient>
        <linearGradient id="wall-face" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={theme.wallEdge} />
          <stop offset="100%" stopColor={theme.wall} />
        </linearGradient>
      </defs>

      <g transform={`translate(${offset}, ${offset})`}>
        {/* Floor + grid */}
        <rect width={map.width * cell} height={map.height * cell} fill={theme.floorA} />
        {Array.from({ length: map.height }).map((_, y) =>
          Array.from({ length: map.width }).map((__, x) =>
            (x + y) % 2 === 0 ? (
              <rect key={`f-${x}-${y}`} x={x * cell} y={y * cell} width={cell} height={cell} fill={theme.floorB} />
            ) : null,
          ),
        )}
        <rect width={map.width * cell} height={map.height * cell} fill="url(#board-grid)" />

        {/* Reachable highlight */}
        {Array.from(reachablePaths.keys()).map((k) => {
          const [x, y] = k.split(",").map(Number);
          return (
            <rect
              key={`r-${k}`}
              x={x * cell + 3}
              y={y * cell + 3}
              width={cell - 6}
              height={cell - 6}
              rx={4}
              fill="rgba(111,211,160,0.16)"
              stroke="rgba(111,211,160,0.45)"
              strokeWidth={1}
            />
          );
        })}

        {/* Interactive floor cells (clickable) */}
        {Array.from({ length: map.height }).map((_, y) =>
          Array.from({ length: map.width }).map((__, x) => {
            const k = `${x},${y}`;
            if (walls.has(k)) return null;
            return (
              <rect
                key={`c-${k}`}
                x={x * cell}
                y={y * cell}
                width={cell}
                height={cell}
                fill="transparent"
                className="cursor-pointer"
                onClick={() => onCellClick({ x, y })}
              />
            );
          }),
        )}

        {/* Walls with depth */}
        {uniqueWalls.map((wall) => (
          <g key={`w-${key(wall)}`}>
            <rect x={wall.x * cell} y={wall.y * cell} width={cell} height={cell} fill="url(#wall-face)" />
            <rect x={wall.x * cell} y={wall.y * cell} width={cell} height={4} fill={theme.wallEdge} opacity={0.9} />
            <rect
              x={wall.x * cell + 2}
              y={wall.y * cell + 6}
              width={cell - 4}
              height={cell - 8}
              rx={3}
              fill="rgba(0,0,0,0.22)"
            />
          </g>
        ))}

        {/* Obstacles */}
        {uniqueObstacles.map((obstacle) => (
          <g key={`o-${key(obstacle)}`}>
            <rect
              x={obstacle.x * cell + 4}
              y={obstacle.y * cell + 4}
              width={cell - 8}
              height={cell - 8}
              rx={5}
              fill={theme.obstacle}
              stroke="rgba(0,0,0,0.35)"
            />
            <rect
              x={obstacle.x * cell + 7}
              y={obstacle.y * cell + 7}
              width={cell - 14}
              height={cell - 18}
              rx={3}
              fill="rgba(255,255,255,0.05)"
            />
          </g>
        ))}

        {/* Doors */}
        {map.doors.map((door, index) => (
          <rect
            key={`d-${index}`}
            x={door.x * cell + 4}
            y={door.y * cell + 4}
            width={cell - 8}
            height={cell - 8}
            rx={4}
            fill={door.open ? "rgba(201,162,39,0.18)" : theme.wallEdge}
            stroke={theme.accent}
            strokeWidth={1.5}
          />
        ))}

        {/* Decor with torch glow */}
        {map.decor.map((decor, index) => (
          <g key={`decor-${index}`}>
            {decor.icon === "torch" && (
              <circle
                className="torch-glow"
                cx={decor.x * cell + cell / 2}
                cy={decor.y * cell + cell / 2}
                r={cell * 1.6}
                fill="url(#torch-glow)"
              />
            )}
            <text
              x={decor.x * cell + cell / 2}
              y={decor.y * cell + cell / 2 + 5}
              textAnchor="middle"
              fontSize={cell * 0.5}
              opacity={0.7}
            >
              {DECOR_ICONS[decor.icon] ?? "✦"}
            </text>
          </g>
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
              accent={theme.accent}
              onClick={() => onEntityClick(entity)}
            />
          ))}

        {/* Vignette */}
        <rect width={map.width * cell} height={map.height * cell} fill="url(#board-vignette)" pointerEvents="none" />
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
  accent,
  onClick,
}: {
  entity: GameEntity;
  cell: number;
  selected: boolean;
  active: boolean;
  targetable: boolean;
  accent: string;
  onClick: () => void;
}) {
  const floatingTexts = useGameStore((state) => state.floatingTexts);
  const floating = floatingTexts.filter((f) => f.entityId === entity.id);
  const dead = !entity.alive;
  const x = entity.x * cell + cell / 2;
  const y = entity.y * cell + cell / 2;
  const color = entity.color ?? accent;

  return (
    <motion.g
      onClick={(event) => {
        event.stopPropagation();
        onClick();
      }}
      initial={{ opacity: 0, scale: 0.6 }}
      animate={{ opacity: dead ? 0.28 : 1, scale: dead ? 0.75 : 1 }}
      transition={{ type: "spring", stiffness: 260, damping: 20 }}
      className={cn("cursor-pointer", targetable && "animate-float")}
    >
      {(active || selected || targetable) && (
        <motion.circle
          cx={x}
          cy={y}
          r={cell * 0.46}
          fill="none"
          stroke={targetable ? "#d0566a" : active ? accent : "#e8e1d5"}
          strokeWidth={2.5}
          strokeDasharray={targetable ? "5 4" : undefined}
          animate={{ opacity: [0.5, 1, 0.5] }}
          transition={{ duration: 1.6, repeat: Infinity }}
        />
      )}
      <circle cx={x} cy={y} r={cell * 0.36} fill="#0c0a08" />
      <circle cx={x} cy={y} r={cell * 0.33} fill={color} />
      <circle cx={x} cy={y} r={cell * 0.33} fill="none" stroke="rgba(0,0,0,0.55)" strokeWidth={2} />
      <circle cx={x - cell * 0.1} cy={y - cell * 0.1} r={cell * 0.12} fill="rgba(255,255,255,0.18)" />
      <text x={x} y={y + 4} textAnchor="middle" fontSize={cell * 0.3} fill="#0c0a08" fontWeight={800}>
        {entity.name.charAt(0).toUpperCase()}
      </text>

      {entity.kind !== "ITEM" && entity.maxHp > 1 && (
        <g>
          <rect x={x - cell * 0.36} y={y + cell * 0.38} width={cell * 0.72} height={5} rx={2} fill="rgba(0,0,0,0.65)" />
          <rect
            x={x - cell * 0.36}
            y={y + cell * 0.38}
            width={cell * 0.72 * Math.max(0, entity.hp / entity.maxHp)}
            height={5}
            rx={2}
            fill={entity.kind === "ENEMY" ? "#d0566a" : "#6fd3a0"}
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
          fill={text.kind === "damage" ? "#e06a7a" : "#6fd3a0"}
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

/** Removes duplicate cells, keeping first occurrence (defends against bad persisted maps). */
function dedupe(cells: Cell[]): Cell[] {
  const seen = new Set<string>();
  const result: Cell[] = [];
  for (const cell of cells) {
    const k = key(cell);
    if (!seen.has(k)) {
      seen.add(k);
      result.push(cell);
    }
  }
  return result;
}
