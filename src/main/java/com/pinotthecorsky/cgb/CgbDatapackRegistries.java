package com.pinotthecorsky.cgb;

import com.pinotthecorsky.cgb.badge.BadgeDefinition;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

public final class CgbDatapackRegistries {
    public static void onRegisterDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CobblemonGymBadges.BADGE_REGISTRY_KEY, BadgeDefinition.CODEC, BadgeDefinition.CODEC);
    }

    private CgbDatapackRegistries() {
    }
}
