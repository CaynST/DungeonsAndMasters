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

## ADR-010 — Áreas por radio Chebyshev centradas en el lanzador
**Contexto:** el tablero usa rejilla y la primera área del prototipo es un cubo de 15 pies centrado
en el lanzador; una geometría continua sería desproporcionada.
**Decisión:** `SpellTargeting.creaturesInCube` selecciona criaturas vivas a distancia Chebyshev
≤ radio, excluyendo lanzador y objetos. `Spell.Kind.AREA` usa `range` como radio.
**Consecuencia:** Onda de trueno es determinista y testeable; conos, cubos desplazados, línea de
visión y daño a aliados con reglas completas quedan como deuda documentada.

## ADR-011 — Componentes materiales mediante foco/bolsa
**Contexto:** R-10 pedía componentes; modelar manos libres, silencio y componentes verbales/somáticos
requiere un sistema de equipo y condiciones que el prototipo aún no tiene.
**Decisión:** metadata `components/material` en `Spell`; solo **M** se enforcea y se cubre con un
foco o bolsa. El inventario inicial de lanzadores incluye el foco; los tokens antiguos derivan el
foco de su personaje.
**Consecuencia:** la regla material es real y testeable sin bloquear a los personajes existentes.
V/S quedan descriptivos y se documentan en `TECH_DEBT.md`.

## ADR-012 — Estilo de combate como dato de personaje + reglas puras
**Contexto:** Defensa modifica la CA (que se recalcula al equipar), Arquería el ataque y Duelo el
daño; el cliente no debe calcular ninguna.
**Decisión:** `fightingStyle` se guarda en `creationData` y se copia a la entidad. `FightingStyleRules`
decide los bonos; `CharacterBuilder`/`EquipmentService` aplican Defensa y `CombatService` aplica
Arquería/Duelo. `Attack.twoHanded` permite excluir armas a dos manos de Duelo.
**Consecuencia:** el rasgo es testeable y sobrevive a cambios de equipo. Solo se ofrecen tres estilos;
Gran arma y Protección quedan como deuda.

## ADR-013 — Inspiración bárdica como dado preparado
**Contexto:** la inspiración 5e se decide antes o después del d20, pero el motor resuelve tiradas
automáticamente; no hay un prompt interactivo por tirada.
**Decisión:** el jugador usa `USE_INSPIRATION` para preparar el d6 y el motor lo suma y consume en
su próxima tirada de ataque, prueba o salvación. La inspiración es una acción adicional del bardo,
no consume acción del objetivo y dura 10 rondas (adaptación del prototipo).
**Consecuencia:** se conserva la agencia sin interrumpir cada tirada con un diálogo; no se puede
decidir después de ver el d20. La duración real de 10 minutos queda aproximada.

## ADR-014 — Sorpresa como bandera consumible por el flujo de turnos
**Contexto:** el PHB hace que una criatura sorprendida pierda su primer turno, pero el prototipo
no tiene una fase de detección ni un contrato del DM para declarar sorpresa.
**Alternativas:** añadir una columna a `combat_participants`; o guardar la bandera en `entity.data`
y consumirla desde `CombatService`.
**Decisión:** `SurpriseRules` gestiona `surprised` en `entity.data`; `CombatService` la consume al
abrir el combate y en cada avance de turno, registra `SURPRISED` y limpia la bandera una sola vez.
El DM la activa con la acción validada `SET_SURPRISED` sobre un `entityId` de la escena.
**Consecuencia:** la regla es determinista, persistente y testeable sin migración; la detección
automática o por guion y el render visual quedan como deuda.

## ADR-015 — Competencias e idiomas como metadata derivada de la creación
**Contexto:** el PHB 2014 define idiomas por especie y competencias de armadura/arma/herramienta
por clase y trasfondo. Añadir columnas nuevas exigiría migración; además el prototipo todavía no
aplica mecánicamente herramientas ni idiomas.
**Alternativas:** columnas dedicadas con migración; duplicar los datos en el frontend; no mostrarlos.
**Decisión:** `CharacterProficiencies` deriva listas de visualización a partir de los ids de especie,
clase y trasfondo, y `CharacterBuilder` las guarda en `creationData` (`languages`,
`armorProficiencies`, `weaponProficiencies`, `toolProficiencies`). La ficha las muestra; no hay
migración. En 2024 se usa la regla de creación de personaje (Común + dos idiomas a elección).
Los trasfondos nuevos se añaden solo a 2014 porque el SRD 5.2.1 no incluye sus equivalentes.
**Consecuencia:** la información está disponible sin tocar el esquema y es testeable como regla pura.
Los personajes creados antes de este cambio no tienen los campos, y las competencias siguen siendo
descriptivas hasta que existan pruebas de herramienta/idioma.

## ADR-016 — Listado paginado junto al contrato legacy de campañas
**Contexto:** la Home descargaba y renderizaba todas las campañas; con datos de prueba acumulados
la lista crecía sin límite. Borrar campañas no es aceptable porque son datos de usuario.
**Alternativas:** cambiar `GET /api/campaigns` para devolver siempre un sobre paginado (rompe a
los consumidores actuales); o añadir un endpoint dedicado manteniendo el array existente.
**Decisión:** `GET /api/campaigns/page?page=&size=&status=` devuelve un `CampaignPage` con
`content`, metadatos de paginación y filtro opcional por estado; `GET /api/campaigns` conserva el
array y los parámetros de paginación se validan (página ≥ 0, tamaño 1-100, estado conocido).
**Consecuencia:** la Home pagina y filtra en el servidor sin borrar datos y los clientes antiguos
siguen funcionando. El coste es un endpoint adicional y dos rutas que deben mantenerse alineadas.

## ADR-017 — Peso por unidad en el JSON de inventario, con carga solo informativa
**Contexto:** R-15 pide peso, capacidad y un aviso de sobrecarga sin romper personajes ya guardados
ni cambiar el combate. El inventario se persiste como `inventory_json`, sin columnas por objeto.
**Alternativas:** una tabla de inventario normalizada con migración; o un campo de peso en el JSON
existente. La primera es más rica, pero desproporcionada para el estado actual y arriesga datos.
**Decisión:** `InventoryItem.weight` es el peso de **una unidad** en libras y se multiplica por
`quantity`; el JSON ausente se lee como `0` y se conserva el constructor de cuatro argumentos.
`PlayerCharacter` deriva carga total y capacidad (`FUE × 15`) y las expone como campos calculados,
sin depender de `engine`. La sobrecarga se muestra en ficha y tienda, pero no modifica la velocidad.
El catálogo propaga el peso a las compras y los inventarios iniciales usan `quantity` para munición
(20 flechas) y herramientas.
**Consecuencia:** no hay migración ni cambios en el contrato de combate; los personajes antiguos
siguen cargando con peso `0` hasta que se edite su inventario. El consumo de munición y la
penalización de movimiento quedan explícitamente en R-18/TECH_DEBT.

## ADR-018 — Acciones R-02/R-03: banderas de un solo uso y Preparado mínimo
**Contexto:** faltaban Ayudar, Ocultarse, Preparado, Buscar, Usar objeto y la interacción gratuita;
también faltaba el ataque de oportunidad del jugador. Un modelo completo de disparadores de
reacción (elegir disparador y respuesta, ventanas de reacción, expiración exacta) sería un
subsistema nuevo y ambiguo para el alcance del prototipo.
**Alternativas:** (a) implementar un motor de disparadores genérico; (b) dejar Preparado como
bandera sin efecto; (c) aproximación acotada y documentada.
**Decisión:** (b+) aproximación acotada:
- **Ayudar**: consume la acción, exige un aliado vivo a 1 casilla y guarda `helpAdvantage` +
  `helpedBy` en el aliado. `CombatService` consume la bandera en el siguiente ataque y solo aplica
  ventaja si el objetivo está a 1 casilla del ayudante.
- **Ocultarse**: consume la acción; falla si hay un enemigo vivo adyacente. Marca `hidden` y da
  ventaja al siguiente ataque (sin tirada de Sigilo).
- **Preparado**: consume la acción y marca `readied`. Si un enemigo entra en el alcance de un
  jugador preparado, este hace un ataque cuerpo a cuerpo con su reacción, consumiendo `readied` y
  `reactionUsed`. La bandera expira al inicio del siguiente turno del jugador vía
  `TurnEconomy.reset`.
- **Buscar**: consume la acción, resuelve Percepción o Investigación con `CheckResolver` y registra
  el evento `SEARCH`.
- **Usar objeto**: consume la acción y usa un objeto no-poción del inventario del personaje con un
  efecto descriptivo acotado (antorcha, cuerda, raciones, herramientas, símbolo sagrado). Las
  pociones siguen por `USE_ITEM` para no duplicar la curación.
- **Interactuar**: nueva acción `INTERACT`, gratuita una vez por turno (`objectInteractionUsed`),
  sin consumir la acción; registra el evento y se reinicia en `TurnEconomy.reset`.
- **Ataque de oportunidad del jugador**: `opportunityAttacks` es simétrico; un enemigo que sale del
  alcance de un jugador provoca un ataque cuerpo a cuerpo con la reacción del jugador.
**Consecuencia:** las acciones son testeables y utilizables desde la UI sin inventar un motor de
reacciones. Quedan documentadas en `TECH_DEBT.md` las aproximaciones: Ocultarse sin Sigilo,
Preparado limitado a "atacar al entrar en alcance", la ventaja de Ayudar sin expiración temporal
exacta y efectos de objeto descriptivos.

## ADR-019 — Gran arma con repetición por dado y Protección como reacción automática
**Contexto:** R-11 pedía los estilos **Gran arma** y **Protección**. Gran arma repite los dados de
daño que muestran 1 o 2 con un arma cuerpo a cuerpo a dos manos; Protección impone desventaja, con
la reacción, a un ataque contra un aliado a 5 pies. El prototipo no tiene ventana de reacción ni
selección de objetivo de reacción.
**Alternativas:** (a) aproximar Gran arma sumando una media fija o repitiendo el total; (b) repetir
cada dado individualmente; (c) para Protección, pedir una reacción al jugador (requiere UI y turnos
suspendidos) o aplicarla automáticamente.
**Decisión:**
- **Gran arma** se implementa por **dado individual**: `DiceEngine.rollRerollingLowFaces` repite una
  sola vez cada 1 o 2 y conserva el resultado nuevo, aunque vuelva a ser bajo. Solo aplica a ataques
  de arma (`weaponAttack`), cuerpo a cuerpo y con `Attack.twoHanded`. En un crítico se mantiene la
  aproximación existente (se repite un conjunto de dados y se duplica el total), por lo que el bonus
  de la repetición también se duplica. El evento de ataque registra `greatWeaponFighting`,
  `greatWeaponRerolls` y `greatWeaponBonus`.
- **Protección** se aplica como **reacción automática acotada**: al atacarse a un jugador, se busca
  un protector jugador, vivo, consciente, con el estilo, a 1 casilla (5 pies) del objetivo y con
  `reactionUsed` libre; se elige el de menor `id` para que sea determinista. El protector gasta su
  reacción, el ataque recibe desventaja (se combina con ventaja/desventaja según 5e) y se registra
  un evento `PROTECTION`. El protector nunca es el atacante ni el objetivo.
**Consecuencia:** ambas reglas son testeables de extremo a extremo y visibles en el registro de
combate. Aproximaciones documentadas en `TECH_DEBT.md`: no hay elección del jugador (la reacción se
gasta siempre que sea elegible, incluso si el ataque ya tenía desventaja), no se comprueba línea de
visión ni sorpresa, y las armas versátiles no cambian de empuñadura.

## ADR-020 — Dados de golpe en `entity.data`, sin migración
**Contexto:** R-14 necesita dados de golpe por personaje, pero el prototipo no tiene columnas por
token y ya existen partidas con tokens creados antes de esta regla. Añadir columnas exigiría una
migración y un backfill; calcular solo para tokens nuevos dejaría incompatibles los existentes.
**Alternativas:** (a) columnas nuevas en `board_entities` con migración; (b) derivar siempre el dado
de la clase/nivel sin persistirlo; (c) persistir en `entity.data` con respaldos.
**Decisión:** (c) `hitDie`, `hitDiceMax` y `hitDiceRemaining` viven en `entity.data`.
`CampaignService` los inicializa al crear tokens desde `creationData.hitDie`; `RestRules` aplica
respaldos por clase SRD, nivel y `d8`. El descanso corto gasta exactamente un dado y el largo
recupera `max(restantes, mitad con mínimo 1)`. El descanso corto recarga Segundo aliento y no toca
espacios de conjuro; el largo limpia espacios, rasgos, concentración, inspiración y banderas de un
solo uso. La UI solo muestra y solicita; el motor decide.
**Consecuencia:** no hay migración ni cambios en el contrato del DM, y los tokens antiguos siguen
funcionando. Quedan como deuda el gasto de varios dados por descanso corto, la limpieza de
condiciones/efectos de terceros y el backfill persistente hasta el primer descanso.

## ADR-021 — Accesibilidad AA con foco global y patrones nativos
**Contexto:** la UI tenía foco inconsistente, controles solo-icono sin nombre, un modal de tienda
sin semántica de diálogo, pestañas de Admin sin roles y varios textos atenuados por debajo de 4.5:1.
Se pedía corregirlo sin rediseñar la dirección "mesa de taberna" ni tocar el VTT.
**Alternativas:** (a) sustituir controles por una librería de componentes accesibles; (b) revisar
pantalla a pantalla con estilos ad-hoc; (c) establecer una base compartida y aplicar patrones
nativos/ARIA en los puntos concretos.
**Decisión:** (c). Un anillo `:focus-visible` de latón y una regla `prefers-reduced-motion` en
`index.css`, un `<main>` con skip link en `App`, foco explícito en `Button`, `role="progressbar"`
en la barra de vida y patrones ARIA estándar donde el HTML nativo no basta (pestañas de Admin y
diálogo modal de tienda con foco atrapado, Escape y restauración). Los colores se ajustan a tokens
existentes (`crimson-soft`, `gold-soft`, `muted`) en lugar de introducir una paleta nueva.
**Consecuencia:** la base queda reutilizable y testeable sin añadir dependencias. Los avisos de
lint preexistentes y la cobertura de VTT/creación de personajes quedan fuera del alcance y
documentados en `TECH_DEBT.md`.

## ADR-022 — Munición de arcos y ballestas desde la ficha persistida
**Contexto:** R-18 pide que los ataques de arma a distancia de los jugadores consuman munición. El
prototipo guarda el inventario como JSON en `characters`, los tokens solo copian ataques y datos de
combate, y no existe una taxonomía genérica de objetos ni recuperación de munición. Los conjuros de
ataque reutilizan el pipeline de ataque y los enemigos llaman directamente a `CombatService`.
**Alternativas:** (a) modelar tipos de arma/objeto y munición genérica con migración; (b) mapear por
nombre de ataque con una tabla explícita y consumir desde `GameService`; (c) consumir dentro de
`CombatService` para todos los ataques de arma.
**Decisión:** (b) acotada:
- `AmmunitionRules` mapea **arcos** → `Flechas` y **ballestas** → `Virotes`, con alias español/inglés
  y comparación insensible a mayúsculas/acentos. Las armas arrojadizas (jabalina, daga), el cuerpo a
  cuerpo y los nombres de conjuros no consumen.
- El consumo ocurre en `GameService.attack` solo para tokens `PLAYER` con `refId`, tras validar
  objetivo/turno/acción/rango/cobertura y antes de marcar `actionUsed`. Sin munición se devuelve un
  `ActionResult` de error en español y no se resuelve el ataque.
- La cantidad se descuenta con `CharacterRepository` + `CharacterMapper`; al llegar a cero se elimina
  la línea. `GameState.party` relee la ficha, así que la UI se actualiza al refrescar el estado.
- `CombatService` no consume: los ataques de enemigos/aliados automáticos y los conjuros
  (`weaponAttack=false`) quedan fuera por diseño.
**Consecuencia:** la regla es testeable, no requiere migración y no altera energía de acción ni
conjuros. Quedan como deuda la recuperación de munición, el consumo de armas arrojadizas, los turnos
automáticos de aliados jugadores y la visualización explícita de munición en la barra de acción.
La penalización de carga en el movimiento se separa en R-18b.
