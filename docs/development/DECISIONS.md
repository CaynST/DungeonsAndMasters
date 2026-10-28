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
