# PROGRESS

## 2026-10-29 — Condiciones del Apéndice A

**Objetivo:** implementar las condiciones 5e con efectos reales en el motor, aplicación por el DM
y visibilidad en la UI.

**Acciones**
- Modelo puro `Condition` / `ConditionSet` / `ConditionRules` con los 15 estados, duración por
  rondas, agotamiento acumulativo (1-6) y consultas de reglas.
- Integración: ventaja/desventaja y cancelación, crítico automático cuerpo a cuerpo contra
  paralizado/inconsciente, desventaja en pruebas, penalti -2/nivel de agotamiento, velocidad 0,
  movimiento a la mitad (agotamiento 2 / arrastrarse) y bloqueo de acciones incapacitadas.
- Acciones del DM `APPLY_CONDITION` / `REMOVE_CONDITION` validadas y registradas como eventos.
- Acción `STAND_UP` (coste: mitad de la velocidad) y tick de duraciones al inicio del turno.
- UI: insignias de estado en tablero, iniciativa y barra; aviso de incapacitado; botón Levantarse.
- DM offline: fallar la prueba de percepción/investigación inicial aplica **Cegado** durante 2 rondas.

**Verificación (TDD)**
- `ConditionTest`, `ConditionSetTest`, `ConditionRulesTest` (modelo puro).
- `ConditionIntegrationTest` (expiración, bloqueo, penalti, ventaja, DM, `STAND_UP`).
- `DmActionValidatorTest`, `MockAIProviderTest`.
- Backend completo: **144 tests** en verde. Frontend: **24 tests** y `pnpm build` en verde.

**Siguiente paso**
- Cobertura (R-05), luego conjuros/rasgos de clase y las aproximaciones listadas en `TECH_DEBT.md`.

## 2026-10-29 — Reglas P0: rango y esquinas

**Objetivo:** cerrar las dos brechas P0 detectadas en la auditoría.

**Acciones**
- `RangeRules`: ataques cuerpo a cuerpo a 1 casilla; a distancia con alcance normal y largo
  (hasta 2×) con **desventaja**; más allá se rechaza. Integrado en `GameService.attack`.
- `AttackResolver`: soporte de ventaja/desventaja (2d20, mayor/menor).
- `MovementValidator`: el movimiento diagonal no puede atravesar la esquina de un muro/obstáculo.

**Verificación (TDD)**
- `RangeRulesTest` (5 casos) y test de ventaja/desventaja en `RulesEngineTest`.
- `MovementValidatorTest`: test Red de esquina bloqueada y caso permitido.
- Suite backend completa en verde.

**Muerte y salvaciones (P1)**
- `DeathSaveRules` puro (moribundo/estable/muerto, 20/1, daño masivo) con tests.
- Integrado en `CombatService` (los PJ caen moribundos) y `GameService` (acción `DEATH_SAVE`,
  bloqueo de acciones para moribundos, curación limpia el estado).
- Frontend: banner de moribundo/estable y botón de salvación; acciones bloqueadas.

**Economía de turno (P1 parcial)**
- `TurnEconomy` puro (reset de acción/bono/movimiento/reacción) con tests.
- Correr, Esquivar y Retirada como acciones; ataques de oportunidad enemigos al salir de alcance;
  ventaja/desventaja de Esquivar integrada en el resolver.
- Frontend: botones Correr/Esquivar/Retirada y movimiento máximo leído del estado.

**Commits**
- `feat(rules): validate attack range and forbid cutting wall corners`
- `docs(development): mark P0 range and corner rules as done`
- `feat(rules): death saving throws and dying state`
- `feat(rules): turn economy, dodge and opportunity attacks`

## 2026-10-28 — Auditoría de manuales + rediseño visual

**Objetivo:** verificar compatibilidad del flujo de juego con los manuales (PHB, DMG, MM),
auditar el frontend y aplicar una dirección de arte no genérica.

**Fase:** completada (análisis + correcciones críticas + primera pasada visual).

**Acciones**
- Extraídos y analizados los tres manuales (`manuals/`, fuera del repo).
- Comparado el motor de reglas con el PHB (ver TECH_DEBT.md para brechas).
- Capturada la UI real con Chromium (Login, Home, Lobby, Juego) y auditada.
- Corregidos 3 bugs visuales/funcionales críticos:
  1. Imágenes SVG guardadas como `.png` → convertidas a PNG reales (`rsvg-convert`).
  2. Dev server no proxeaba `/images` ni `/uploads` → añadido a Vite.
  3. El DM mostraba su cadena de razonamiento → el proveedor ya no la expone.
  4. La barra de acciones mostraba el personaje de otro jugador → ahora usa el del usuario.
- Rediseño: dirección "mesa de taberna" (madera/latón/pergamino), campaña destacada en Home,
  Login con imagen de fondo, CTA dorado, fichas del tablero con retrato, intro con viñeta y
  letterbox, 14 fondos por temática, contraste y tipografía mejorados.

**Verificación**
- Backend: `mvn test` en verde (incluye nuevo test de razonamiento no expuesto).
- Frontend: `pnpm build` y `pnpm test` en verde (21 tests).
- Capturas: `.taste-skill` no usado; capturas temporales en `/tmp/opencode/audit/`.

**Commits**
- `fix(ai): never surface chain-of-thought ...`
- `feat(design): tavern art direction, themed backdrops ...`
- `docs(development): audit findings and rules gaps`

**Siguiente paso**
- Priorizar brechas de reglas en `TODO.md` (muerte/salvaciones, acciones/reacciones, cobertura,
  rango, condiciones, conjuros) y terminar la segunda pasada visual (tablero a pantalla completa,
  retratos ilustrados, pulido de la intro).
