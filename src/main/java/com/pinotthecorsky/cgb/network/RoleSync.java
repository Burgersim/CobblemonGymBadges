package com.pinotthecorsky.cgb.network;

import com.pinotthecorsky.cgb.role.RoleManager;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RoleSync {
    public static void sendTo(ServerPlayer player) {
        Set<String> roles = RoleManager.getRolesFor(player);
        PacketDistributor.sendToPlayer(player, new RoleSyncPayload(roles));
    }

    private RoleSync() {
    }
}
