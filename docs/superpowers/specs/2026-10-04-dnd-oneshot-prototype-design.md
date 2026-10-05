# Plataforma D&D — Prototipo One-Shot ("La Cripta de Valdros") — Diseño

**Fecha:** 2026-10-04
**Estado:** Aprobado (deriva del prompt del proyecto + decisión de auth confirmada)

## 1. Objetivo

Construir una **vertical slice** jugable de una plataforma web para jugar D&D con un
Dungeon Master basado en IA. No es el producto final: prioriza **experiencia de juego**
y **calidad visual**, con una arquitectura sencilla, desacoplada y fácil de modificar.

Criterio de éxito: poder abrir la app, entrar, elegir personaje, empezar el one-shot
"La Cripta de Valdros", jugar exploración + un check + un combate con enemigos y jefe,
y terminar la aventura — todo con dados 3D, tablero y narración del DM.

## 2. Decisiones confirmadas

- **Auth:** usuario + contraseña con **hash BCrypt** y sesión **JWT**. Spring Security
  real, pero las rutas de juego aceptan el token; el dominio no depende del mecanismo.
- **Backend = autoridad del estado.** El LLM nunca calcula ni persiste; produce acciones
  estructuradas que el engine valida y aplica.
- **IA:** `AIProvider` desacoplado + `MockAIProvider` por defecto (funciona sin API).
  Adaptador real por variables de entorno (`AI_PROVIDER`, `AI_API_KEY`, `AI_MODEL`).
- **Imágenes:** `ImageProvider` desacoplado + placeholders locales por defecto.
- **DB:** SQLite + JPA/Hibernate + Flyway, persistencia desacoplada tras repositorios.
- **Multiplayer:** WebSocket STOMP; eventos de dominio broadcast a la campaña.
- **Build:** Maven (backend), Vite (frontend).

## 3. Arquitectura

```
frontend (React/TS)  ⇄ REST + STOMP ⇄  backend (Spring Boot)
                                          │
                    ┌─────────────────────┼──────────────────────┐
                    ▼                     ▼                      ▼
              web (REST/WS)          service (orquestación)      ai (DM)
                    │                     │                      │
                    ▼                     ▼                      ▼
              persistence          engine (reglas)  ◄──── ActionValidator
              (JPA/Flyway)               │
                                         ▼
                                   domain (modelo)
```

Autoridad y flujo:

```
Player → Frontend → Backend → Game Engine → Rules Engine → Game State
Game State → DM AI → Structured Action → Game Engine → Validation → State Update
```

### Paquetes backend

- `domain` — modelo de juego puro (Character, Stats, Entity, Combat, Scene, Board…).
- `engine` — DiceEngine, AttackResolver, DamageResolver, InitiativeEngine,
  TurnManager, MovementValidator, Checks. **Toda la matemática vive aquí.**
- `ai` — `AIProvider`, `MockAIProvider`, adaptador real, `DungeonMasterService`,
  `DmContext` (contexto estructurado), `DmResponse` (salida estructurada),
  `ActionValidator`.
- `image` — `ImageProvider`, `PlaceholderImageProvider`.
- `persistence` — entidades JPA, repositorios, mappers.
- `service` — CampaignService, GameStateService, CharacterService, AdventureSeeder.
- `web` — controladores REST, DTOs, config STOMP, manejador de eventos.
- `security` — JWT, filtros, UserDetails.

### Frontend

- Rutas: `/` (Home/sala), `/login`, `/campaign/:id/lobby`, `/campaign/:id/play`.
- Estado servidor: TanStack Query. Estado de UI/juego local: Zustand.
- Tablero: **SVG** con grid, paredes, puertas, obstáculos, entidades, selección,
  resaltado de movimiento y cámara/zoom.
- Dados: **React Three Fiber** + drei. Animación de tirada y resultado.
- DM: panel narrativo (no un chat genérico) con avatar, narración, eventos, checks.
- Animación: Framer Motion (daño, muerte, turnos, transiciones de escena).

## 4. Dirección artística

"Taberna medieval reinterpretada como app moderna de gaming." **70% gaming UI +
30% fantasía.** Paleta oscura dada (background #0D0B09, surface #17130F, elevated
#211A14, wood #2A1D14, parchment #D8C39A, text #E8E1D5, muted #9E9485) con acentos
gold/crimson/emerald/arcane. Tipografía: **Cinzel** (títulos/narrativa) + **Inter**
(UI/estadísticas). El dorado se usa con moderación.

## 5. Sistema de juego (5e simplificado)

d20 + modificador vs DC/AC; ventaja/desventaja opcional; crítico natural 20 (dobla
dados de daño), fallo crítico natural 1. Ataques con bonus y daño por dados.
HP, AC, iniciativa (d20 + DEX mod), rondas, turnos, movimiento por casillas, checks
por habilidad. Solo las reglas necesarias para el one-shot.

## 6. Contenido del one-shot

Introducción → exploración → NPC (archivista) → decisión → check → combate
(goblins + arquero) → jefe (Guardián de Valdros) → recompensa → final.

Personajes predefinidos: Kael (Human Fighter), Lyra (Elf Wizard), Aria (Half-Elf
Rogue), Thorin (Dwarf Cleric). Enemigos: Goblin, Goblin Archer, Valdros Guardian.

## 7. Fuera de alcance (roadmap)

Amigos, editor de mapas, fog of war complejo, generación real de imágenes, todas las
clases/reglas de D&D, campañas múltiples por usuario con DM persistente a largo plazo,
despliegue/cloud/Docker.

## 8. Pruebas

Backend: DiceEngine, AttackResolver, DamageResolver, Initiative, TurnManager,
HP y transiciones de estado de combate. Frontend: componentes críticos (board,
initiative, dice, DM panel) y acciones principales con Vitest + Testing Library.
Sin perseguir cobertura artificial.
