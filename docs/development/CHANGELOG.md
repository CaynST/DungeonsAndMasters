# CHANGELOG (desarrollo)

## 2026-10-29

- **D-13** `feat(rules)` — Condiciones del Apéndice A: modelo puro (`Condition`, `ConditionSet`,
  `ConditionRules`) con los 15 estados, ventaja/desventaja, crítico automático, velocidad 0 y
  agotamiento; duración por rondas y acciones `APPLY_CONDITION`/`REMOVE_CONDITION` del DM. Penalti de
  agotamiento en pruebas, bloqueo de acciones incapacitadas y acción `STAND_UP`. UI: insignias en
  tablero, iniciativa y barra de acciones; aviso de incapacitado. Disparador offline: fallar la
  prueba inicial aplica **Cegado**.

- **D-09** `feat(rules)` — Validación de rango de ataque: cuerpo a cuerpo 1 casilla; a distancia
  con alcance normal y largo (2×) con desventaja; fuera de alcance se rechaza. Nuevo
  `RangeRules` y soporte de ventaja/desventaja en `AttackResolver`.
- **D-10** `fix(rules)` — El movimiento diagonal ya no atraviesa esquinas de muros/obstáculos.
- **D-12** `feat(rules)` — Economía de turno: `TurnEconomy` (reset de acción/movimiento/bono/reacción),
  **Correr** (movimiento ×2), **Esquivar** (desventaja a ataques contra ti), **Retirada** (sin ataques
  de oportunidad) y **ataques de oportunidad** de enemigos al salir de su alcance (1 reacción).
  Botones nuevos en la barra de acciones.
- **D-11** `feat(rules)` — Muerte y salvaciones: los PJ a 0 PG caen moribundos; salvación al inicio
  del turno (3 éxitos estabilizan, 3 fallos matan, 20 = 1 PG, 1 = 2 fallos); daño a 0 PG suma fallo
  (2 en crítico) y daño ≥ PG máximos mata. Acción `DEATH_SAVE` y bloqueo de acciones para moribundos.

## 2026-10-28

- **D-01** `fix(assets)` — Convertidos los placeholders de SVG-con-extensión-`.png` a PNG reales;
  los retratos y fondos vuelven a renderizarse. Componente: `backend/.../static/images`.
- **D-02** `fix(frontend)` — Añadidos `/images` y `/uploads` al proxy de Vite; antes el dev server
  los pedía a 5173 y fallaban. Componente: `frontend/vite.config.ts`.
- **D-03** `fix(ai)` — El proveedor ya no devuelve `reasoning`/`reasoning_content` como respuesta;
  si no hay `content`, falla y se usa el siguiente modelo o el DM offline. Subido `max_tokens` a 1500.
  Componente: `ai/OpenAiCompatibleProvider`, `ai/DungeonMasterService`.
- **D-04** `fix(game)` — La barra de acciones usa el personaje del usuario autenticado
  (`campaign.members`), no el primer token controlado por un humano.
- **D-05** `feat(design)` — Dirección "mesa de taberna": contraste y tipografía, campaña destacada en
  Home con imagen real, Login atmosférico, CTA dorado en Lobby y ataque en carmesí.
- **D-06** `feat(design)` — 14 fondos PNG por temática y asignación a campaña/escena según el tema.
- **D-07** `feat(design)` — Fichas del tablero con retrato (recorte circular) en lugar de iniciales.
- **D-08** `feat(design)` — Intro cinematográfica con imagen de fondo, viñeta y letterbox.
- **docs** — Auditoría de manuales y brechas de reglas (`TODO.md`, `TECH_DEBT.md`).
