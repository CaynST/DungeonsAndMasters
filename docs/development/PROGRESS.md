# PROGRESS

## 2026-11-08 — Tablero a pantalla completa, responsive, ventaja y d100

**Objetivo:** cerrar U-01, U-05, R-16 y R-17 sin tocar el motor de reglas ni el backend.

**Acciones**
- U-01: `Board` deja de reservar una casilla vacía alrededor del mapa (`padding = 0`) y el layout
  del juego reduce padding/gaps; el tablero es el elemento que absorbe el espacio restante en
  escritorio.
- U-05: raíz de altura de viewport (`100dvh`) con la zona central desplazable en móvil/tablet. El
  tablero (`52dvh`, 58dvh en `sm`) y la sidebar (`62dvh`) tienen alturas utilizables; la barra de
  acciones queda fija como último hijo del shell. Cabecera con `truncate`/`min-w-0`, sidebar con
  `min-w-0` y menú de conjuros limitado a `min(20rem, 100vw - 2rem)` para evitar overflow.
- R-16: `lib/combat.ts` lee `advantage` del `dataJson` de los eventos; `DmPanel` pinta una insignia
  **Ventaja**/**Desventaja** en `ATTACK`/`ATTACK_MISS`.
- R-17: `DICE_SIDES` incluye 100; el menú rápido y `DiceTray` lo soportan como dado percentil
  (geometría d10 con caras 00–90). `outcomeOf` generaliza el resultado **MÁXIMO** a cualquier dado.

**Verificación**
- `pnpm exec tsc -b` en verde.
- `pnpm build` en verde.
- `pnpm test -- --maxWorkers=1 --no-file-parallelism`: **11 archivos, 33 tests** en verde.
- Tests nuevos: `lib/combat.test.ts`, insignias en `DmPanel.test.tsx`, d100/outcome en
  `dice.test.ts`, viewBox sin anillo en `Board.test.tsx`.
- Nota: con la máquina bajo carga alta, dos ejecuciones iniciales de Vitest agotaron el tiempo de
  respuesta del worker; la repetición con un solo worker terminó en verde. No se observaron fallos
  de aserción.
- `pnpm lint` solo reporta avisos preexistentes (no errores).

**Commits**
- `feat(ui): show attack advantage in the DM event log`
- `feat(ui): add d100 to the dice tray`
- `feat(ui): let the game board fill the screen and reflow on small devices`

**Siguiente paso**
- Verificación visual con navegador a 1440 px y ~390 px (no ejecutada en esta sesión).
- Continuar con deuda P2 restante (U-02 retratos, U-03 segunda pasada visual, U-04 accesibilidad).

## 2026-11-05 — Inspiración bárdica (R-11)

**Objetivo:** añadir el rasgo de Bardo de nivel 1 con acción adicional y uso en tiradas.

**Acciones**
- `BardicInspirationRules`: d6, 12 casillas, usos = modificador de CAR (mínimo 1), no autoinspiración.
- `BARDIC_INSPIRATION`: acción adicional, otro aliado vivo, sin inspiración previa, consume uso.
- `USE_INSPIRATION`: prepara el d6; se consume en la próxima tirada de ataque, prueba o salvación.
- `CombatService`/`GameService` suman el d6 y lo limpian; la concentración también puede usarlo.
- Duración 10 rondas; se reinicia al comenzar combate. UI con **Inspirar** e **Inspiración d6**.

**Verificación (TDD)**
- `BardicInspirationRulesTest` y `BardicInspirationIntegrationTest` (inspirar, usar, consumir,
  límites, autoinspiración, expiración, no-bardo).
- Backend completo: **225 tests** en verde. Frontend: **27 tests** y `pnpm build` en verde.

**Siguiente paso**
- R-11: Canalizar divinidad, Gran arma/Protección u otros rasgos.

## 2026-11-05 — Estilo de combate (R-11)

**Objetivo:** añadir el primer rasgo de clase pasivo con elección en la creación.

**Acciones**
- `FightingStyleRules`: Defensa (+1 CA con armadura), Arquería (+2 ataques a distancia) y Duelo
  (+2 daño con arma a una mano).
- `CharacterBuildRequest`/DTO/UI incorporan `fightingStyle`; solo Guerrero/Paladín.
- `CharacterBuilder` aplica Defensa a la CA y guarda el estilo; `EquipmentService` lo conserva al
  equipar.
- `Attack` distingue `twoHanded`; Duelo no se aplica a armas a dos manos.
- `CombatService` aplica Arquería al ataque y Duelo al daño; los eventos registran las
  bonificaciones.
- Kael recibe **Duelo** en bases nuevas y en las existentes mediante `V10`.

**Verificación (TDD)**
- `FightingStyleRulesTest`, dos casos nuevos en `CharacterBuilderTest`, `FightingStyleIntegrationTest`.
- Backend completo: **218 tests** en verde. Frontend: **27 tests** y `pnpm build` en verde.

**Siguiente paso**
- Completar R-11: Inspiración bárdica, Canalizar divinidad, Gran arma/Protección.

## 2026-11-05 — Componentes de conjuros

**Objetivo:** cerrar el alcance de R-10 con componentes V/S/M y foco material.

**Acciones**
- `Spell` incorpora `components` y `material`; la UI muestra `V, S, M` y el material en el tooltip.
- `SpellComponentRules`: un conjuro con **M** exige foco/bolsa; V/S no.
- `CharacterBuilder` añade foco arcano/símbolo sagrado/foco druídico/instrumento a lanzadores.
- `CampaignService` persiste `spellFocus`; `GameService` lo comprueba y, para tokens antiguos,
  deriva el foco del inventario del personaje.
- Bendición pasa a `V, S, M` (agua bendita); Palabra curativa a `V`.

**Verificación (TDD)**
- `SpellComponentRulesTest`, ampliación de `SpellCatalogTest` y
  `SpellIntegrationTest#materialComponentsRequireAFocus`.
- Backend completo: **209 tests** en verde. Frontend: **27 tests** y `pnpm build` en verde.

**Siguiente paso**
- R-10 queda cerrado en su alcance actual; continuar con R-11 (rasgos restantes) o deuda P2.

## 2026-11-05 — Conjuros de área

**Objetivo:** añadir el primer conjuro de área manteniendo el motor como autoridad.

**Acciones**
- `SpellTargeting.creaturesInCube`: criaturas vivas (PJ/ENEMIGO/PNJ) dentro del radio Chebyshev,
  excluyendo lanzador y objetos.
- `Spell.Kind.AREA` y `Spell.area(...)`: **Onda de trueno** (Mago, nivel 1), 2 casillas, salvación
  de CON, 2d8 de trueno.
- `GameService.castSpell` resuelve salvación y daño por cada objetivo; reutiliza
  `resolveTargetSave` y `CombatService.spellDamage`.
- UI: el menú muestra el conjuro como **área**; no requiere seleccionar un objetivo.

**Verificación (TDD)**
- `SpellTargetingTest` (incluye cerca, aliado, lejano, muerto y objeto).
- `SpellCatalogTest` (Onda de trueno y tipo AREA).
- `SpellIntegrationTest` (afecta a enemigos cercanos, no al lejano, consume espacio).
- Backend completo: **204 tests** en verde. Frontend: **27 tests** y `pnpm build` en verde.

**Siguiente paso**
- Componentes, más formas de área y upcasting; o completar R-11.

## 2026-11-05 — Arquitectura self-hosted (propuesta, sin código)

**Objetivo:** guardar la idea de hostear partidas y unirse por enlace/IP/código sin implementarla
todavía.

**Acciones**
- Creado `docs/development/ARCHITECTURE_SELF_HOSTED.md` con fases, huecos actuales del código,
  criterios de aceptación, riesgos y no objetivos.
- Añadidas tareas U-08 a U-12 en `TODO.md` y ADR-009 en `DECISIONS.md`.
- Confirmado que el enfoque correcto es **host-autoritativo**, no P2P: mismo origen, LAN primero,
  túnel para Internet, escritorio después y código corto solo con rendezvous.
- No se modificó código de producción.

**Estado del proyecto en este punto**
- Última unidad completada: **concentración + Bendición** (R-10 parcial).
- Backend: 202 tests en verde; frontend: 27 tests y build en verde.
- Pendiente inmediato de reglas: componentes/áreas/upcasting de conjuros (R-10) o rasgos restantes
  de R-11.
- Cambios locales sin commitear: `AGENTS.md`, `application.yml`, `vite.config.ts` (configuración de
  puerto 8081 del usuario).

**Siguiente paso**
- Retomar R-10/R-11; la arquitectura self-hosted queda esperando decisión de producto.

## 2026-10-30 — Conjuros

**Objetivo:** avanzar R-10 con un sistema de conjuros real, no solo botones.

**Acciones**
- `Spell`, `Spellbook`, `SpellCatalog` y `Spellcasting`: listas por clase, espacios, bono de ataque y DC.
- `SaveResolver` + `ConditionRules.saveAdvantage/autoFailsSave`: salvaciones con ventaja/desventaja
  y fallo automático por parálisis/aturdimiento/inconsciencia.
- `GameService.castSpell`: valida clase, objetivo, alcance, cobertura total, acción/acción adicional
  y espacios; consume el recurso antes de resolver.
- `CombatService.spellDamage` reutiliza el pipeline de daño/muerte (incluido moribundo/muerte).
- `GameState.spellbook` expone el catálogo a la UI; el menú **Conjuros** muestra espacios,
  alcance y descripción, y deshabilita lo que no se puede lanzar.
- Hechizos: Fuego feérico, Rayo de escarcha, Proyectil mágico (mago); Llama sagrada, Curar heridas
  y Palabra curativa (clérigo).

**Verificación (TDD)**
- `SpellCatalogTest`, `SpellcastingTest`, `SaveResolverTest` y casos de salvación en
  `ConditionRulesTest`.
- `SpellIntegrationTest` (no lanzadores, truco sin espacio, espacios limitados, curación con
  acción/adicional, salvación, catálogo en el estado).
- Backend completo: **199 tests** en verde. Frontend: **27 tests** y `pnpm build` en verde.
- Verificación visual con Lyra (Maga): menú **Conjuros**, **Espacios 2/2**, Fuego feérico,
  Rayo de escarcha y Proyectil mágico; sin errores de consola ni HTTP.

**Concentración (2026-10-30)**
- **Bendición** (Clérigo): +1d4 a ataques y salvaciones, 10 rondas, concentración.
- `CombatService` suma el d4 al ataque y comprueba concentración al recibir daño
  (CON CD 10 o mitad del daño); `CampaignService.startTurn` tickea la duración y
  `clearConcentration` limpia el buff en todos los objetivos.
- UI: chip **Conc. Bendición (rondas)** y estado **Bendito**.
- Tests: `SpellCatalogTest`, `SaveResolverTest`, `SpellIntegrationTest` (buff, ataque bendecido,
  rotura por daño, expiración). Backend **202 tests**; frontend **27 tests** y build.
- Verificación visual con Thorin: Bendición lanzada, **BENDITO**, **Conc. Bendición (10)** y
  espacios **1/2**, sin errores.

**Siguiente paso**
- Componentes/áreas/upcasting o completar los rasgos restantes de R-11.

## 2026-10-30 — Rasgos de clase

**Objetivo:** avanzar R-11 y completar la acción adicional de R-02 con rasgos reales.

**Acciones**
- `ClassFeatures` normaliza clases en inglés/español y lee `classId`/`level` del estado.
- **Segundo aliento** (Guerrero): acción adicional, `1d10 + nivel` PG, un uso por combate.
- **Furia** (Bárbaro): acción adicional, `+2` daño cuerpo a cuerpo, resistencia al daño físico,
  10 rondas y 2 usos; termina al inicio del turno si se agota.
- **Ataque furtivo** (Pícaro): `+1d6` una vez por turno con ventaja o un aliado consciente
  adyacente al objetivo; no aplica con desventaja.
- `DamageResolver` acepta bonificación plana y resistencia; el ataque registra `rageBonus`,
  `sneakAttack`, `sneakAttackDamage` y `resisted`.
- UI: chip de **acción adicional**, botones **Segundo aliento** y **Furia**; el estado de Furia y
  los usos se leen de la entidad.

**Verificación (TDD)**
- `ClassFeaturesTest`, `SecondWindRulesTest`, `RageRulesTest`, `SneakAttackRulesTest` y el caso
  de bonificación/resistencia en `RulesEngineTest`.
- `ClassFeatureIntegrationTest` (curación, límites de Furia, ataque furtivo, fin de Furia).
- Backend completo: **180 tests** en verde. Frontend: **27 tests** y `pnpm build` en verde.

**Corrección detectada en verificación visual**
- El PNJ usaba `/images/npcs/archivist.png` (inexistente) en vez de `archivista.png`; se añadió
  `AdventureGeneratorAssetsTest` (Red→Green) y se corrigió la ruta. Las campañas existentes se
  arreglan con las migraciones V8 (plantillas) y V9 (fichas del tablero).

**Siguiente paso**
- Completar R-11 (Estilo de combate, Inspiración, Canalizar divinidad) o R-10 (conjuros).

## 2026-10-29 — Cobertura

**Objetivo:** implementar R-05 con la rejilla como fuente de verdad.

**Acciones**
- `CoverRules`: línea de Bresenham entre centros de casillas; muro o puerta cerrada = total;
  1 obstáculo = media (+2 CA); 2+ obstáculos = tres cuartos (+5 CA); cuerpo a cuerpo a 1 casilla
  ignora cobertura.
- `GameService.attack` rechaza la cobertura total antes de consumir la acción.
- `CombatService.attack` aplica la CA efectiva a ataques de jugador y de IA, y registra
  `cover`/`coverBonus` en el evento; los ataques bloqueados se registran como `ATTACK_BLOCKED`.
- La UI muestra el motivo en el aviso y la bonificación en el registro de combate.

**Verificación (TDD)**
- `CoverRulesTest` (10 casos: línea abierta, obstáculos, muro, puerta, cuerpo a cuerpo, diagonal).
- `CoverIntegrationTest` (rechazo por cobertura total y +2 CA real en el resolver).
- Backend completo: **162 tests** en verde. Frontend: **24 tests** y `pnpm build` en verde.

**Siguiente paso**
- Conjuros/rasgos de clase (R-10/R-11) o las brechas P2 de `TECH_DEBT.md`.

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
- Backend completo: **150 tests** en verde. Frontend: **24 tests** y `pnpm build` en verde.

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
