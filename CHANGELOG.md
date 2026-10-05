# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/) y
[Conventional Commits](https://www.conventionalcommits.org/).

## [Unreleased]

### Added

**Interacción y tablero**
- Cuadro de texto para **decirle al DM qué haces** (en tu turno); el DM responde a esa acción.
- **Movimiento con validación**: se calcula el camino, se muestran las casillas alcanzables y se
  avisa si el destino está bloqueado o queda más lejos que tus pasos.
- **Confirmación de ataque** (atacante, arma, bonificador y daño) antes de resolver.
- Pestañas **Dungeon Master / Grupo / Crónica**; el tablero ocupa ahora la mayor parte de la pantalla.
- **Crónica** completa de la campaña desde el principio (para quien se pierda la intro).
- **Dados 3D con el número en cada cara**, orientando la cara del resultado hacia la cámara.

**Reglas y economía**
- **Subclases** por clase en ambas ediciones, elegibles al crear el personaje.
- **Equipo que afecta a las estadísticas**: equipar armadura recalcula la CA y equipar un arma
  recalcula el ataque (comprar/vender/equipar desde la tienda).
- La **tienda solo se abre por evento del DM** (acción `OPEN_SHOP`); se cierra al cambiar de escena.

**Aventuras generadas**
- Cada campaña crea una **historia nueva**: el DM (IA) genera título, premisa, villano,
  escenas y enemigos; sin IA, un generador procedural crea igual una distinta.
- **Bestiario** de ~20 criaturas por tiers y **mapas procedurales** por temática.
- **Introducción cinematográfica**: premisa "érase una vez", presentación de la compañía y
  título épico, con opción de saltar.
- **Foco narrativo por turnos** fuera de combate: el DM se dirige a cada miembro.

**Economía e inventario**
- Monedas (oro/plata/cobre) con oro inicial por clase y **tienda** para comprar/vender.

**Frontend**
- Rediseño con tipografía legible (Sora + Inter; Cinzel solo para el título épico).
- **Dados 3D reales** por tipo (d4–d20) con el resultado visible y **tablero atmosférico**.

**Personajes y ediciones**
- Creación de personajes con reglas **SRD de nivel 1**: especie/raza, clase, trasfondo,
  compra de puntos (27), habilidades; el backend calcula HP, CA, modificadores, salvaciones
  y ataques. Catálogo por edición en `GET /api/rulesets/{edition}`.
- Dos ediciones por campaña: **D&D 5e (2014)** y **D&D 5e (2024)**. En 2024 las características
  provienen del trasfondo y "raza" pasa a ser "especie".
- Asistente de creación de personaje en el frontend (7 pasos) y lobby consciente de la edición.

**IA (OpenCode Go)**
- Proveedor **`opencode-go`** (`https://opencode.ai/zen/go/v1`) con modelo por defecto
  **`space-bunny-free`** y cadena de fallback barata (`deepseek-v4-flash`, `mimo-v2.5`,
  `glm-5.3-flash`). Headers `User-Agent` y `x-opencode-session`.
- Parseo robusto de la salida del DM: JSON simple, con cercas markdown o **truncado**.

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
- `README.md`, `AGENTS.md`, `CHANGELOG.md`, `docs/` (arquitectura, motor, creación de
  personajes, IA, base de datos, decisiones, roadmap), spec y plan de implementación.
- Tests: 61 backend (dados, reglas, movimiento, combate, creación de personaje, ediciones,
  auth, IA y flujo completo), 16 frontend.

### Fixed
- Bucle infinito de render por un selector de Zustand que devolvía un array nuevo
  (tablero con textos flotantes).
