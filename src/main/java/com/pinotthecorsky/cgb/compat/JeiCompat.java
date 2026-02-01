package com.pinotthecorsky.cgb.compat;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import net.neoforged.fml.ModList;

public final class JeiCompat {
    private static final String JEI_MODID = "jei";
    private static final String PLUGIN_CLASS = "com.pinotthecorsky.cgb.compat.jei.CgbJeiPlugin";

    public static void onRolesChanged() {
        if (!ModList.get().isLoaded(JEI_MODID)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(PLUGIN_CLASS);
            clazz.getMethod("onRolesChanged").invoke(null);
        } catch (ReflectiveOperationException exception) {
            CobblemonGymBadges.LOGGER.debug("Unable to notify JEI about role changes", exception);
        }
    }

    public static void onBadgesChanged() {
        if (!ModList.get().isLoaded(JEI_MODID)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(PLUGIN_CLASS);
            clazz.getMethod("onBadgesChanged").invoke(null);
        } catch (ReflectiveOperationException exception) {
            CobblemonGymBadges.LOGGER.debug("Unable to notify JEI about badge changes", exception);
        }
    }

    private JeiCompat() {
    }
}
