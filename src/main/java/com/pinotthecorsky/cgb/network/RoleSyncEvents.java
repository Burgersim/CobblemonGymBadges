package com.pinotthecorsky.cgb.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class RoleSyncEvents {
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RoleSync.sendTo(player);
        }
    }

    private RoleSyncEvents() {
    }
}
