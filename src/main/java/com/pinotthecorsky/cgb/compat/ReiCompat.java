package com.pinotthecorsky.cgb.compat;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import net.neoforged.fml.ModList;

public final class ReiCompat {
    private static final String REI_MODID = "roughlyenoughitems";
    private static final String PLUGIN_CLASS = "com.pinotthecorsky.cgb.compat.rei.CgbReiPlugin";

    public static void onRecipesUpdated() {
        if (!ModList.get().isLoaded(REI_MODID)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(PLUGIN_CLASS);
            clazz.getMethod("onRecipesUpdated").invoke(null);
        } catch (ReflectiveOperationException exception) {
            CobblemonGymBadges.LOGGER.debug("Unable to notify REI about recipe updates", exception);
        }
    }

    public static void onRolesChanged() {
        if (!ModList.get().isLoaded(REI_MODID)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(PLUGIN_CLASS);
            clazz.getMethod("onRolesChanged").invoke(null);
        } catch (ReflectiveOperationException exception) {
            CobblemonGymBadges.LOGGER.debug("Unable to notify REI about role updates", exception);
        }
    }

    private ReiCompat() {
    }
}
