# Arquitectura

## Visión general

Monorepo con dos aplicaciones. El **backend** es la única autoridad sobre el estado del
juego; el **frontend** es una vista que envía intenciones y recibe estado + eventos.

```
┌────────────┐   REST (comandos/consultas)   ┌──────────────────────────────┐
│  Frontend  │ ────────────────────────────► │           Backend            │
│ React/TS   │ ◄──────────────────────────── │  web → service → engine      │
└────────────┘   STOMP (eventos broadcast)   │                → domain      │
                                             │  ai (DM) ─► ActionValidator  │
                                             │  persistence (SQLite/Flyway) │
                                             └──────────────────────────────┘
```

## Capas del backend

| Paquete | Responsabilidad | Depende de |
|---------|-----------------|------------|
| `domain` | Modelo de juego puro (sin framework) | — |
| `engine` | Dados, ataques, daño, iniciativa, turnos, movimiento | `domain` |
| `ai` | `AIProvider`, `DungeonMasterService`, validación de acciones | `domain` |
| `image` | `ImageProvider` y placeholders | — |
| `persistence` | Entidades JPA, repositorios, mappers | `domain` |
| `service` | Orquestación de campañas, estado y acciones | `engine`, `ai`, `persistence` |
| `security` | JWT + Spring Security | `persistence` |
| `web` | Controladores REST, DTOs, STOMP | `service` |

La regla de dependencias apunta siempre hacia el dominio: nada de `engine` conoce JPA
ni HTTP. Esto permite testear el motor sin arrancar Spring.

## Flujo de una acción de jugador

```
1. Frontend envía POST /api/campaigns/{id}/actions  (PlayerAction)
2. GameController delega en ActionProcessor
3. ActionProcessor valida reglas con engine (turno, movimiento, rango, etc.)
4. Se aplica el cambio al estado y se persiste
5. Se registra un GameEvent
6. WebSocket broadcast a /topic/campaign/{id}
7. Se devuelve ActionResult al emisor
```

## Flujo del Dungeon Master IA

```
GameState ─► DmContextBuilder ─► AIProvider ─► DmResponse (JSON estructurado)
                                                      │
                                                      ▼
                                              DmActionValidator
                                                      │
                                                      ▼
                                    ActionProcessor / engine (autoridad)
```

El DM **propone**; el engine **dispone**. Si una acción del DM es inválida, se
descarta y se registra el motivo.

## Frontend

- **TanStack Query** para estado del servidor (campañas, personaje, estado de juego).
- **Zustand** para estado efímero de UI (selección, target, cámara, animaciones).
- **WebSocket** (STOMP) para eventos en vivo; al reconectar, se hace *refetch* completo
  del estado para evitar divergencias.
- **SVG** para el tablero (grid, paredes, tokens) y **React Three Fiber** para dados 3D.

## Desacoplamiento y evolución

- Cambiar de SQLite a PostgreSQL = cambiar datasource + dialecto; el dominio no cambia.
- Añadir un proveedor de IA real = implementar `AIProvider`; nada más cambia.
- Añadir generación de imágenes = implementar `ImageProvider`.
