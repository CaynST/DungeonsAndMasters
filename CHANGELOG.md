# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/) y
[Conventional Commits](https://www.conventionalcommits.org/).

## [Unreleased]

### Added

**Backend**
- Scaffold Spring Boot 3.5 + Java 21 + Maven con SQLite, Hibernate y Flyway.
- Modelo de dominio puro y migraciones `V1`/`V2` (usuarios, personajes, campañas,
  escenas, mapas, entidades de tablero, NPCs, enemigos, items, combate, eventos, tiradas).
- `DiceEngine` (d4–d20, `NdM±K`, crítico/fallo natural, entrada mal formada rechazada).
- Rules engine: modificadores, checks, ataques, daño, iniciativa, HP, movimiento.
- `CampaignService`, `CombatService`, `GameService`, `GameEventService` — el backend es
  la autoridad del estado; el DM propone acciones y el engine las valida.
- Dungeon Master IA desacoplado: `AIProvider`, `MockAIProvider` (one-shot completo
  offline), `OpenAiCompatibleProvider`, `DungeonMasterService`, `DmActionValidator`.
- `ImageProvider` + `PlaceholderImageProvider` con arte SVG generado.
- Multiplayer por WebSocket STOMP (`/topic/campaign/{id}`).
- Auth usuario + contraseña con BCrypt y JWT; rutas de juego protegidas.

**Frontend**
- React + TypeScript + Vite + Tailwind con la dirección artística del spec (Cinzel +
  Inter, paleta oscura gold/crimson/emerald/arcane).
- Home (sala de aventuras), Login, Lobby (taberna con partículas) y pantalla de juego VTT.
- Tablero SVG con grid, paredes, puertas, obstáculos, tokens, selección y resaltado.
- Tracker de iniciativa, tarjeta de personaje y panel narrativo del Dungeon Master.
- Dados 3D con React Three Fiber (aparece, rueda, se detiene, muestra resultado y
  destaca crítico/fallo).
- Acciones: mover, atacar, checks, dados rápidos, hablar, terminar turno.
- Sincronización en vivo por WebSocket con resync al reconectar.

**Docs / proceso**
- `README.md`, `AGENTS.md`, `CHANGELOG.md`, `docs/` (arquitectura, motor, IA, base de
  datos, decisiones, roadmap), spec y plan de implementación.
- Tests: 45 backend (dados, reglas, movimiento, combate, auth, flujo completo), 10 frontend.
