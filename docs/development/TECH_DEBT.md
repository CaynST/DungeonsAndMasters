# TECH_DEBT

Deuda consciente del prototipo, ordenada por impacto en la fidelidad con el PHB.

| ID | Deuda | Impacto | Prioridad | Remediación |
|----|-------|---------|-----------|-------------|
| R-02 | Acción adicional con Segundo aliento/Furia; faltan interacción gratuita y reacciones elegibles del jugador | Medio | P1 | Interacción de objeto y prompts de reacción |
| R-03 | Hechas Carrera/Retirada/Esquivar; faltan Ayudar/Ocultarse/Preparado/Buscar/Usar objeto | Medio | P1 | Enum de acciones + efectos por turno |
| R-04 | Condiciones implementadas; fuente de miedo/encanto, resistencia petrificada, nivel 6 de agotamiento y salvaciones FUE/DES automáticas quedan aproximados | Medio | P2 | Fuente por condición, resistencias y salvaciones genéricas |
| R-05 | Cobertura implementada para ataques; faltan salvaciones de Destreza y cobertura otorgada por criaturas | Bajo | P2 | Añadir salvaciones genéricas y criaturas como cobertura |
| R-08 | Terreno difícil se persiste y cobra doble, pero el tablero aún no lo dibuja y la IA (`moveToward`) cuenta casillas, no coste | Bajo | P2 | Render en frontend y coste en el movimiento automático |
| R-09 | Sorpresa implementada con acción del DM `SET_SURPRISED`; falta detección automática/guion y exposición en la UI | Bajo | P2 | Detección automática/guion y render del estado |
| R-10 | Conjuros con espacios, concentración, área y componentes materiales; los componentes V/S son descriptivos (sin silencio/manos libres) y faltan upcasting y más formas de área | Bajo | P2 | Silencio/manos, upcasting y áreas |
| R-11 | Hechos Segundo aliento, Furia, Ataque furtivo, Estilo de combate e Inspiración bárdica; faltan Canalizar divinidad, Gran arma/Protección, etc. | Medio | P1 | Rasgos restantes por clase/nivel |
| R-13 | Idiomas y competencias se muestran pero no se aplican mecánicamente; los personajes anteriores al cambio no tienen los campos en `creationData` | Bajo | P2 | Pruebas de herramienta/idioma y backfill al leer |
| R-14 | Sin descansos ni dados de golpe | Medio | P2 | Descanso corto/largo en el servicio |
| R-15 | El peso y la capacidad se muestran, pero la sobrecarga no modifica velocidad ni tiradas; el JSON antiguo sin peso carga como 0 y la munición no se consume | Bajo | P3 | R-18: consumo de munición y penalización de movimiento |
| U-02 | `Avatar` unificado en Home, Lobby, tarjetas e iniciativa; `CharacterSheet` (otro workstream), `CinematicIntro` y el fallback de `DmPanel` aún dibujan sus propias iniciales | Bajo | P2 | Reutilizar `components/ui/Avatar` cuando se retomen esas pantallas |

Arquitectura: el motor sigue siendo la autoridad; estas deudas se implementan en
`engine/` y se exponen por acciones, sin tocar el contrato del DM.
