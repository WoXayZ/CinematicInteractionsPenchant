package com.woxayz.cinematicpenchant.mixin;

import com.woxayz.cinematicpenchant.CinematicPenchant;

import archives.tater.penchant.menu.PenchantmentMenu;

import dev.dyslexic.cinematic.station.MenuActions;
import dev.dyslexic.cinematic.station.Sessions;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;

/**
 * Cinematic Interactions 0.18.30.
 * Target: {@code MenuActions.open(ServerPlayer, Sessions.Session)Z}.
 * {@code open} already called {@code player.openMenu} before this return.
 * {@code StationEvents.onRightClickBlock} cancels the block use, so Penchant's
 * {@code useWithoutItem} inject never runs and never calls {@code sendEnchantments}.
 */
@Mixin(MenuActions.class)
public class MenuActionsOpenMixin {
    @Inject(method = "open(Lnet/minecraft/server/level/ServerPlayer;Ldev/dyslexic/cinematic/station/Sessions$Session;)Z", at = @At("RETURN"))
    private static void cinematicpenchant$sendEnchantments(ServerPlayer player, Sessions.Session session, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (!(player.containerMenu instanceof PenchantmentMenu menu)) return;
        menu.sendEnchantments();
        CinematicPenchant.LOGGER.info(
                "Sent Penchant enchantments to {} at {} (bookshelf unlocks={}, books={}, disenchanter={})",
                player.getScoreboardName(),
                session.pos(),
                PenchantmentMenu.getUnlockedEnchantments(player.serverLevel(), session.pos()).size(),
                menu.getBookCount(),
                menu.hasDisenchanter());
    }
}
