package com.dndmasters.ai.dm;

import java.util.List;

/** Structured DM output: narration plus optional actions the engine may apply. */
public record DmResponse(
        String narration,
        List<DmAction> actions,
        boolean sceneChanged,
        String imagePrompt) {

    public static DmResponse narration(String narration) {
        return new DmResponse(narration, List.of(), false, null);
    }

    public static DmResponse of(String narration, List<DmAction> actions) {
        return new DmResponse(narration, actions, false, null);
    }
}
