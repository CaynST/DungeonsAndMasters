# Decisiones de arquitectura (ADR ligero)

## D-001 · Backend como autoridad del estado
**Contexto:** se quiere un DM IA flexible pero reglas correctas.
**Decisión:** el LLM solo produce acciones estructuradas; el engine valida y aplica.
**Consecuencia:** el DM no puede "hacer trampas" ni corromper el estado; la física y
las reglas son testeables sin IA.

## D-002 · Mock AI Provider por defecto
**Contexto:** puede no haber API key ni red.
**Decisión:** abstracción `AIProvider` con `MockAIProvider` que implementa el guion del
one-shot.
**Consecuencia:** la demo completa funciona offline; conectar un modelo real es
implementar una interfaz.

## D-003 · SQLite en lugar de PostgreSQL
**Contexto:** prototipo personal, un proceso, sin despliegue.
**Decisión:** SQLite + Flyway.
**Consecuencia:** cero infraestructura; migrar después es un cambio de datasource.

## D-004 · Tablero SVG antes que Canvas/WebGL
**Contexto:** el prototipo necesita un tablero claro y barato de modificar.
**Decisión:** SVG con grid y tokens.
**Consecuencia:** fácil de animar y accesible; si crece, migrar a Canvas.

## D-005 · WebSocket STOMP
**Contexto:** demostrar multiplayer conceptual.
**Decisión:** STOMP sobre Spring WebSocket, topic por campaña.
**Consecuencia:** estándar y simple; el cliente resincroniza estado al reconectar.

## D-006 · Auth JWT con BCrypt
**Contexto:** el prompt final pide usuarios reales; se confirmó usuario+contraseña.
**Decisión:** Spring Security + JWT firmado, contraseñas con BCrypt.
**Consecuencia:** realista y desacoplado; el dominio no conoce el mecanismo de sesión.

## D-007 · Reglas 5e simplificadas
**Contexto:** implementar todas las reglas es inviable y no aporta a la demo.
**Decisión:** implementar solo d20, mods, checks, ataque/daño/AC/HP, iniciativa, turnos,
movimiento y críticos.
**Consecuencia:** suficiente para el one-shot; extensible después.

## D-008 · Monorepo
**Contexto:** un solo desarrollador, dos apps acopladas por API.
**Decisión:** `backend/` + `frontend/` en un repo.
**Consecuencia:** commits atómicos que cruzan capas; sin coordinación de versiones.
