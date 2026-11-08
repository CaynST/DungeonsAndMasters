# PROGRESS

## 2026-11-08 — Revisión de accesibilidad WCAG 2.2 AA (U-04)

**Objetivo:** auditar y corregir las pantallas no-VTT (Login, Home, Lobby, Crónica, Admin, tienda,
ficha y primitivas compartidas) sin cambiar la dirección visual ni el VTT.

**Fase:** completada.

**Acciones**
- Foco: anillo global `:focus-visible` de latón, anillo explícito en `Button` y en controles
  ad-hoc; enlace "Saltar al contenido" y landmark `<main>` en `App`.
- Movimiento: regla global `prefers-reduced-motion` y `Fireflies` del Lobby desactivadas con
  `useReducedMotion`; el scroll de "Crear campaña" respeta la preferencia.
- Semántica: `role="group"` para el selector Entrar/Crear cuenta, pestañas ARIA en Admin
  (roving tabindex + flechas/Home/End), diálogo modal de tienda con `aria-modal`, foco atrapado,
  Escape y restauración de foco, y tablas con `caption` y `scope="col"`.
- Formularios: errores con `role="alert"`, `aria-invalid`/`aria-describedby` en contraseña,
  etiquetas asociadas verificadas y formulario de creación de campaña con envío por Enter.
- Contraste: `text-muted/70` y `text-gold/70` sustituidos por tokens AA; precio no asequible y
  sobrecarga usan `crimson-soft`; placeholders del Login con contraste suficiente.
- Iconos: controles solo-icono con nombre accesible (cerrar tienda, retrato, borrar); iconos
  decorativos ocultos a tecnologías de asistencia.
- Color-only: barra de vida expuesta como `progressbar` con `aria-valuetext`; "no asequible" y
  estados listos/esperando llevan texto además de color.
- Tamaño táctil: botón "Usar" del inventario a 24×24 mínimo; el resto de controles ya superaba
  24 px.

**Verificación**
- `pnpm build` en verde.
- `pnpm test --maxWorkers=1 --no-file-parallelism --testTimeout=30000`: 21 archivos, 75 tests.
- `pnpm lint`: sin errores; se mantienen los avisos preexistentes (claves JSX en Admin, imports sin
  usar en archivos fuera de alcance, etc.).

**Commits**
- `feat(a11y): add global focus, skip link and reduced-motion baseline`
- `fix(a11y): give shared buttons focus rings and expose HP as a progressbar`
- `fix(a11y): announce login errors and label the access mode switch`
- `fix(a11y): make the shop a labelled dialog with focus trap and Escape`
- `fix(a11y): give admin sections real tabs and captioned tables`
- `fix(a11y): raise contrast, heading order and motion handling on non-game screens`
- `test(a11y): cover skip link and main landmark`

**Limitaciones / siguiente paso**
- El VTT (`Game`, `Board`, `ActionBar`) y `CharacterCreation` quedan para sus workstreams.
- No se ejecutó auditoría automática axe/Lighthouse ni prueba con lector de pantalla real; el
  contraste se verificó por cálculo (pares y ratios en el informe de la tarea).

## 2026-11-08 — Peso, capacidad y munición/herramientas (R-15)

**Objetivo:** modelar el peso del inventario y mostrarlo sin alterar las reglas de combate ni la
velocidad; dejar preparada la munición y las herramientas sin implementar su consumo.

**Acciones**
- `InventoryItem` incorpora `weight` (libras por unidad) y `totalWeight()`; el constructor de cuatro
  argumentos y `of(...)` conservan la compatibilidad. El JSON antiguo sin `weight` se lee como `0`
  gracias al valor por defecto de Jackson para primitivos.
- `PlayerCharacter` deriva carga total, capacidad `FUE × 15` y sobrecarga
  (`totalCarriedWeight()`, `carryingCapacity()`, `isEncumbered()`), y serializa `carriedWeight`,
  `carryingCapacity` y `encumbered` para la UI. La aritmética vive en el dominio, junto a
  `totalGold()`, sin crear una dependencia de `domain` hacia `engine`.
- `CharacterSheet` muestra **Carga X / Y lb**, avisa **Sobrecargado** cuando se supera la capacidad
  y muestra el peso de cada línea de inventario. La velocidad sigue siendo la del personaje: la
  sobrecarga es solo informativa.
- `ShopModal` muestra el peso unitario del catálogo, el peso de cada línea del inventario y la carga
  total/capacidad de la ficha. `ShopService` propaga el peso del catálogo a las compras.
- `ItemCatalog` asigna pesos SRD simplificados para que la tienda muestre peso y carga.
- Inventario inicial: el pícaro recibe **Herramientas de ladrón**; los usuarios de arco reciben
  **Flechas ×20**; los focos existentes conservan su función y ahora tienen peso. Los héroes
  predefinidos reciben pesos y equipo coherente (Aria: arco corto y flechas; Kael: jabalinas).
- Sin migración: el peso vive en `inventory_json`.

**Verificación (TDD)**
- Rojo: `InventoryItemTest`, `PlayerCharacterTest`, `CharacterBuilderTest`,
  `PredefinedCharactersTest` y `ShopApiTest` fallaron por API ausente antes de implementar.
- Backend completo: **266 tests** en verde (`mvn test`).
- Frontend: **48 tests** en verde (`pnpm test -- --maxWorkers=1 --no-file-parallelism
  --testTimeout=30000`) y `pnpm build` en verde.
- Nuevos tests frontend: `formatWeight`, carga/aviso en `CharacterSheet` y pesos/carga en
  `ShopModal`.

**Commits**
- `feat(equipment): add item weight, carrying capacity and encumbrance display`
- `docs(development): record equipment weight work`

**Siguiente paso**
- R-18: consumir munición al atacar y conectar la sobrecarga con la economía de movimiento cuando
  el producto lo decida. La carga actual no modifica la velocidad a propósito.

## 2026-11-08 — Acciones de combate (R-02/R-03)

**Objetivo:** cerrar R-02/R-03 con acciones reales, verificables y sin inventar un motor de
reacciones completo.

**Acciones**
- Nuevas acciones `HELP`, `HIDE`, `READY`, `SEARCH`, `USE_OBJECT` e `INTERACT` en `PlayerAction`,
  despachadas por `GameService`.
- **Ayudar**: aliado vivo a 1 casilla; `helpAdvantage` + `helpedBy` en el aliado. `CombatService`
  consume la bandera en el siguiente ataque y solo aplica ventaja si el objetivo está a 5 pies del
  ayudante.
- **Ocultarse**: falla con un enemigo vivo adyacente; marca `hidden` y da ventaja al siguiente
  ataque. **Buscar**: Percepción/Investigación con `CheckResolver`, consume la acción y registra
  `SEARCH`. **Usar objeto**: objetos no-poción del inventario con efecto descriptivo acotado;
  las pociones siguen por `USE_ITEM`.
- **Preparado**: consume la acción y marca `readied`. Si un enemigo entra en el alcance, el jugador
  ataca con su reacción; la bandera expira al inicio de su siguiente turno.
- **R-02**: `INTERACT` gratuita una vez por turno, sin coste de acción, y `opportunityAttacks`
  simétrico para que los enemigos provoquen ataques de oportunidad de los jugadores con su reacción.
- `TurnEconomy.reset` reinicia `objectInteractionUsed` y `readied`; `CombatService` limpia
  `helpAdvantage`, `helpedBy` y `hidden` al comenzar un combate.
- UI: menú compacto **Acciones** en `ActionBar` (Ayudar, Ocultarse, Prepararse, Buscar ×2, Usar
  objeto e Interactuar), con indicadores de bandera usada y ancho acotado para móvil.

**Verificación (TDD)**
- Red inicial: `CombatActionsIntegrationTest` no compilaba por constantes y `readiedAttacks`
  ausentes.
- Verde: **11 tests** de `CombatActionsIntegrationTest` (Ayudar con y sin objetivo cerca, Ocultarse,
  Buscar válido/inválido, Usar objeto/poción/desconocido, Preparado y su disparo/expiración,
  Interactuar y ataques de oportunidad simétricos/Retirada) y **3** de `TurnEconomyTest`.
- `CombatActionsIntegrationTest` también cubre que un ataque preparado consume la reacción y que
  un enemigo Retirado no provoca ataques de oportunidad.
- `ActionBar.test.tsx`: **5 tests** del menú, Help sin aliado, lista de objetos sin pociones y
  estados de un solo uso.
- Backend completo: **261 tests** en verde. Frontend: **47 tests** en verde con
  `pnpm test --maxWorkers=1 --no-file-parallelism --testTimeout=30000` y `pnpm build` en verde.
- Nota de comando: la forma literal `pnpm test -- --maxWorkers=1 ... --testTimeout=30000` no
  aplica el timeout (vitest recibe los flags tras `--` y usa 5 s); con esa forma fallan por timeout
  tests preexistentes (`Home`, `InitiativeTracker`). Sin el `--` los 47 pasan.

**Commits**
- `feat(combat): add R-02/R-03 actions and symmetric reactions`
- `feat(ui): add compact combat actions menu`
- `docs(development): record R-02/R-03 combat actions`

**Siguiente paso**
- Reacciones elegibles para conjuros/rasgos, tirada de Sigilo al ocultarse y disparadores genéricos
  para Preparado (ver `TECH_DEBT.md`).

## 2026-11-08 — Segunda pasada visual de Home, Lobby y Crónica (U-03)

**Objetivo:** mejorar jerarquía, espaciado y estados vacíos/carga de Home, Lobby y Crónica sin
tocar el tablero, la barra de acciones ni el contrato de la API.

**Acciones**
- Nuevos primitivos compartidos `EmptyState` y `Skeleton`, y helper `lib/campaigns` para traducir
  estado y fase de campaña. `Badge` deja de partir etiquetas cortas.
- **Home:** cabecera con identidad del usuario, campaña destacada con estado, miembros y fase,
  filtros con iconos, formulario con etiquetas, esqueletos con forma de tarjeta, estados vacíos con
  CTA y error con reintento. Las tarjetas de héroe muestran badges, retrato, CA/VEL en fichas y
  acciones accesibles. Se conserva intacta la consulta paginada/filtrada (`page`, `size`, `status`).
- **Lobby:** cabecera con imagen, estado, edición y fase; resumen de preparación; fila del DM;
  estados vacíos de compañía y héroes; selección con `aria-pressed` y confirmación del héroe.
- **Crónica:** recuento de entradas, agrupación por día, etiquetas en español, hora por evento,
  puntos de color y estados de carga/vacío/error con reintento.
- **CampaignCard:** estado y fase traducidos, DM visible, avatares de miembros y affordance de
  entrada. Sin cambios en el comportamiento de navegación.

**Verificación**
- `pnpm build` en verde.
- `pnpm test -- --maxWorkers=1 --no-file-parallelism --testTimeout=30000`: **15 archivos, 50 tests**
  en verde (8 nuevos: 2 en Home, 3 en Lobby y 3 en Crónica).
- `pnpm lint`: sin errores; solo warnings preexistentes ajenos a los archivos tocados.
- Verificación visual con Chrome headless + CDP a **1440 px** y **390 px**: Home, destacada, filtros,
  estado vacío, Lobby con y sin miembros y Crónica. Sin errores de consola.
- Nota: la primera ejecución completa de tests bajo carga (backend + Vite + Chrome) agotó el
  timeout por defecto de 5 s; tras detener los servicios, la misma orden terminó en verde.

**Commits**
- `be1debd` `feat(ui): add shared empty-state and skeleton primitives`
- `3334f30` `feat(ui): polish home, lobby and chronicle visual states`

**Siguiente paso**
- U-04 (accesibilidad: contraste, foco y `aria` en iconos) y reutilizar `Avatar`/`EmptyState` en las
  pantallas restantes cuando se retomen.

## 2026-11-08 — Terreno difícil (R-08) y sorpresa (R-09)

**Objetivo:** cerrar las dos brechas P2 del motor sin tocar el frontend ni inventar UX.

**Acciones**
- `GameMap` incorpora `difficultTerrain` con `isDifficultTerrain(cell)`; `MapEntity`/`MapMapper`
  lo persisten como JSON y la migración **V11** añade `maps.difficult_terrain_json`.
- `MovementValidator` cobra **2** al entrar en una casilla difícil, también en diagonal, porque
  este motor ya cobra 1 por cualquier paso. El coste se acumula en `MoveResult` y, por tanto,
  en `movementUsed` y en el límite por turno.
- `SurpriseRules` gestiona la bandera `surprised` de `entity.data`. `CombatService` la consume al
  abrir el combate y en cada avance de turno, registra `SURPRISED` y salta el primer turno.
- No se sembró terreno difícil en mapas existentes: el tablero aún no lo dibuja y añadirlo
  introduciría penalizaciones invisibles. La puerta queda abierta para sembrarlo cuando la UI lo
  muestre.
- La activación de la sorpresa se resolvió con la acción validada del DM `SET_SURPRISED`
  documentada en `TECH_DEBT.md`; el motor ya aplica la bandera.

**Verificación (TDD)**
- Red inicial: tests de `MovementValidator`, `MapMapper`, persistencia y `SurpriseRules` fallaron
  por APIs ausentes; un fallo de setup del test de sorpresa se corrigió antes de continuar.
- `MovementValidatorTest`: coste 3 en ruta normal+difícil, coste 2 en diagonal difícil y rechazo
  cuando el coste supera la velocidad.
- `MapMapperTest` (ida y vuelta JSON), `MapPersistenceTest` (columna V11 en SQLite) y
  `SurpriseRulesTest` (bandera consumible).
- `SurpriseIntegrationTest`: un participante sorprendido pierde su turno y se limpia la bandera;
  si todo el combate empieza sorprendido, se salta la ronda 1.
- Suite backend completa: **235 tests** en verde.

**Commits**
- `feat(map): support difficult terrain with double movement cost`
- `feat(combat): skip the first turn of surprised creatures`
- `docs(development): record difficult terrain and surprise progress`

**Siguiente paso**
- Detección automática/guion de la sorpresa y render del terreno difícil/sorpresa en la UI;
  terreno difícil en el tablero antes de sembrarlo en contenido.

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

## 2026-11-08 — Trasfondos y competencias (R-12, R-13)

**Objetivo:** completar los trasfondos PHB 2014 y mostrar idiomas y competencias en la ficha.

**Acciones**
- `RuleSet2014`: añadidos **Charlatán** (`charlatan`), **Artista** (`entertainer`),
  **Artesano gremial** (`guild-artisan`), **Ermitaño** (`hermit`), **Forastero** (`outlander`) y
  **Marinero** (`sailor`) con habilidades, descripción y sin bonificadores (2014).
- `RuleSet2024` se mantiene sin cambios: el SRD 5.2.1 solo incluye Acólito, Criminal, Sabio y
  Soldado, sin equivalentes de los seis trasfondos nuevos.
- `CharacterProficiencies` deriva idiomas de especie, competencias de clase y herramientas/idiomas
  de trasfondo. `CharacterBuilder` guarda `languages`, `armorProficiencies`,
  `weaponProficiencies` y `toolProficiencies` en `creationData` (sin migración).
- 2024 usa la regla de origen: Común + dos idiomas a elección, sin idiomas por especie.
- `CharacterSheet` muestra la sección **Idiomas y competencias** cuando hay datos.

**Verificación (TDD)**
- Rojo inicial: `RuleSetRegistryTest` falló con `Unknown background: charlatan`.
- `RuleSetRegistryTest`, `CharacterProficienciesTest`, `CharacterBuilderTest` y
  `CharacterCreationApiTest` (persistencia de `creationData` vía API).
- Backend completo: **233 tests** en verde. Frontend: **29 tests** y `pnpm build` en verde.
  `pnpm lint` solo reporta avisos preexistentes ajenos a estos archivos.

**Siguiente paso**
- R-02/R-03 (interacción/reacciones y acciones restantes), R-14 (descansos) o aplicar
  mecánicamente competencias de herramienta/idioma si el producto lo requiere.

## 2026-11-08 — Retratos consistentes y paginación de campañas (U-02, U-06)

**Objetivo:** unificar los avatares de personaje/PNJ y evitar que la Home renderice todas las
campañas sin límite, sin borrar datos de usuario.

**U-02 — Retratos**
- Auditoría: el tablero ya usaba `portrait`; Home y Lobby duplicaban imagen + fallback de iniciales;
  el Lobby ignoraba `dmAvatar`; la iniciativa no mostraba retrato. `CharacterSheet` (otro
  workstream), `CinematicIntro` y el fallback textual de `DmPanel` quedan fuera de alcance y se
  registran en `TECH_DEBT.md`.
- Nuevo `components/ui/Avatar`: retrato local/subido + fallback de iniciales determinista, color
  por personaje y recuperación al cambiar `src` o fallar la imagen. Sin assets externos.
- Usado en Home (héroes), Lobby (DM, miembros y elección de héroe), `CampaignCard` (avatar del DM) e
  `InitiativeTracker` (entidades del tablero).

**U-06 — Paginación y filtro**
- `GET /api/campaigns/page?page=&size=&status=` devuelve `CampaignPage` (`content`, `page`, `size`,
  `totalElements`, `totalPages`, `first`, `last`). `GET /api/campaigns` conserva el array legacy.
- `CampaignService.listPage` valida página (≥0), tamaño (1-100) y estado (`LOBBY`, `ACTIVE`,
  `FINISHED` o `ALL`), ordena por id descendente y no borra ninguna campaña.
- Home pide 6 campañas por página, ofrece filtros Todas/En curso/En lobby/Completadas y navegación
  Anterior/Siguiente; el bloque destacado se oculta al aplicar un filtro.

**Verificación (TDD)**
- Red: `CampaignPaginationApiTest` falló con 500 antes de registrar `/page`; `Avatar.test.tsx`
  falló por el componente inexistente.
- Backend completo: **229 tests** en verde (`mvn -q test`).
- Frontend: **34 tests** en verde (`vitest run --maxWorkers=1`) y `pnpm build` en verde.
- `pnpm lint` sin errores (solo warnings preexistentes).

**Commits**
- `1787377` `feat(campaigns): paginate and filter campaign listing` (backend).
- `ff6c258` `feat(campaigns): paginate and filter campaigns on Home` (frontend).
- `5747eab` `feat(ui): unify character and NPC avatars with local fallback`.
- Documentación de U-02/U-06 en este mismo cambio de `docs/development`.

**Siguiente paso**
- Reutilizar `Avatar` en `CharacterSheet`, `CinematicIntro` y `DmPanel` cuando se retomen esas
  pantallas; valorar una búsqueda por nombre además del filtro de estado.

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
