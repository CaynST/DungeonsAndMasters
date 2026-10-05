# AI Dungeon Master

El DM no es un chatbot: recibe el **estado estructurado** de la campaña y devuelve una
**respuesta estructurada**. El engine valida y aplica.

## Interfaz del proveedor

```java
public interface AIProvider {
    AiResponse complete(AiRequest request);
    String id();
}
```

Selección por entorno (`AiConfig`):

| `AI_PROVIDER` | Implementación | Base URL |
|---------------|----------------|----------|
| `mock` (default) | `MockAIProvider` — guion consciente del estado, sin red | — |
| `opencode-go` | `OpenAiCompatibleProvider` — OpenCode Go | `https://opencode.ai/zen/go/v1` |
| `opencode-zen` | `OpenAiCompatibleProvider` — OpenCode Zen | `https://opencode.ai/zen/v1` |
| `openai-compatible` | `OpenAiCompatibleProvider` — endpoint propio | `AI_BASE_URL` |

Si no hay `AI_API_KEY`, siempre se usa el Mock (la partida nunca se rompe por falta de API).

### OpenCode Go

```dotenv
AI_PROVIDER=opencode-go
AI_API_KEY=<tu clave de https://opencode.ai/auth>
AI_MODEL=mimo-v2.6-flash
AI_FALLBACK_MODELS=longcat-2.5-preview-free,space-bunny-free,muse-spark-1.3-contributor
```

- Modelo por defecto: **`mimo-v2.6-flash`** (barato, ~1M de contexto).
- **Cadena de fallback**: si un modelo falla (p. ej. 429 o restricción de región), se
  prueba el siguiente en orden. `muse-spark-1.3-contributor` exige activar
  "Allow paid endpoints that train on request data" en los ajustes de privacidad del
  workspace y una región soportada.
- Se envían los headers recomendados: `User-Agent` propio y `x-opencode-session`
  (este último es **obligatorio** para OpenCode Go).
- El `.env` se carga automáticamente al arrancar (ver README); las variables de entorno
  reales tienen prioridad y los tests nunca lo cargan.

### Robustez del parseo

Los modelos reales a veces devuelven JSON envuelto en ``` o truncado por `max_tokens`
(`finish_reason=length`). `DungeonMasterService.parse` recupera la narración incluso de un
JSON incompleto y, si no hay JSON, trata todo el texto como narración. Ante cualquier error
del proveedor se cae al `MockAIProvider`.

## Contexto de entrada (`DmContext`)

```json
{
  "campaign": { "id": 1, "name": "La Cripta de Valdros", "phase": "EXPLORATION" },
  "scene": { "id": 2, "title": "El Umbral", "imagePrompt": "Ancient crypt..." },
  "characters": [ { "id": 1, "name": "Kael", "hp": 28, "maxHp": 28, "ac": 16 } ],
  "npcs": [ { "id": 9, "name": "El Archivista" } ],
  "enemies": [ { "id": 21, "name": "Goblin", "hp": 7, "ac": 13 } ],
  "combat": { "active": false, "round": 0, "currentTurnEntityId": null },
  "currentTurn": null,
  "recentEvents": [ "El grupo entró en la cripta" ]
}
```

## Respuesta de salida (`DmResponse`)

```json
{
  "narration": "Los muros de Valdros respiran humedad...",
  "actions": [
    { "type": "REQUEST_CHECK", "skill": "investigation", "dc": 14 }
  ],
  "sceneChanged": false,
  "imagePrompt": null
}
```

Acciones admitidas: `NARRATE`, `REQUEST_CHECK`, `START_COMBAT`, `SPAWN_ENEMY`,
`SHIFT_SCENE`, `GRANT_REWARD`, `END_ADVENTURE`.

## Validación

`DmActionValidator` comprueba:

- entidades referenciadas existen en la escena;
- `dc` en rango 1..30;
- tipos de acción conocidos;
- no se puede `START_COMBAT` si ya hay combate activo, etc.

Una acción inválida se descarta con motivo; el engine nunca confía en el LLM.

## Mock inteligente

`MockAIProvider` implementa el guion del one-shot como una máquina de estados por
escena/fase, de modo que la demo completa es reproducible **sin API**. Esto es
deliberado: la experiencia no debe depender de un servicio externo. Con una clave de
OpenCode Go, el DM real narra libremente dentro del mismo contrato estructurado.

## Tests

`OpenAiCompatibleProviderTest` (cadena de fallback y errores), `AiConfigTest` (selección de
proveedor) y `DmResponseParsingTest` (JSON simple, con cercas, truncado y fallo del proveedor).
