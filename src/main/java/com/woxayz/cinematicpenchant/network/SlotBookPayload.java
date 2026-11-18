package com.woxayz.cinematicpenchant.network;

import com.woxayz.cinematicpenchant.CinematicPenchant;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Slot 1 when it is not lapis. {@code EnchantDisplayPayload} can only carry a lapis count.
 */
public record SlotBookPayload(BlockPos pos, int side, ItemStack book) implements CustomPacketPayload {
    public static final Type<SlotBookPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CinematicPenchant.MOD_ID, "slot_book"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotBookPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.pos());
                buf.writeByte(payload.side());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.book());
            },
            buf -> new SlotBookPayload(BlockPos.STREAM_CODEC.decode(buf), buf.readByte(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf))
    );

    @Override
    public Type<? extends SlotBookPayload> type() {
        return TYPE;
    }
}
