package com.pinotthecorsky.cgb.compat;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import net.neoforged.fml.ModList;

public final class EmiCompat {
    private static final String EMI_MODID = "emi";
    private static final String PLUGIN_CLASS = "com.pinotthecorsky.cgb.compat.emi.CgbEmiPlugin";

    public static void onRecipesUpdated() {
        if (!ModList.get().isLoaded(EMI_MODID)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(PLUGIN_CLASS);
            clazz.getMethod("onRecipesUpdated").invoke(null);
        } catch (ReflectiveOperationException exception) {
            CobblemonGymBadges.LOGGER.debug("Unable to notify EMI about recipe updates", exception);
        }
    }

    public static void onRolesChanged() {
        if (!ModList.get().isLoaded(EMI_MODID)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(PLUGIN_CLASS);
            clazz.getMethod("onRolesChanged").invoke(null);
        } catch (ReflectiveOperationException exception) {
            CobblemonGymBadges.LOGGER.debug("Unable to notify EMI about role updates", exception);
        }
    }

    private EmiCompat() {
    }
}
