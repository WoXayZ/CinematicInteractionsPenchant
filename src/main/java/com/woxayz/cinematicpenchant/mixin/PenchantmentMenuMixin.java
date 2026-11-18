package com.woxayz.cinematicpenchant.mixin;

import archives.tater.penchant.menu.PenchantmentMenu;

import dev.dyslexic.cinematic.net.EnchantFxPayload;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.station.Sessions;
import dev.dyslexic.cinematic.station.StationKind;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ChunkPos;

/**
 * Penchant 1.21.1-1.8-fix3.
 * {@code handleEnchant(Holder)} is public. Copies taken at the start are compared at the return.
 * A refused enchant or extract leaves both slots unchanged and sends nothing.
 */
@Mixin(PenchantmentMenu.class)
public class PenchantmentMenuMixin {
    @Shadow
    @Final
    private Player player;

    @Unique
    private ItemStack cinematicpenchant$item = ItemStack.EMPTY;

    @Unique
    private ItemStack cinematicpenchant$ingredient = ItemStack.EMPTY;

    @Inject(method = "handleEnchant(Lnet/minecraft/core/Holder;)V", at = @At("HEAD"))
    private void cinematicpenchant$captureSlots(Holder<Enchantment> enchantment, CallbackInfo ci) {
        PenchantmentMenu menu = (PenchantmentMenu) (Object) this;
        cinematicpenchant$item = menu.getEnchantingStack().copy();
        cinematicpenchant$ingredient = menu.getIngredientStack().copy();
    }

    @Inject(method = "handleEnchant(Lnet/minecraft/core/Holder;)V", at = @At("RETURN"))
    private void cinematicpenchant$flashIfChanged(Holder<Enchantment> enchantment, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer server)) return;
        Sessions.Session session = Sessions.get(server);
        if (session == null || session.kind() != StationKind.ENCHANTING) return;
        PenchantmentMenu menu = (PenchantmentMenu) (Object) this;
        if (ItemStack.matches(cinematicpenchant$item, menu.getEnchantingStack())
                && ItemStack.matches(cinematicpenchant$ingredient, menu.getIngredientStack())) return;
        ServerLevel level = server.serverLevel();
        Network.sendToTrackingChunk(level, new ChunkPos(session.pos()), new EnchantFxPayload(session.pos(), server.getId()));
        double x = session.pos().getX() + 0.5;
        double y = session.pos().getY() + 1.1;
        double z = session.pos().getZ() + 0.5;
        level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 40, 0.3, 0.3, 0.3, 0.6);
    }
}
