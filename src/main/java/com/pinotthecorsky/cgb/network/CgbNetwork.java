package com.pinotthecorsky.cgb.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CgbNetwork {
    public static final String NETWORK_VERSION = "1";

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(RoleSyncPayload.TYPE, RoleSyncPayload.STREAM_CODEC, RoleSyncPayload::handle);
    }

    private CgbNetwork() {
    }
}
