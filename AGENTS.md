# AGENTS.md

Guía para agentes (y humanos) que trabajen en este repositorio.

## Reglas no negociables

1. **El backend es la autoridad.** Toda la matemática del juego vive en
   `backend/src/main/java/com/dndmasters/engine`. El LLM nunca calcula ni persiste.
2. **La IA produce acciones estructuradas.** `DmResponse.actions[]` se valida en
   `DmActionValidator` antes de aplicarse al estado.
3. **Sin secretos en código.** Config por variables de entorno.
4. **Mock primero.** La app debe funcionar sin API de IA ni proveedor de imágenes.
5. **No añadir dependencias** fuera de las definidas en el spec sin justificarlo.

## Comandos

```bash
# Backend
cd backend && mvn test && mvn spring-boot:run

# Frontend
cd frontend && pnpm install && pnpm test && pnpm build && pnpm dev
```

## Estructura

```
backend/src/main/java/com/dndmasters/
├── domain/        # modelo de juego
├── engine/        # reglas: dados, ataque, daño, iniciativa, turnos, movimiento
├── ai/            # AIProvider + DungeonMasterService (Mock y real)
├── image/         # ImageProvider
├── persistence/   # entidades JPA, repositorios, mappers
├── service/       # orquestación (campañas, estado, acciones, seeder)
├── security/      # JWT + Spring Security
└── web/           # REST + STOMP

frontend/src/
├── pages/         # Home, Login, Lobby, Game
├── components/    # board, dm, dice, actions, ui
├── store/         # Zustand
├── lib/           # api, ws, utils
└── types/         # tipos del dominio espejo del backend
```

## Convenciones

- **Conventional Commits** en inglés: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`.
- Tests por unidad de reglas y por componente crítico. No perseguir 100%.
- Antes de cada commit: tests, lint y build en verde; revisar que no haya secretos.
- No hacer push remoto automáticamente.
