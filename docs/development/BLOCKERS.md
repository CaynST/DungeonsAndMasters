# BLOCKERS

## B-01 — Latencia de los modelos gratuitos/baratos
**Evidencia:** una llamada al DM puede tardar 30–120 s (crear campaña y arrancar la partida).
**Impacto:** la demo se siente lenta; no bloquea la funcionalidad.
**Mitigación aplicada:** fallback entre modelos y DM offline.
**Estado:** abierto (decisión de producto: elegir un modelo más rápido).

## B-02 — Razonamiento en eventos históricos
**Evidencia:** las narraciones guardadas antes del fix contienen cadena de pensamiento.
**Impacto:** solo en campañas antiguas.
**Mitigación:** las nuevas no lo incluyen; se puede empezar una campaña limpia.
**Estado:** aceptado.

## B-03 — Canalizar divinidad: decisión de producto (dominio y nivel 2)
**Evidencia:** los personajes del prototipo se crean a nivel 1 (`CharacterBuilder` fija el nivel y no
guarda subclase/dominio). Canalizar divinidad es un rasgo de **nivel 2** del clérigo y su efecto
depende del **dominio divino** (Expulsar muertos vivientes, Preservar vida, etc.), que tampoco se
elige ni se persiste en la creación.
**Impacto:** R-11 no puede cerrarse sin inventar dos decisiones de producto: (a) qué dominio(s)
existen y (b) si el prototipo admite personajes de nivel 2 con sus recursos (usos, descanso,
recuperación). Implementarlo ahora produciría una regla ficticia o un subsistema de nivel/dominio
fuera de alcance.
**Acciones intentadas:** se revisaron `CharacterBuilder`, `CharacterBuildRequest`, `RuleSet2014`,
`ClassFeatures` y la creación de personaje; no hay nivel 2, dominio ni recurso de Canalizar
divinidad.
**Decisión necesaria:** elegir dominio(s) y confirmar el soporte de nivel 2 (o aceptar que el
prototipo se queda en nivel 1 sin Canalizar divinidad). Recomendación: mantener nivel 1 y dejar el
rasgo fuera de la demo hasta que exista una historia de subida de nivel.
**Estado:** abierto (bloquea el resto de R-11).
