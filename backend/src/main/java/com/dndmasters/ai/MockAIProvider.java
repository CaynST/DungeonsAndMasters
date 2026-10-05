package com.dndmasters.ai;

import com.dndmasters.ai.dm.DmAction;
import com.dndmasters.ai.dm.DmResponse;
import com.dndmasters.util.Json;
import java.util.List;
import java.util.Map;

/**
 * Offline Dungeon Master. Implements the "La Cripta de Valdros" script as a small state
 * machine keyed by the structured context, so the whole one-shot plays without any API.
 */
public class MockAIProvider implements AIProvider {

    @Override
    public String id() {
        return "mock";
    }

    @Override
    public AiResponse complete(AiRequest request) {
        Map<String, Object> ctx = request.context();
        String trigger = str(ctx.get("trigger"));
        int sceneOrder = num(ctx.get("sceneOrder"));
        boolean checkSuccess = bool(ctx.get("checkSuccess"));
        String skill = str(ctx.get("skill"));

        DmResponse response = switch (trigger) {
            case "START" -> intro();
            case "DIALOGUE" -> sceneOrder == 0 ? dialogue() : narrate(sceneOrder, ctx);
            case "CHECK_RESULT" -> checkResult(sceneOrder, skill, checkSuccess);
            case "COMBAT_VICTORY" -> victory(sceneOrder);
            case "COMBAT_DEFEAT" -> defeat();
            default -> narrate(sceneOrder, ctx);
        };
        return new AiResponse(Json.write(response));
    }

    private DmResponse intro() {
        return DmResponse.of(
                "Las escaleras de Valdros descienden hacia un silencio húmedo. Antorchas moribundas dibujan sombras "
                        + "que parecen respirar. En la penumbra, una figura espectral hojea un tomo encadenado: el "
                        + "Archivista. «Habéis llegado... por fin. La cripta os esperaba.»",
                List.of(DmAction.requestCheck("perception", 10)));
    }

    private DmResponse dialogue() {
        return DmResponse.of(
                "El Archivista cierra el tomo con un golpe seco. «El Relicario duerme tras la puerta sellada, pero "
                        + "los goblins han profanado la antesala y el sello se debilita. Buscad la llave de hueso entre "
                        + "los archivos... si os atrevéis a mirar.»",
                List.of(DmAction.requestCheck("investigation", 14)));
    }

    private DmResponse checkResult(int sceneOrder, String skill, boolean success) {
        if (sceneOrder != 0) {
            return narrate(sceneOrder, Map.of());
        }
        if (!success) {
            return DmResponse.narration("Registráis los archivos sin éxito: el polvo os ciega y las letras bailan "
                    + "ante vuestros ojos. El Archivista deja escapar un suspiro impaciente. Quizá otra mirada lo logre.");
        }
        if ("investigation".equalsIgnoreCase(skill)) {
            return new DmResponse(
                    "Vuestra búsqueda da fruto: bajo un estante caído encontráis una llave tallada en hueso. La puerta "
                            + "sellada cede con un suspiro de polvo y un aliento frío sube desde la cripta profanada.",
                    List.of(DmAction.shiftScene(1), DmAction.startCombat()),
                    true,
                    "Sealed stone door opening, dust, ancient crypt beyond");
        }
        return DmResponse.narration("Notáis una corriente de aire frío que sale de una grieta tras el altar. Algo "
                + "vivo se mueve más abajo.");
    }

    private DmResponse victory(int sceneOrder) {
        if (sceneOrder == 1) {
            return new DmResponse(
                    "Sangre verde y huesos rotos. Los goblins yacen inertes, pero al fondo una puerta de ébano gotea "
                            + "una luz violeta. El sello del Guardián os llama.",
                    List.of(DmAction.shiftScene(2), DmAction.startCombat()),
                    true,
                    "Ancient crypt, defeated goblins, ebony door glowing violet");
        }
        if (sceneOrder == 2) {
            return new DmResponse(
                    "El Guardián se desmorona en un estruendo de placas y polvo. Sobre el pedestal, el Relicario de "
                            + "Valdros late con luz serena. La cripta vuelve a dormir... y vosotros habéis sobrevivido.",
                    List.of(DmAction.grantReward("Relicario de Valdros"), DmAction.endAdventure()),
                    true,
                    "Triumphant heroes holding a glowing silver relic in a quiet ancient crypt");
        }
        return DmResponse.narration("El último enemigo cae. El silencio regresa a la cripta.");
    }

    private DmResponse defeat() {
        return DmResponse.narration("La oscuridad os reclama. Vuestra antorcha se apaga y Valdros guarda vuestros "
                + "nombres en sus muros. Quizá otra compañía lo intente.");
    }

    private DmResponse narrate(int sceneOrder, Map<String, Object> ctx) {
        boolean combatActive = bool(ctx.get("combatActive"));
        if (sceneOrder == 0) {
            return DmResponse.narration("El aire huele a polvo y a siglos. El Archivista os observa sin levantar la "
                    + "vista del tomo, esperando vuestro siguiente movimiento.");
        }
        if (sceneOrder == 1) {
            return DmResponse.narration(combatActive
                    ? "Los goblins chillan y se abalanzan entre los huesos, buscando vuestra sangre."
                    : "La cripta profanada se extiende ante vosotros; los sarcófagos han sido saqueados.");
        }
        if (sceneOrder == 2) {
            return DmResponse.narration(combatActive
                    ? "El Guardián avanza con pasos de hierro; el aire se vuelve pesado como una losa."
                    : "El sello de Valdros pulsa en la oscuridad, vigilando el Relicario.");
        }
        return DmResponse.narration("El Dungeon Master aguarda vuestra decisión.");
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int num(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static boolean bool(Object value) {
        return value instanceof Boolean b && b;
    }
}
