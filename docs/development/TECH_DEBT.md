# TECH_DEBT

Deuda consciente del prototipo, ordenada por impacto en la fidelidad con el PHB.

| ID | Deuda | Impacto | Prioridad | Remediación |
|----|-------|---------|-----------|-------------|
| R-02 | Interacción gratuita y ataques de oportunidad del jugador implementados; faltan prompts de reacción elegibles para conjuros/rasgos y una ventana de reacción fuera de las automáticas | Bajo | P2 | UI de reacción y modelo de disparadores |
| R-03 | Ayudar/Ocultarse/Preparado/Buscar/Usar objeto implementados con aproximaciones: Ocultarse no tira Sigilo, Preparado solo cubre "atacar al entrar en alcance", la ventaja de Ayudar caduca al usarse (no al inicio del turno del ayudante) y los objetos tienen efectos descriptivos acotados | Bajo | P2 | Sigilo, disparadores genéricos y catálogo de efectos de objeto |
| R-04 | Condiciones implementadas; fuente de miedo/encanto, resistencia petrificada, nivel 6 de agotamiento y salvaciones FUE/DES automáticas quedan aproximados | Medio | P2 | Fuente por condición, resistencias y salvaciones genéricas |
| R-05 | Cobertura implementada para ataques; faltan salvaciones de Destreza y cobertura otorgada por criaturas | Bajo | P2 | Añadir salvaciones genéricas y criaturas como cobertura |
| R-08 | Terreno difícil se persiste y cobra doble, pero el tablero aún no lo dibuja y la IA (`moveToward`) cuenta casillas, no coste | Bajo | P2 | Render en frontend y coste en el movimiento automático |
| R-09 | Sorpresa implementada con acción del DM `SET_SURPRISED`; falta detección automática/guion y exposición en la UI | Bajo | P2 | Detección automática/guion y render del estado |
| R-10 | Conjuros con espacios, concentración, área y componentes materiales; los componentes V/S son descriptivos (sin silencio/manos libres) y faltan upcasting y más formas de área | Bajo | P2 | Silencio/manos, upcasting y áreas |
| R-11 | Hechos Segundo aliento, Furia, Ataque furtivo, Estilo de combate (Defensa, Arquería, Duelo, Gran arma, Protección) e Inspiración bárdica; **falta Canalizar divinidad**, bloqueado por decisión de producto (dominio y nivel 2) | Medio | P1 | Rasgos restantes por clase/nivel |
| R-11b | Gran arma repite cada dado 1/2 y Protección se resuelve como reacción automática: no hay elección del jugador (se gasta siempre que es elegible), no se comprueba visión ni sorpresa y las armas versátiles no cambian de empuñadura | Bajo | P2 | Ventana de reacción, visión y empuñadura versátil |
| R-13 | Idiomas y competencias se muestran pero no se aplican mecánicamente; los personajes anteriores al cambio no tienen los campos en `creationData` | Bajo | P2 | Pruebas de herramienta/idioma y backfill al leer |
| R-14 | Descanso corto y largo implementados en `GameService` con dados de golpe en `entity.data`; el descanso corto gasta exactamente un dado (la especificación pedía uno), no elimina condiciones ni efectos de otros lanzadores, y los tokens antiguos derivan el dado de su clase/nivel | Bajo | P2 | Varios dados por descanso, limpieza de condiciones/efectos y backfill explícito al leer |
| R-18 | Munición implementada para ataques de arma a distancia de jugadores (arcos/ballestas) vía acción `ATTACK`; las armas arrojadizas no consumen ni se recuperan, los aliados jugadores controlados por IA atacan por `CombatService` sin consumir y no hay recuperación de flechas/virotes | Bajo | P3 | Consumo en turnos automáticos, armas arrojadizas/recuperación y munición visible en la UI |
| R-15 | El peso y la capacidad se muestran, pero la sobrecarga no modifica velocidad ni tiradas; el JSON antiguo sin peso carga como 0. La munición ya se consume desde R-18 | Bajo | P3 | R-18b: penalización de movimiento; backfill de peso |
| U-02 | `Avatar` unificado en Home, Lobby, tarjetas e iniciativa; `CharacterSheet` (otro workstream), `CinematicIntro` y el fallback de `DmPanel` aún dibujan sus propias iniciales | Bajo | P2 | Reutilizar `components/ui/Avatar` cuando se retomen esas pantallas |
| U-07A | El DM puede invocar enemigos del bestiario en la escena actual, pero no generar/cambiar escenas ni mapas (U-07B). La colocación no comprueba línea de visión, no crea `enemy_templates` de campaña ni enlaza `refId`, y no reparte XP al derrotar criaturas invocadas. Varias invocaciones del mismo monstruo comparten nombre. | Bajo | P2 | Generación de escenas, visión, plantilla/XP y nombres únicos o etiquetas |
| U-04 | Cobertura AA de las pantallas no-VTT: quedan sin revisar el VTT (`Game`, `Board`, `ActionBar`) y `CharacterCreation` (otros workstreams); no hay auditoría automática axe/Lighthouse en CI ni prueba con lector de pantalla real; las descripciones de inventario siguen dependiendo de `title` | Medio | P2 | Añadir `axe`/Lighthouse al pipeline, probar con NVDA/VoiceOver y exponer las descripciones con texto visible o `aria-describedby` |

Arquitectura: el motor sigue siendo la autoridad; estas deudas se implementan en
`engine/` y se exponen por acciones, sin tocar el contrato del DM.
