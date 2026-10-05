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
            en segunda persona del plural, con tono evocador pero conciso (2-4 frases).
            NO inventes resultados de dados ni modifiques el estado: solo narras y propones acciones.
            Responde SIEMPRE con un único objeto JSON con esta forma exacta:
            {
              "narration": "texto narrativo",
              "actions": [ { "type": "REQUEST_CHECK|START_COMBAT|SPAWN_ENEMY|SHIFT_SCENE|GRANT_REWARD|END_ADVENTURE|NARRATE",
                             "skill": "perception", "dc": 12, "sceneOrder": 1, "enemyName": null, "itemName": null } ],
              "sceneChanged": false,
              "imagePrompt": null
            }
            Usa REQUEST_CHECK solo cuando tenga sentido pedir una tirada. Estilos válidos:
            perception, investigation, stealth, athletics, arcana, insight, persuasion, intimidation.
            """;

    private final AIProvider provider;
    private final DmActionValidator validator;
    private final MockAIProvider fallback = new MockAIProvider();

    public DungeonMasterService(AIProvider provider, DmActionValidator validator) {
        this.provider = provider;
        this.validator = validator;
    }

    public String providerId() {
        return provider.id();
    }

    public DmResponse respond(DmContext context) {
        DmResponse raw;
        try {
            AiRequest request = new AiRequest(SYSTEM, userPrompt(context), context.toMap(), 0.85, 700);
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
        if (text == null) {
            return DmResponse.narration("El Dungeon Master guarda silencio.");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return DmResponse.narration(text.trim());
        }
        return Json.read(text.substring(start, end + 1), DmResponse.class);
    }
}
