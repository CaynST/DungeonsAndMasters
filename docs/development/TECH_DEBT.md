# TECH_DEBT

Deuda consciente del prototipo, ordenada por impacto en la fidelidad con el PHB.

| ID | Deuda | Impacto | Prioridad | Remediación |
|----|-------|---------|-----------|-------------|
| R-02 | Reacción solo para oportunidad enemiga; sin acción adicional con habilidades ni interacción gratuita | Medio | P1 | Habilidades de acción adicional por clase y reacciones elegibles |
| R-03 | Hechas Carrera/Retirada/Esquivar; faltan Ayudar/Ocultarse/Preparado/Buscar/Usar objeto | Medio | P1 | Enum de acciones + efectos por turno |
| R-04 | Condiciones implementadas; fuente de miedo/encanto, resistencia petrificada, nivel 6 de agotamiento y salvaciones FUE/DES automáticas quedan aproximados | Medio | P2 | Fuente por condición, resistencias y salvaciones genéricas |
| R-05 | Cobertura implementada para ataques; faltan salvaciones de Destreza y cobertura otorgada por criaturas | Bajo | P2 | Añadir salvaciones genéricas y criaturas como cobertura |
| R-10 | Sin conjuros (trucos, espacios, concentración) | Alto | P1 | Catálogo mínimo + recursos por clase |
| R-11 | Sin rasgos de clase | Alto | P1 | Rasgos por clase/nivel con hooks en el motor |
| R-12 | 7 de 13 trasfondos PHB | Bajo | P2 | Añadir los 6 restantes al RuleSet |
| R-14 | Sin descansos ni dados de golpe | Medio | P2 | Descanso corto/largo en el servicio |

Arquitectura: el motor sigue siendo la autoridad; estas deudas se implementan en
`engine/` y se exponen por acciones, sin tocar el contrato del DM.
