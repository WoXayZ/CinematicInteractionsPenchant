package com.woxayz.cinematicpenchant.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CinematicPenchantNetworking {
    private CinematicPenchantNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SlotBookPayload.TYPE, SlotBookPayload.CODEC, (payload, context) -> context.enqueueWork(() -> SlotBooks.set(payload)));
    }
}
