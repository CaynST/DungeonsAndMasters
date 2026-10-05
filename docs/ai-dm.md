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

| `AI_PROVIDER` | Implementación |
|---------------|----------------|
| `mock` (default) | `MockAIProvider` — guion consciente del estado, sin red |
| `openai-compatible` | `OpenAiCompatibleProvider` — usa `AI_BASE_URL`, `AI_API_KEY`, `AI_MODEL` |

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
deliberado: la experiencia no debe depender de un servicio externo.
