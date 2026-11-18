package com.woxayz.cinematicpenchant.client;

import com.woxayz.cinematicpenchant.network.SlotBooks;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Cinematic Interactions drops {@code ClientEnchants} on chunk unload, level unload and logout.
 * The slot-1 book is not in that map, so it has to leave on the same three paths.
 */
public final class SlotBookEvents {
    private SlotBookEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SlotBookEvents::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(SlotBookEvents::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(SlotBookEvents::onLogout);
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) SlotBooks.unloadChunk(event.getChunk().getPos());
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) SlotBooks.clear();
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SlotBooks.clear();
    }
}
