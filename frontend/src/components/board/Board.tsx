import { useMemo } from "react";
import { motion } from "framer-motion";
import type { Cell, Decor, GameEntity, GameMap } from "@/types/game";
import { entityConditions } from "@/lib/conditions";
import { cn } from "@/lib/utils";
import { useGameStore } from "@/store/useGameStore";

/** Location decor drawn as vector art (no emoji). */
function DecorShape({ decor, cell, accent }: { decor: Decor; cell: number; accent: string }) {
  const cx = decor.x * cell + cell / 2;
  const cy = decor.y * cell + cell / 2;
  const s = cell * 0.5;
  switch (decor.kind) {
    case "torch":
      return (
        <g>
          <circle className="torch-glow" cx={cx} cy={cy} r={cell * 1.6} fill="url(#torch-glow)" />
          <rect x={cx - s * 0.12} y={cy - s * 0.1} width={s * 0.24} height={s * 0.9} rx={2} fill="#3a2c1e" />
          <ellipse cx={cx} cy={cy - s * 0.25} rx={s * 0.28} ry={s * 0.5} fill="#e0a83a" />
          <ellipse cx={cx} cy={cy - s * 0.3} rx={s * 0.14} ry={s * 0.3} fill="#ffe08a" />
        </g>
      );
    case "bones":
      return (
        <g stroke="#cbb98f" strokeWidth={cell * 0.08} strokeLinecap="round" opacity={0.8}>
          <line x1={cx - s * 0.7} y1={cy - s * 0.5} x2={cx + s * 0.7} y2={cy + s * 0.5} />
          <line x1={cx - s * 0.7} y1={cy + s * 0.5} x2={cx + s * 0.7} y2={cy - s * 0.5} />
        </g>
      );
    case "tree":
      return (
        <g>
          <rect x={cx - s * 0.12} y={cy} width={s * 0.24} height={s * 0.7} fill="#3a2c1e" />
          <polygon points={`${cx},${cy - s} ${cx - s * 0.7},${cy + s * 0.2} ${cx + s * 0.7},${cy + s * 0.2}`} fill="#2f5a2a" />
          <polygon points={`${cx},${cy - s * 0.6} ${cx - s * 0.55},${cy + s * 0.4} ${cx + s * 0.55},${cy + s * 0.4}`} fill="#3c6f34" />
        </g>
      );
    case "rock":
      return (
        <polygon
          points={`${cx - s * 0.7},${cy + s * 0.4} ${cx - s * 0.3},${cy - s * 0.5} ${cx + s * 0.4},${cy - s * 0.4} ${cx + s * 0.7},${cy + s * 0.4}`}
          fill="#4a443c"
        />
      );
    case "barrel":
      return (
        <g>
          <rect x={cx - s * 0.5} y={cy - s * 0.7} width={s} height={s * 1.4} rx={s * 0.2} fill="#5a3f22" />
          <rect x={cx - s * 0.55} y={cy - s * 0.45} width={s * 1.1} height={s * 0.12} fill="#8a6a34" />
          <rect x={cx - s * 0.55} y={cy + s * 0.3} width={s * 1.1} height={s * 0.12} fill="#8a6a34" />
        </g>
      );
    case "rubble":
      return (
        <g fill="#5a5148">
          <polygon points={`${cx - s * 0.7},${cy + s * 0.5} ${cx - s * 0.2},${cy - s * 0.1} ${cx + s * 0.1},${cy + s * 0.5}`} />
          <polygon points={`${cx - s * 0.1},${cy + s * 0.5} ${cx + s * 0.3},${cy - s * 0.4} ${cx + s * 0.7},${cy + s * 0.5}`} />
        </g>
      );
    case "reed":
      return (
        <g stroke="#4f6b3a" strokeWidth={cell * 0.06} strokeLinecap="round">
          <line x1={cx} y1={cy + s * 0.6} x2={cx - s * 0.3} y2={cy - s * 0.6} />
          <line x1={cx} y1={cy + s * 0.6} x2={cx + s * 0.2} y2={cy - s * 0.5} />
        </g>
      );
    default:
      return (
        <g>
          <polygon
            points={`${cx},${cy - s * 0.6} ${cx + s * 0.5},${cy} ${cx},${cy + s * 0.6} ${cx - s * 0.5},${cy}`}
            fill="none"
            stroke={accent}
            strokeWidth={cell * 0.06}
            opacity={0.85}
          />
          <circle cx={cx} cy={cy} r={cell * 0.08} fill={accent} opacity={0.8} />
        </g>
      );
  }
}

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
  desert: { floorA: "#2a2216", floorB: "#2f2619", wall: "#3a2e1c", wallEdge: "#4c3d24", obstacle: "#4a3a22", accent: "#e0b84a", fog: "#171208" },
  mountain: { floorA: "#1b1b1f", floorB: "#1f1f24", wall: "#2a2a30", wallEdge: "#3a3a42", obstacle: "#33333a", accent: "#9bb4d6", fog: "#0c0c10" },
  temple: { floorA: "#1c1710", floorB: "#201a12", wall: "#2e2417", wallEdge: "#463521", obstacle: "#3a2c1a", accent: "#e0c56a", fog: "#0d0a06" },
  sewer: { floorA: "#141813", floorB: "#171c16", wall: "#20261d", wallEdge: "#2f3a29", obstacle: "#28301f", accent: "#8fae6a", fog: "#090c08" },
  ice: { floorA: "#141b22", floorB: "#18212a", wall: "#22303c", wallEdge: "#33475a", obstacle: "#2b3d4c", accent: "#8fd0ff", fog: "#0a0f14" },
  volcano: { floorA: "#1c1210", floorB: "#201512", wall: "#2c1a15", wallEdge: "#44261c", obstacle: "#3a2018", accent: "#ff7a3d", fog: "#0f0705" },
  village: { floorA: "#1a1712", floorB: "#1e1a14", wall: "#2a2218", wallEdge: "#3d3121", obstacle: "#3a2c1c", accent: "#c9a227", fog: "#0c0a07" },
  tavern: { floorA: "#241a10", floorB: "#2a1f13", wall: "#33261a", wallEdge: "#4a3520", obstacle: "#3d2b1a", accent: "#e0b84a", fog: "#0e0a06" },
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
        <pattern id="floor-cracks" width={cell * 2} height={cell * 2} patternUnits="userSpaceOnUse">
          <path d={`M ${cell * 0.2} ${cell * 0.6} L ${cell * 0.7} ${cell * 0.9} L ${cell * 0.5} ${cell * 1.4}`}
            stroke="rgba(0,0,0,0.22)" strokeWidth={1} fill="none" />
          <path d={`M ${cell * 1.3} ${cell * 0.2} L ${cell * 1.5} ${cell * 0.6} L ${cell * 1.1} ${cell * 0.9}`}
            stroke="rgba(0,0,0,0.16)" strokeWidth={1} fill="none" />
          <rect x={cell * 1.2} y={cell * 1.2} width={cell * 0.4} height={cell * 0.25} rx={3}
            fill="rgba(255,255,255,0.02)" stroke="rgba(0,0,0,0.2)" strokeWidth={0.5} />
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
        <rect width={map.width * cell} height={map.height * cell} fill="url(#floor-cracks)" />

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

        {/* Decor drawn as SVG (no emojis) */}
        {map.decor.map((decor, index) => (
          <DecorShape key={`decor-${index}`} decor={decor} cell={cell} accent={theme.accent} />
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
      {entity.portrait ? (
        <>
          <defs>
            <clipPath id={`token-clip-${entity.id}`}>
              <circle cx={x} cy={y} r={cell * 0.33} />
            </clipPath>
          </defs>
          <image
            href={entity.portrait}
            x={x - cell * 0.33}
            y={y - cell * 0.33}
            width={cell * 0.66}
            height={cell * 0.66}
            clipPath={`url(#token-clip-${entity.id})`}
            preserveAspectRatio="xMidYMid slice"
          />
        </>
      ) : (
        <>
          <circle cx={x} cy={y} r={cell * 0.33} fill={color} />
          <text x={x} y={y + 4} textAnchor="middle" fontSize={cell * 0.3} fill="#0c0a08" fontWeight={800}>
            {entity.name.charAt(0).toUpperCase()}
          </text>
        </>
      )}
      <circle
        cx={x}
        cy={y}
        r={cell * 0.33}
        fill="none"
        stroke={entity.kind === "ENEMY" ? "#d0566a" : "rgba(0,0,0,0.6)"}
        strokeWidth={2.5}
      />

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

      <ConditionDots entity={entity} x={x} y={y} cell={cell} />

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

/** Small condition markers around a token, with a native tooltip explaining each one. */
function ConditionDots({ entity, x, y, cell }: { entity: GameEntity; x: number; y: number; cell: number }) {
  const conditions = entityConditions(entity);
  if (conditions.length === 0) return null;
  const visible = conditions.slice(0, 4);
  const spread = (visible.length - 1) / 2;
  return (
    <g>
      {visible.map((condition, index) => {
        const angle = -Math.PI / 2 + (index - spread) * 0.5;
        const cx = x + Math.cos(angle) * cell * 0.42;
        const cy = y + Math.sin(angle) * cell * 0.42;
        return (
          <g key={condition.code}>
            <circle
              cx={cx}
              cy={cy}
              r={cell * 0.07}
              fill={condition.color}
              stroke="#0c0a08"
              strokeWidth={1.5}
            />
            <title>{`${condition.label}: ${condition.description}`}</title>
          </g>
        );
      })}
      {conditions.length > visible.length && (
        <text x={x + cell * 0.38} y={y - cell * 0.28} fontSize={cell * 0.16} fill="#e8e1d5" fontWeight={700}>
          +{conditions.length - visible.length}
        </text>
      )}
    </g>
  );
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
