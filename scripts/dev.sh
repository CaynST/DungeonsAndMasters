#!/usr/bin/env bash
# Starts backend and frontend together. Backend logs go to /tmp; the frontend
# runs in the foreground so Ctrl+C stops everything.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${TMPDIR:-/tmp}/dnd-masters"
mkdir -p "$LOG_DIR"

cleanup() {
  echo ""
  echo "▶ Parando backend..."
  if [ -n "${BACKEND_PID:-}" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill "$BACKEND_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

echo "▶ Arrancando backend (logs en $LOG_DIR/backend.log)..."
( cd "$ROOT/backend" && mvn spring-boot:run > "$LOG_DIR/backend.log" 2>&1 ) &
BACKEND_PID=$!

# Wait until the backend answers /api/health (up to ~90s).
echo "▶ Esperando al backend..."
for _ in $(seq 1 45); do
  if curl -s --max-time 2 http://localhost:8080/api/health | grep -q ok; then
    echo "✓ Backend listo"
    break
  fi
  sleep 2
done

echo "▶ Arrancando frontend..."
cd "$ROOT/frontend"
if [ ! -d node_modules ]; then
  pnpm install
fi
pnpm dev
