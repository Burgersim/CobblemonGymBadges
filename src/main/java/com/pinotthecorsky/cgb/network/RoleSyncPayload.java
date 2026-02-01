package com.pinotthecorsky.cgb.network;

import com.pinotthecorsky.cgb.CobblemonGymBadges;
import com.pinotthecorsky.cgb.role.ClientRoleData;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RoleSyncPayload(Set<String> roles) implements CustomPacketPayload {
    public static final Type<RoleSyncPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(CobblemonGymBadges.MODID, "role_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RoleSyncPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8),
        RoleSyncPayload::roles,
        RoleSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RoleSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientRoleData.setRoles(payload.roles()));
    }
}
