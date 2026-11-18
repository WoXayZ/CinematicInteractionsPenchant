package com.woxayz.cinematicpenchant.mixin;

import archives.tater.penchant.menu.PenchantmentMenu;

import dev.dyslexic.cinematic.station.MenuActions;
import dev.dyslexic.cinematic.station.StationKind;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Cinematic Interactions 0.18.30.
 * Target: {@code MenuActions.isOpen(StationKind, AbstractContainerMenu)Z}.
 * The {@code (Sessions.Session, AbstractContainerMenu)} overload delegates here for every kind other than {@code ADDON}.
 * Vanilla {@code EnchantmentMenu} stays on the original switch.
 */
@Mixin(MenuActions.class)
public class MenuActionsIsOpenMixin {
    @Inject(method = "isOpen(Ldev/dyslexic/cinematic/station/StationKind;Lnet/minecraft/world/inventory/AbstractContainerMenu;)Z", at = @At("HEAD"), cancellable = true)
    private static void cinematicpenchant$acceptPenchantMenu(StationKind kind, AbstractContainerMenu menu, CallbackInfoReturnable<Boolean> cir) {
        if (kind == StationKind.ENCHANTING && menu instanceof PenchantmentMenu) {
            cir.setReturnValue(true);
        }
    }
}
