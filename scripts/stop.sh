#!/usr/bin/env bash
# Stops any backend/frontend dev processes started by the scripts.
set -uo pipefail

echo "▶ Parando procesos de desarrollo..."
pkill -f "spring-boot:run" 2>/dev/null && echo "  backend detenido" || echo "  backend no estaba corriendo"
pkill -f "vite" 2>/dev/null && echo "  frontend detenido" || echo "  frontend no estaba corriendo"
