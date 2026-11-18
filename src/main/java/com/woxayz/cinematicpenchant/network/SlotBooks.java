package com.woxayz.cinematicpenchant.network;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

/**
 * Latest slot-1 stack seen by this client for each enchanting table. Empty removes the entry.
 */
public final class SlotBooks {
    public record Shown(int side, ItemStack stack) {}

    private static final Map<BlockPos, Shown> BOOKS = new HashMap<>();

    private SlotBooks() {}

    public static void set(SlotBookPayload payload) {
        if (payload.book().isEmpty()) {
            BOOKS.remove(payload.pos());
            return;
        }
        BOOKS.put(payload.pos().immutable(), new Shown(payload.side(), payload.book().copy()));
    }

    public static void unloadChunk(ChunkPos chunk) {
        BOOKS.keySet().removeIf(pos -> pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z);
    }

    public static void clear() {
        BOOKS.clear();
    }

    public static Map<BlockPos, Shown> all() {
        return BOOKS;
    }
}
