#!/usr/bin/env bash
# Starts the Vite dev server for the frontend.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/frontend"

if [ ! -d node_modules ]; then
  echo "▶ Instalando dependencias del frontend..."
  pnpm install
fi

echo "▶ Frontend en http://localhost:5173  (Ctrl+C para parar)"
exec pnpm dev
