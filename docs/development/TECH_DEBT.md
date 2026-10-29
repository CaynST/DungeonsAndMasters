# TECH_DEBT

Deuda consciente del prototipo, ordenada por impacto en la fidelidad con el PHB.

| ID | Deuda | Impacto | Prioridad | Remediación |
|----|-------|---------|-----------|-------------|
| R-01 | Sin salvaciones contra muerte: a 0 PG se marca muerto | Alto | P1 | Estado `dying` + contadores de éxitos/fallos + estabilizar |
| R-02 | Solo acción + movimiento; sin acción adicional/reacción/ataques de oportunidad | Alto | P1 | Añadir contadores y desencadenantes; reacciones en el motor |
| R-03 | Faltan acciones de combate (Carrera, Retirada, Esquivar, Ayudar, Ocultarse, Preparado, Buscar, Usar objeto) | Alto | P1 | Enum de acciones + efectos por turno |
| R-04 | Sin condiciones (14 del Apéndice A) | Alto | P1 | Modelo de condiciones con duración y modificadores |
| R-05 | Sin cobertura | Medio | P1 | Cálculo por línea entre atacante/objetivo (+2/+5/total) |
| R-10 | Sin conjuros (trucos, espacios, concentración) | Alto | P1 | Catálogo mínimo + recursos por clase |
| R-11 | Sin rasgos de clase | Alto | P1 | Rasgos por clase/nivel con hooks en el motor |
| R-12 | 7 de 13 trasfondos PHB | Bajo | P2 | Añadir los 6 restantes al RuleSet |
| R-14 | Sin descansos ni dados de golpe | Medio | P2 | Descanso corto/largo en el servicio |

Arquitectura: el motor sigue siendo la autoridad; estas deudas se implementan en
`engine/` y se exponen por acciones, sin tocar el contrato del DM.
