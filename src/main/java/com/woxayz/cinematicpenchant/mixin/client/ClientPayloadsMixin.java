package com.woxayz.cinematicpenchant.mixin.client;

import com.woxayz.cinematicpenchant.client.scene.PenchantEnchantStation;

import dev.dyslexic.cinematic.client.ClientPayloads;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.net.EnchantFxPayload;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;

/**
 * Cinematic Interactions 0.18.30.
 * {@code enchantFx} skips {@code ClientEnchants.flash} for the player standing at the table,
 * because a vanilla {@code EnchantStation} already plays its ritual. A Penchant station has no ritual,
 * so the same payload starts its own flash. Observers still use the original path.
 */
@Mixin(ClientPayloads.class)
public class ClientPayloadsMixin {
    @Inject(method = "enchantFx(Ldev/dyslexic/cinematic/net/EnchantFxPayload;)V", at = @At("HEAD"))
    private static void cinematicpenchant$localFlash(EnchantFxPayload payload, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || payload.entityId() != minecraft.player.getId()) return;
        if (Scene.station instanceof PenchantEnchantStation station && station.drawsPenchant()) station.beginFlash();
    }
}
