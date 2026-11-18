package com.woxayz.cinematicpenchant.mixin.client;

import com.woxayz.cinematicpenchant.client.scene.PenchantEnchantStation;

import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.EnchantStation;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.station.StationKind;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cinematic Interactions 0.18.30.
 * {@code Scene.create} is private and runs before any menu exists.
 * {@code EnchantRenderer} and {@code ClientPayloads.enchantFx} find the local table with
 * {@code Scene.station(EnchantStation.class)}. The wrapper is not an {@code EnchantStation},
 * so that lookup returns the station kept inside the wrapper.
 */
@Mixin(Scene.class)
public class SceneCreateMixin {
    @Inject(method = "create(Ldev/dyslexic/cinematic/station/StationKind;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/core/Direction;)Ldev/dyslexic/cinematic/client/ClientStation;", at = @At("RETURN"), cancellable = true)
    private static void cinematicpenchant$wrapEnchant(StationKind kind, BlockPos pos, BlockState state, LocalPlayer player, Direction side, CallbackInfoReturnable<ClientStation> cir) {
        if (kind == StationKind.ENCHANTING && cir.getReturnValue() instanceof EnchantStation enchant) {
            cir.setReturnValue(new PenchantEnchantStation(pos, side, enchant));
        }
    }

    @Inject(method = "station(Ljava/lang/Class;)Ldev/dyslexic/cinematic/client/ClientStation;", at = @At("HEAD"), cancellable = true)
    private static void cinematicpenchant$unwrapEnchant(Class<?> type, CallbackInfoReturnable<ClientStation> cir) {
        if (type != EnchantStation.class || !Scene.active()) return;
        if (Scene.station instanceof PenchantEnchantStation station) {
            cir.setReturnValue(station.enchantStation());
        }
    }
}
