package com.dndmasters.ai;

import com.dndmasters.ai.dm.DmContext;
import com.dndmasters.ai.dm.DmResponse;
import com.dndmasters.ai.dm.DmActionValidator;
import com.dndmasters.util.Json;
import org.springframework.stereotype.Service;

/** Turns structured context into a validated {@link DmResponse}. */
@Service
public class DungeonMasterService {

    private static final String SYSTEM = """
            Eres el Dungeon Master de una partida de Dungeons & Dragons. Narra en español,
            en segunda persona del plural, con tono evocador pero conciso (2-4 frases, máx. 60 palabras).
            NO inventes resultados de dados ni modifiques el estado: solo narras y propones acciones.
            Responde SIEMPRE con un único objeto JSON válido y COMPLETO (cierra todas las llaves),
            sin texto antes ni después, con esta forma exacta:
            {
              "narration": "texto narrativo breve",
              "actions": [ { "type": "REQUEST_CHECK|START_COMBAT|SPAWN_ENEMY|SHIFT_SCENE|GRANT_REWARD|END_ADVENTURE|OPEN_SHOP|NARRATE",
                             "skill": "perception", "dc": 12, "sceneOrder": 1, "enemyName": null, "itemName": null } ],
              "sceneChanged": false,
              "imagePrompt": null
            }
            Usa REQUEST_CHECK solo cuando tenga sentido pedir una tirada. Habilidades válidas:
            perception, investigation, stealth, athletics, arcana, insight, persuasion, intimidation.
            Si el contexto incluye "spotlight" (el personaje al que le toca decidir), dirígete a él
            por su nombre y pregúntale qué quiere hacer, sin decidir por el jugador.
            Usa OPEN_SHOP SOLO cuando aparezca un mercader o el grupo busque comerciar.
            """;

    private final AIProvider provider;
    private final DmActionValidator validator;
    private final MockAIProvider fallback = new MockAIProvider();

    public DungeonMasterService(AIProvider provider, DmActionValidator validator) {
        this.provider = provider;
        this.validator = validator;
    }

    public String providerId() {
        if (provider instanceof OpenAiCompatibleProvider compatible) {
            return "opencode/" + compatible.models().get(0);
        }
        return provider.id();
    }

    public DmResponse respond(DmContext context) {
        DmResponse raw;
        try {
            AiRequest request = new AiRequest(SYSTEM, userPrompt(context), context.toMap(), 0.8, 900);
            raw = parse(provider.complete(request).text());
        } catch (Exception e) {
            // Never let an AI outage break the game: fall back to the offline script.
            raw = parse(fallback.complete(new AiRequest(SYSTEM, userPrompt(context), context.toMap(), 0.8, 700)).text());
        }
        return validator.sanitize(raw, context);
    }

    private String userPrompt(DmContext context) {
        return "Contexto estructurado de la partida (JSON):\n" + Json.write(context.toMap())
                + "\n\nTrigger: " + context.getTrigger()
                + (context.getLastAction() == null ? "" : "\nÚltima acción del jugador: " + context.getLastAction());
    }

    private DmResponse parse(String text) {
        if (text == null || text.isBlank()) {
            return DmResponse.narration("El Dungeon Master guarda silencio.");
        }
        String cleaned = text.trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start >= 0 && end > start) {
            try {
                return Json.read(cleaned.substring(start, end + 1), DmResponse.class);
            } catch (RuntimeException ignored) {
                // Fall through to the lenient recovery below.
            }
        }
        // Truncated or unbalanced JSON (e.g. finish_reason=length): pull out the narration.
        String narration = extractQuoted(cleaned, "narration");
        if (narration != null && !narration.isBlank()) {
            return DmResponse.narration(narration);
        }
        // No JSON at all: treat the whole text as narration, stripping fences if present.
        String plain = cleaned.replaceAll("```(?:json)?", "").replace("```", "").trim();
        return DmResponse.narration(plain.isBlank() ? "El Dungeon Master guarda silencio." : plain);
    }

    /** Recovers the value of a JSON string field even when the document is truncated. */
    private String extractQuoted(String text, String field) {
        String needle = "\"" + field + "\"";
        int key = text.indexOf(needle);
        if (key < 0) {
            return null;
        }
        int colon = text.indexOf(':', key + needle.length());
        if (colon < 0) {
            return null;
        }
        int open = text.indexOf('"', colon + 1);
        if (open < 0) {
            return null;
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = open + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                value.append(unescape(c));
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break; // End of the string value.
            } else {
                value.append(c);
            }
        }
        return value.toString();
    }

    private char unescape(char c) {
        return switch (c) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            default -> c;
        };
    }
}
