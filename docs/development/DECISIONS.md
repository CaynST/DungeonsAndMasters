# DECISIONS

## ADR-001 — No exponer el razonamiento del modelo
**Contexto:** los modelos de razonamiento devuelven `content: null` y el texto en `reasoning`.
Se estaba mostrando el pensamiento interno como narración del DM.
**Alternativa:** usar `reasoning` como respuesta cuando `content` falta.
**Decisión:** nunca exponer `reasoning`; si no hay `content`, fallar y probar el siguiente modelo o
el DM offline.
**Consecuencia:** la narración siempre es texto final; se pierde algo de contexto si el modelo agota
tokens, pero la inmersión y la coherencia mejoran.

## ADR-002 — Arte local por temática, sin generación de imágenes
**Contexto:** OpenCode Go/Zen no ofrece generación de imágenes (solo texto/visión).
**Alternativa:** integrar otro proveedor de imágenes (coste y otra clave).
**Decisión:** arte local curado (14 fondos por temática, retratos) + `ImageProvider` listo para un
proveedor real; los jugadores pueden subir retratos.
**Consecuencia:** cero coste y demo offline; el arte es estilizado, no fotorrealista.

## ADR-003 — Dirección de arte "mesa de taberna"
**Contexto:** la UI se percibía genérica ("IA"): superficies planas, acento púrpura, grids de tarjetas.
**Decisión:** tratar cada panel como una pieza de la mesa (madera, latón, pergamino), el tablero como
el mapa, y reservar el púrpura para lo arcano. Tipografía Sora/Inter con mayor contraste.
**Consecuencia:** se mantiene la legibilidad y se gana identidad; se desvía a propósito del estilo
"app SaaS" plano.

## ADR-004 — Condiciones como datos + reglas puras, aplicadas por el motor
**Contexto:** el PHB define 15 condiciones con efectos que cruzan ataques, pruebas, movimiento y
economía de acciones; el DM solo puede proponer acciones estructuradas.
**Alternativas:** codificar cada condición dentro de `CombatService`; o modelarlas como datos
persistidos y una capa pura de decisión.
**Decisión:** `Condition` (catálogo), `ConditionSet` (datos inmutables en `entity.data`) y
`ConditionRules` (ventaja, crítico, pruebas, velocidad). El DM aplica o retira con
`APPLY_CONDITION`/`REMOVE_CONDITION`; el motor valida y persiste. Las duraciones se tickean al
inicio del turno.
**Consecuencia:** las condiciones son testeables sin Spring, visibles en la UI y extensibles; las
aproximaciones (fuente de miedo, resistencia petrificada, salvaciones FUE/DES) quedan documentadas
en `TECH_DEBT.md`.

## ADR-005 — Cobertura por línea de rejilla, no por geometría continua
**Contexto:** el tablero es una rejilla de casillas y el motor ya usa distancias Chebyshev; una
solución geométrica continua sería más precisa pero desproporcionada para el prototipo.
**Alternativas:** calcular polígonos de visión; o aproximar con la línea de casillas.
**Decisión:** Bresenham entre centros de casillas. Muro o puerta cerrada bloquean por completo;
los obstáculos suman cobertura media (+2 CA) o de tres cuartos (+5 CA). El cuerpo a cuerpo a 1
casilla no tiene casilla intermedia y no recibe cobertura.
**Consecuencia:** reglas deterministas y testeables; las esquinas y la cobertura por criaturas
quedan como aproximaciones documentadas en `TECH_DEBT.md`.
