# Dungeons & Masters — Prototipo One-Shot

Vertical slice jugable de una plataforma web para jugar Dungeons & Dragons con un
**Dungeon Master basado en IA**. La demo implementa el one-shot **"La Cripta de
Valdros"**: exploración, un check, un combate con goblins y un enemigo final.

> Este es un prototipo para demostrar la experiencia. No implementa todas las reglas
> ni todas las funcionalidades de la plataforma final.

## Stack

| Capa | Tecnología |
|------|------------|
| Backend | Java 21 · Spring Boot 3 · Maven · Spring Security + JWT |
| Persistencia | SQLite · Hibernate/JPA · Flyway |
| Tiempo real | WebSocket (STOMP) |
| IA | `AIProvider` desacoplado · **Mock** por defecto |
| Frontend | React + TypeScript + Vite · Tailwind · shadcn/ui · Zustand · TanStack Query |
| Visual | Three.js / React Three Fiber / drei · Framer Motion · SVG |

La **autoridad del estado del juego es el backend**. El LLM nunca calcula ni escribe
en la base de datos: produce acciones estructuradas que el *rules engine* valida.

## Requisitos

- Java 21 + Maven
- Node 20+ (se usa pnpm)

## Ejecución

### Backend

```bash
cd backend
mvn spring-boot:run
# API en http://localhost:8080  (health: /api/health)
```

La base de datos SQLite se crea en `backend/data/dnd.db` y Flyway aplica el esquema
al arrancar.

### Frontend

```bash
cd frontend
pnpm install
pnpm dev
# App en http://localhost:5173
```

## Variables de entorno

Todas opcionales. Sin ellas la app funciona usando los proveedores Mock.

```dotenv
# Backend
AI_PROVIDER=mock            # mock | openai-compatible
AI_API_KEY=
AI_MODEL=gpt-4o-mini
AI_BASE_URL=https://api.openai.com/v1
IMAGE_PROVIDER=placeholder  # placeholder | (futuro proveedor real)
JWT_SECRET=change-me-in-production
```

## Flujo de la demo

```
Home → Lobby → elegir personaje → comenzar aventura
     → introducción del DM → escena → acción → check/dado
     → narración → combate → iniciativa → turno → mover → atacar
     → daño → enemigos → jefe → victoria → final del one-shot
```

El Dungeon Master usa un guion offline (`MockAIProvider`) mientras `AI_PROVIDER=mock`,
de modo que la partida completa es jugable sin API. Para conectar un modelo real:
`AI_PROVIDER=openai-compatible AI_API_KEY=... AI_MODEL=...`.

## Tests

```bash
cd backend && mvn test
cd frontend && pnpm test
```

## Documentación

- [`docs/architecture.md`](docs/architecture.md)
- [`docs/game-engine.md`](docs/game-engine.md)
- [`docs/ai-dm.md`](docs/ai-dm.md)
- [`docs/database.md`](docs/database.md)
- [`docs/decisions.md`](docs/decisions.md)
- [`docs/roadmap.md`](docs/roadmap.md)
- [`docs/walkthrough.md`](docs/walkthrough.md)
