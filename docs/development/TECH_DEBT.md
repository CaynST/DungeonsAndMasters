# TECH_DEBT

Deuda consciente del prototipo, ordenada por impacto en la fidelidad con el PHB.

| ID | Deuda | Impacto | Prioridad | Remediación |
|----|-------|---------|-----------|-------------|
| R-02 | Reacción solo para oportunidad enemiga; sin acción adicional con habilidades ni interacción gratuita | Medio | P1 | Habilidades de acción adicional por clase y reacciones elegibles |
| R-03 | Hechas Carrera/Retirada/Esquivar; faltan Ayudar/Ocultarse/Preparado/Buscar/Usar objeto | Medio | P1 | Enum de acciones + efectos por turno |
| R-04 | Sin condiciones (14 del Apéndice A) | Alto | P1 | Modelo de condiciones con duración y modificadores |
| R-05 | Sin cobertura | Medio | P1 | Cálculo por línea entre atacante/objetivo (+2/+5/total) |
| R-10 | Sin conjuros (trucos, espacios, concentración) | Alto | P1 | Catálogo mínimo + recursos por clase |
| R-11 | Sin rasgos de clase | Alto | P1 | Rasgos por clase/nivel con hooks en el motor |
| R-12 | 7 de 13 trasfondos PHB | Bajo | P2 | Añadir los 6 restantes al RuleSet |
| R-14 | Sin descansos ni dados de golpe | Medio | P2 | Descanso corto/largo en el servicio |

Arquitectura: el motor sigue siendo la autoridad; estas deudas se implementan en
`engine/` y se exponen por acciones, sin tocar el contrato del DM.
