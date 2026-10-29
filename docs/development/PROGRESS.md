# PROGRESS

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

**Commits**
- `feat(rules): validate attack range and forbid cutting wall corners`
- `docs(development): mark P0 range and corner rules as done`

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
