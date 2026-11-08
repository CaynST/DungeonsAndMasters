# CHANGELOG (desarrollo)

## 2026-11-08

- **D-38** `feat(rest)` — R-14: **descanso corto y largo** con dados de golpe. `PlayerAction`
  acepta `SHORT_REST`/`LONG_REST`; `RestRules` deriva el dado por clase/nivel y calcula curación y
  recuperación. El descanso corto gasta un dado y cura `dado + CON` (tope PG), recarga Segundo
  aliento y no restaura espacios; el largo restaura PG, espacios, rasgos, concentración,
  inspiración y banderas de un solo uso, y recupera al menos la mitad de los dados (mínimo 1).
  Ambos se rechazan en combate o con el personaje moribundo/incapacitado. Menú **Descanso** en
  `ActionBar` con dados restantes y avisos, deshabilitado en combate. Sin migración.

- **D-37** `feat(ui)` — Segunda pasada visual de **Home, Lobby, Crónica y estados vacíos/carga**:
  `EmptyState` y `Skeleton` compartidos, etiquetas de estado/fase de campaña centralizadas,
  cabecera destacada y tarjetas de héroe con mejor jerarquía, resumen de preparación y selección de
  héroe accesible en Lobby, y crónica agrupada por día con etiquetas en español y reintento.
  Verificación visual a 1440 px y 390 px; 50 tests y build en verde.

- **D-35** `feat(equipment)` — Peso de inventario (libras por unidad), carga total y capacidad
  `FUE × 15` en `PlayerCharacter`; JSON antiguo sin `weight` se lee como `0`. `CharacterSheet`
  muestra **Carga X / Y lb** y un aviso de **Sobrecargado** solo informativo; `ShopModal` muestra
  pesos y carga. `ItemCatalog` recibe pesos; pícaros reciben herramientas de ladrón y los arcos
  empiezan con 20 flechas. Sin migración y sin consumo de munición todavía (R-18).

- **D-36** `feat(combat)` — R-02/R-03: acciones **Ayudar**, **Ocultarse**, **Preparado**,
  **Buscar** y **Usar objeto**; interacción gratuita `INTERACT` una vez por turno; ataques de
  oportunidad del jugador simétricos a los enemigos; `TurnEconomy` reinicia interacción y
  `readied`. `CombatService` consume las banderas de un solo uso y los ataques preparados al entrar
  un enemigo en alcance. UI compacta "Acciones" en `ActionBar` con estado de cada bandera y sin
  romper el layout móvil. Aproximaciones documentadas en `DECISIONS.md` (ADR-018) y `TECH_DEBT.md`.

- **D-34** `feat(campaigns)` — Listado paginado/filtrable: `GET /api/campaigns/page` devuelve
  `CampaignPage` con página, tamaño, estado y metadatos; el endpoint legacy conserva el array. La
  Home pagina de 6 en 6 con filtros Todas/En curso/En lobby/Completadas. Sin borrar campañas.

- **D-33** `feat(ui)` — `Avatar` reutilizable con retrato local y fallback determinista de iniciales
  (incluida recuperación ante imagen rota); usado en Home, Lobby, CampaignCard e InitiativeTracker.

- **D-32** `feat(character)` — Idiomas y competencias de armadura/arma/herramienta: se derivan de
  especie, clase y trasfondo en `CharacterProficiencies` y se guardan en `creationData`
  (`languages`, `armorProficiencies`, `weaponProficiencies`, `toolProficiencies`). La ficha del
  personaje muestra una sección **Idiomas y competencias**. Sin migración de base de datos.

- **D-31** `feat(rules)` — Añadidos los seis trasfondos PHB 2014 que faltaban: **Charlatán**,
  **Artista**, **Artesano gremial**, **Ermitaño**, **Forastero** y **Marinero**, con sus dos
  habilidades y descripción en español. No se añaden a 2024 porque el SRD 5.2.1 solo incluye
  Acólito, Criminal, Sabio y Soldado. Sin bonificadores de característica en 2014.

- **D-30** `feat(ui)` — **d100** añadido al menú de dados y a la mesa 3D como dado percentil
  (caras 00–90). El resultado máximo de cualquier dado se clasifica como **MÁXIMO**.

- **D-29** `feat(ui)` — El registro del DM muestra una insignia **Ventaja** o **Desventaja** en los
  eventos de ataque, leyendo `advantage` del `dataJson` con tolerancia a payloads corruptos.

- **D-28** `feat(ui)` — El tablero usa toda el área disponible: se elimina el anillo vacío de una
  casilla alrededor del mapa y se reducen márgenes/gaps del layout. En móvil/tablet el tablero y la
  sidebar tienen alturas propias dentro de una columna desplazable, mientras la barra de acciones
  queda siempre visible; cabecera truncada y menús de conjuros/dados acotados para evitar overflow
  horizontal.

- **D-27** `feat(dm)` — Disparador de sorpresa: acción estructurada **SET_SURPRISED** validada
  para un `entityId` de la escena; aplica la bandera que consume `SurpriseRules` y registra
  `SURPRISE_APPLIED`. El DM puede declarar una emboscada sin inventar una UX de jugador.

- **D-26** `feat(combat)` — Sorpresa: `SurpriseRules` gestiona la bandera `surprised` en
  `entity.data`. `CombatService` la consume al abrir el combate o al avanzar el turno, registra
  `SURPRISED` y salta el primer turno de la criatura; la bandera se limpia exactamente una vez.

- **D-25** `feat(map)` — Terreno difícil: `GameMap.difficultTerrain` se persiste en
  `maps.difficult_terrain_json` (migración V11) y `MovementValidator` cobra **2** al entrar en
  esas casillas, también en diagonal (el motor cobra 1 por paso). No se sembró contenido nuevo
  para no alterar mapas existentes ni introducir penalizaciones invisibles en la UI.

## 2026-11-05

- **D-24** `feat(rules)` — Inspiración bárdica: acción adicional para dar un **d6** a otro aliado
  a 12 casillas; usos = modificador de CAR (mínimo 1). El aliado puede preparar el dado y sumarlo a
  su próxima tirada de ataque, prueba o salvación. Dura 10 rondas y se reinicia al comenzar combate.
  UI con botones **Inspirar** y **Inspiración d6**.

- **D-23** `feat(rules)` — Estilo de combate: **Defensa** (+1 CA con armadura), **Arquería**
  (+2 a ataques a distancia) y **Duelo** (+2 daño con arma a una mano). Selección en creación para
  Guerrero/Paladín; `Attack` distingue armas a dos manos; `EquipmentService` conserva Defensa al
  equipar. Kael recibe Duelo mediante la migración V10.

- **D-22** `feat(rules)` — Componentes de conjuros: metadata V/S/M, **M** exige foco o bolsa de
  componentes, los lanzadores reciben foco en el inventario inicial y la UI muestra los componentes.
  Bendición requiere `V, S, M` (agua bendita); los trucos y conjuros V/S no lo necesitan.

- **D-21** `feat(rules)` — Conjuros de área: **Onda de trueno** (Mago, nivel 1) afecta a las
  criaturas a 2 casillas del lanzador; salvación de Constitución y 2d8 de trueno. `SpellTargeting`
  selecciona criaturas vivas en el cubo; `GameService` resuelve salvación y daño por objetivo.
  El menú muestra el conjuro como **área**.

- **D-20** `docs(architecture)` — Guardada la arquitectura de partidas autoalojadas
  host-autoritativas (`ARCHITECTURE_SELF_HOSTED.md`), con fases LAN → túnel → escritorio → código
  corto, huecos actuales del código, criterios de aceptación y riesgos. Añadidas U-08 a U-12 y
  ADR-009. **Sin implementación.**

## 2026-10-30

- **D-19** `feat(rules)` — Concentración: **Bendición** (Clérigo, nivel 1) da +1d4 a tiradas de
  ataque y salvaciones durante 10 rondas. El lanzador guarda `concentrationSpell/Rounds`; el buff
  vive en `blessed`/`blessedBy`; se rompe al recibir daño (salvación de CON CD 10 o mitad del daño)
  y al expirar. `SaveResolver` acepta bonificación plana; la UI muestra **Bendito** y
  **Conc. Bendición (rondas)**.

- **D-18** `feat(rules)` — Conjuros: catálogo por clase, espacios de nivel 1, ataques de conjuro,
  salvaciones y curación. Mago: Fuego feérico, Rayo de escarcha y Proyectil mágico; Clérigo:
  Llama sagrada, Curar heridas y Palabra curativa. `SaveResolver` y `Spellcasting` calculan DC,
  bono de ataque y espacios; `CombatService` comparte el pipeline de daño/muerte con las armas.
  UI: menú **Conjuros** con espacios, alcance y descripción.

- **D-17** `fix(db)` — Migraciones V8/V9 actualizan `npc_templates.portrait` y
  `board_entities.portrait` en campañas ya creadas para que apunten a `archivista.png`; V9 corrige
  las fichas ya materializadas. Verificado recargando la campaña de auditoría sin errores HTTP.

- **D-16** `fix(content)` — El retrato del PNJ generado apuntaba a `archivist.png`, pero el asset
  real es `archivista.png`; el `GET` devolvía 500 y la ficha quedaba sin imagen. Nuevo test
  `AdventureGeneratorAssetsTest` que verifica que los retratos materializados existen.

- **D-15** `feat(rules)` — Rasgos de clase con acción adicional: **Segundo aliento** (Guerrero,
  1d10 + nivel PG, 1 uso por combate), **Furia** (Bárbaro, +2 daño cuerpo a cuerpo, resistencia
  física, 10 rondas, 2 usos) y **Ataque furtivo** (Pícaro, +1d6 una vez por turno con ventaja o
  aliado adyacente). `DamageResolver` acepta bonificación y resistencia; la barra muestra la acción
  adicional y los botones de rasgo.

## 2026-10-29

- **D-14** `feat(rules)` — Cobertura 5e: `CoverRules` calcula la línea de visión en la rejilla
  (Bresenham). Muro o puerta cerrada = cobertura total; un obstáculo = media (+2 CA); dos o más =
  tres cuartos (+5 CA). `GameService` rechaza ataques contra cobertura total antes de gastar la
  acción y `CombatService` aplica la CA efectiva a ataques de jugador y de IA, registrando
  `cover`/`coverBonus` en el evento.

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
