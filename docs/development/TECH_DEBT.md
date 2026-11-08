# TECH_DEBT

Deuda consciente del prototipo, ordenada por impacto en la fidelidad con el PHB.

| ID | Deuda | Impacto | Prioridad | Remediación |
|----|-------|---------|-----------|-------------|
| R-02 | Acción adicional con Segundo aliento/Furia; faltan interacción gratuita y reacciones elegibles del jugador | Medio | P1 | Interacción de objeto y prompts de reacción |
| R-03 | Hechas Carrera/Retirada/Esquivar; faltan Ayudar/Ocultarse/Preparado/Buscar/Usar objeto | Medio | P1 | Enum de acciones + efectos por turno |
| R-04 | Condiciones implementadas; fuente de miedo/encanto, resistencia petrificada, nivel 6 de agotamiento y salvaciones FUE/DES automáticas quedan aproximados | Medio | P2 | Fuente por condición, resistencias y salvaciones genéricas |
| R-05 | Cobertura implementada para ataques; faltan salvaciones de Destreza y cobertura otorgada por criaturas | Bajo | P2 | Añadir salvaciones genéricas y criaturas como cobertura |
| R-10 | Conjuros con espacios, concentración, área y componentes materiales; los componentes V/S son descriptivos (sin silencio/manos libres) y faltan upcasting y más formas de área | Bajo | P2 | Silencio/manos, upcasting y áreas |
| R-11 | Hechos Segundo aliento, Furia, Ataque furtivo, Estilo de combate e Inspiración bárdica; faltan Canalizar divinidad, Gran arma/Protección, etc. | Medio | P1 | Rasgos restantes por clase/nivel |
| R-12 | 7 de 13 trasfondos PHB | Bajo | P2 | Añadir los 6 restantes al RuleSet |
| R-14 | Sin descansos ni dados de golpe | Medio | P2 | Descanso corto/largo en el servicio |

Arquitectura: el motor sigue siendo la autoridad; estas deudas se implementan en
`engine/` y se exponen por acciones, sin tocar el contrato del DM.
