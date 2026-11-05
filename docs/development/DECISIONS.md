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

## ADR-006 — Rasgos de clase como datos + reglas puras, con acción adicional
**Contexto:** Segundo aliento y Furia consumen la acción adicional; Ataque furtivo es pasivo y
depende de ventaja/aliado. El motor ya guarda banderas por entidad y el DM no debe calcularlas.
**Alternativas:** codificar cada rasgo en `CombatService`; o extraer reglas puras y datos.
**Decisión:** `ClassFeatures` normaliza la clase; `SecondWindRules`, `RageRules` y
`SneakAttackRules` deciden; `GameService` valida la acción adicional y `CombatService` aplica
bonificación/resistencia en `DamageResolver`.
**Consecuencia:** rasgos testeables sin Spring y extensibles; los usos se reinician al empezar
combate (no hay descansos todavía) y las armas con sutiliza no se modelan aún.

## ADR-007 — Conjuros con catálogo estático y resolución en el motor
**Contexto:** el cliente necesita conocer los conjuros disponibles, pero no debe calcular DC,
espacios ni daño. El prototipo solo tiene personajes de nivel 1 y objetivos individuales.
**Alternativas:** persistir hechizos por personaje y ampliar el esquema; o un catálogo estático
expuesto en `GameState`.
**Decisión:** `SpellCatalog` estático por clase, `GameState.spellbook` para la UI y
`GameService.castSpell` como autoridad. Los hechizos de ataque reutilizan el pipeline de ataque;
los de salvación usan `SaveResolver`; los automáticos y curativos usan `spellDamage`/curación.
**Consecuencia:** no hace falta migración de personajes y la UI no duplica reglas. Concentración,
componentes, áreas y upcasting quedan como deuda documentada.

## ADR-008 — Concentración con banderas en lanzador y objetivos
**Contexto:** la concentración 5e requiere una sola instancia por lanzador, duración y rotura por
daño; el prototipo no tiene descansos ni un gestor de efectos genérico.
**Decisión:** el lanzador guarda `concentrationSpell/Name/Rounds`; los objetivos guardan
`blessed`/`blessedBy`. `startTurn` tickea la duración, `clearConcentration` limpia los buffs del
lanzador y `CombatService` hace la salvación de CON al recibir daño.
**Consecuencia:** concentración observable y testeable sin un sistema de efectos completo. Los
componentes, áreas y upcasting siguen pendientes; el buff actual es específico de Bendición.

## ADR-009 — Partidas autoalojadas host-autoritativas (propuesta)
**Contexto:** la idea de compartir una partida con amigos mediante IP/enlace/código no requiere P2P;
el motor ya es server-authoritative y SQLite es suficiente para una mesa pequeña.
**Alternativas:** P2P WebRTC con consenso; servidor central en la nube; host autoalojado.
**Decisión:** host-autoritativo, mismo origen para frontend/API/WS, LAN con enlace/QR, Internet con
túnel gestionado y escritorio en una fase posterior. Documento completo en
`docs/development/ARCHITECTURE_SELF_HOSTED.md`.
**Consecuencia:** no se toca el motor ni el modelo de datos para el MVP; se evita la complejidad de
P2P y se traslada el coste de cómputo/banda al host. La seguridad, invitaciones y backups pasan a
ser responsabilidad del host y deben endurecerse antes de exponerlo a Internet.
