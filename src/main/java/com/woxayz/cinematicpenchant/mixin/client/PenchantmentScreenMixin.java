package com.woxayz.cinematicpenchant.mixin.client;

import archives.tater.penchant.client.gui.screen.PenchantmentScreen;
import archives.tater.penchant.menu.PenchantmentMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Cinematic Interactions 0.18.30 cancels {@code PenchantmentScreen} before {@code Screen.init(Minecraft, int, int)}.
 * The constructor has already stored {@code rebuildWidgets} as the slot listener. The container packet that follows
 * calls it, and {@code init} reads a client that is still null.
 * The listener is put back at the end of {@code init}, which only runs when the screen is actually shown.
 */
@Mixin(PenchantmentScreen.class)
public abstract class PenchantmentScreenMixin extends AbstractContainerScreen<PenchantmentMenu> {
    protected PenchantmentScreenMixin(PenchantmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cinematicpenchant$dropListener(PenchantmentMenu menu, Inventory playerInventory, Component title, CallbackInfo ci) {
        menu.setSlotChangeListener(() -> {});
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    private void cinematicpenchant$listenOnceShown(CallbackInfo ci) {
        this.menu.setSlotChangeListener(this::rebuildWidgets);
    }
}
