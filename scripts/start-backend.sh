#!/usr/bin/env bash
# Starts the Spring Boot backend. Reads configuration from the repo-root .env.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/backend"

echo "▶ Backend en http://localhost:8080  (Ctrl+C para parar)"
exec mvn spring-boot:run
