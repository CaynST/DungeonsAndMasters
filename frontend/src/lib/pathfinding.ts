import type { Cell, GameMap } from "@/types/game";

const DIRS: [number, number][] = [
  [-1, -1], [0, -1], [1, -1],
  [-1, 0], [1, 0],
  [-1, 1], [0, 1], [1, 1],
];

export function cellKey(cell: Cell): string {
  return `${cell.x},${cell.y}`;
}

function blocked(map: GameMap, cell: Cell, occupied: Set<string>): boolean {
  if (cell.x < 0 || cell.y < 0 || cell.x >= map.width || cell.y >= map.height) return true;
  const k = cellKey(cell);
  if (map.walls.some((w) => cellKey(w) === k)) return true;
  if (map.obstacles.some((o) => cellKey(o) === k)) return true;
  if (map.doors.some((d) => cellKey(d) === k && !d.open)) return true;
  if (occupied.has(k)) return true;
  return false;
}

/**
 * Breadth-first search over the grid (8 directions, 1 cost per step — matching the
 * backend). Returns the full path from start to goal, or null if unreachable.
 */
export function findPath(
  map: GameMap,
  start: Cell,
  goal: Cell,
  occupied: Set<string>,
): Cell[] | null {
  const startKey = cellKey(start);
  const goalKey = cellKey(goal);
  if (startKey === goalKey) return [];
  if (blocked(map, goal, occupied)) return null;

  const frontier: Cell[] = [start];
  const cameFrom = new Map<string, string>();
  const cells = new Map<string, Cell>([[startKey, start]]);
  cameFrom.set(startKey, startKey);

  while (frontier.length > 0) {
    const current = frontier.shift()!;
    for (const [dx, dy] of DIRS) {
      const next = { x: current.x + dx, y: current.y + dy };
      const key = cellKey(next);
      if (cameFrom.has(key) || blocked(map, next, occupied)) continue;
      cameFrom.set(key, cellKey(current));
      cells.set(key, next);
      if (key === goalKey) {
        return reconstruct(cameFrom, cells, startKey, goalKey);
      }
      frontier.push(next);
    }
  }
  return null;
}

/** All cells reachable within {@code maxCost} steps, each mapped to its path. */
export function reachableCells(
  map: GameMap,
  start: Cell,
  occupied: Set<string>,
  maxCost: number,
): Map<string, Cell[]> {
  const startKey = cellKey(start);
  const cameFrom = new Map<string, string>([[startKey, startKey]]);
  const cells = new Map<string, Cell>([[startKey, start]]);
  const cost = new Map<string, number>([[startKey, 0]]);
  const result = new Map<string, Cell[]>();
  const frontier: Cell[] = [start];

  while (frontier.length > 0) {
    const current = frontier.shift()!;
    const currentCost = cost.get(cellKey(current))!;
    if (currentCost >= maxCost) continue;
    for (const [dx, dy] of DIRS) {
      const next = { x: current.x + dx, y: current.y + dy };
      const key = cellKey(next);
      if (cameFrom.has(key) || blocked(map, next, occupied)) continue;
      cameFrom.set(key, cellKey(current));
      cells.set(key, next);
      cost.set(key, currentCost + 1);
      result.set(key, reconstruct(cameFrom, cells, startKey, key));
      frontier.push(next);
    }
  }
  return result;
}

function reconstruct(
  cameFrom: Map<string, string>,
  cells: Map<string, Cell>,
  startKey: string,
  goalKey: string,
): Cell[] {
  const path: Cell[] = [];
  let key = goalKey;
  while (key !== startKey) {
    const cell = cells.get(key);
    if (!cell) break;
    path.unshift(cell);
    key = cameFrom.get(key) ?? startKey;
  }
  return path;
}
