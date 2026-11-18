package com.woxayz.cinematicpenchant.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.woxayz.cinematicpenchant.network.SlotBookPayload;

import archives.tater.penchant.menu.PenchantmentMenu;

import dev.dyslexic.cinematic.enchant.EnchantActions;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.station.Sessions;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

/**
 * Cinematic Interactions 0.18.30.
 * {@code EnchantActions.tick} returns immediately unless the menu is an {@code EnchantmentMenu}.
 * {@code show} is private. {@code SHOWN} is the same map {@code close} clears before sending an empty display,
 * so observers see the item and lapis leave when the session ends.
 * A book in slot 1 is not a lapis count, so it goes out on {@code cinematicpenchant:slot_book}.
 * {@code close} also sends an empty book, because {@code EnchantActions.close} does not know about that payload.
 */
@Mixin(EnchantActions.class)
public class EnchantActionsTickMixin {
    @Shadow
    @Final
    private static Map<UUID, Integer> SHOWN;

    @Shadow
    private static void show(ServerPlayer player, Sessions.Session session, ItemStack item, int lapis) {
    }

    @Unique
    private static final Map<UUID, Integer> cinematicpenchant$books = new HashMap<>();

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerPlayer;Ldev/dyslexic/cinematic/station/Sessions$Session;)V", at = @At("HEAD"), cancellable = true)
    private static void cinematicpenchant$showPenchant(ServerPlayer player, Sessions.Session session, CallbackInfo ci) {
        if (!(player.containerMenu instanceof PenchantmentMenu menu)) return;
        ItemStack item = menu.getEnchantingStack();
        int lapis = menu.isEnchanting() ? menu.getIngredientStack().getCount() : 0;
        int fingerprint = (item.isEmpty() ? 0 : ItemStack.hashItemAndComponents(item)) * 31 + lapis;
        Integer last = SHOWN.put(player.getUUID(), fingerprint);
        if (last == null || last != fingerprint) show(player, session, item, lapis);
        cinematicpenchant$sendBook(player, session, menu.isEnchanting() ? ItemStack.EMPTY : menu.getIngredientStack());
        ci.cancel();
    }

    @Inject(method = "close(Lnet/minecraft/server/level/ServerPlayer;Ldev/dyslexic/cinematic/station/Sessions$Session;)V", at = @At("RETURN"))
    private static void cinematicpenchant$clearBook(ServerPlayer player, Sessions.Session session, CallbackInfo ci) {
        cinematicpenchant$books.remove(player.getUUID());
        cinematicpenchant$send(player, session, ItemStack.EMPTY);
    }

    @Unique
    private static void cinematicpenchant$sendBook(ServerPlayer player, Sessions.Session session, ItemStack ingredient) {
        ItemStack book = ingredient.isEmpty() ? ItemStack.EMPTY : ingredient.copy();
        int fingerprint = book.isEmpty() ? 0 : ItemStack.hashItemAndComponents(book) * 31 + book.getCount();
        Integer last = cinematicpenchant$books.put(player.getUUID(), fingerprint);
        if (last == null || last != fingerprint) cinematicpenchant$send(player, session, book);
    }

    @Unique
    private static void cinematicpenchant$send(ServerPlayer player, Sessions.Session session, ItemStack book) {
        Network.sendToTrackingChunk(player.serverLevel(), new ChunkPos(session.pos()), new SlotBookPayload(session.pos(), session.side().get2DDataValue(), book));
    }
}
