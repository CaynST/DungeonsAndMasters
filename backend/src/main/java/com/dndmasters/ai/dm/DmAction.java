package com.dndmasters.ai.dm;

/** A structured action proposed by the DM. The game engine validates and applies it. */
public record DmAction(
        String type,
        String skill,
        Integer dc,
        Long entityId,
        String enemyName,
        Integer sceneOrder,
        String itemName,
        String location) {

    public static DmAction requestCheck(String skill, int dc) {
        return new DmAction("REQUEST_CHECK", skill, dc, null, null, null, null, null);
    }

    public static DmAction shiftScene(int order) {
        return new DmAction("SHIFT_SCENE", null, null, null, null, order, null, null);
    }

    public static DmAction startCombat() {
        return new DmAction("START_COMBAT", null, null, null, null, null, null, null);
    }

    public static DmAction grantReward(String item) {
        return new DmAction("GRANT_REWARD", null, null, null, null, null, item, null);
    }

    public static DmAction endAdventure() {
        return new DmAction("END_ADVENTURE", null, null, null, null, null, null, null);
    }

    public static DmAction openShop() {
        return new DmAction("OPEN_SHOP", null, null, null, null, null, null, null);
    }
}
