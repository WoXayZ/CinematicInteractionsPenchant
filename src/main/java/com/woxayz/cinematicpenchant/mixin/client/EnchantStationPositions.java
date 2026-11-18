package com.woxayz.cinematicpenchant.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import dev.dyslexic.cinematic.client.EnchantStation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Cinematic Interactions 0.18.30.
 * {@code EnchantStation.lapisPosition} is package-private. The slot-1 book uses the same point as the local book and the observer lapis.
 */
@Mixin(EnchantStation.class)
public interface EnchantStationPositions {
    @Invoker("lapisPosition")
    static Vec3 cinematicpenchant$lapisPosition(BlockPos pos, Direction side) {
        throw new AssertionError();
    }
}
