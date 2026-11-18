package com.woxayz.cinematicpenchant.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

import com.woxayz.cinematicpenchant.client.scene.PenchantEnchantStation;
import com.woxayz.cinematicpenchant.network.SlotBooks;

import dev.dyslexic.cinematic.client.EnchantRenderer;
import dev.dyslexic.cinematic.client.EnchantStation;
import dev.dyslexic.cinematic.client.Scene;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Cinematic Interactions 0.18.30.
 * {@code EnchantRenderer.renderAll} draws the local table by calling {@code EnchantStation.renderScene}.
 * That method only reads an {@code EnchantmentMenu}. A {@link PenchantEnchantStation} draws its own slots instead.
 * After that pass, slot-1 books from {@code cinematicpenchant:slot_book} are drawn for every other table.
 */
@Mixin(EnchantRenderer.class)
public class EnchantRendererMixin {
    @Redirect(method = "renderAll", at = @At(value = "INVOKE", target = "Ldev/dyslexic/cinematic/client/EnchantStation;renderScene(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;I)V"))
    private static void cinematicpenchant$penchantMeshes(EnchantStation station, PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        if (Scene.station instanceof PenchantEnchantStation penchant && penchant.drawsPenchant()) {
            penchant.renderScene(pose, buffers, camera, light);
            return;
        }
        station.renderScene(pose, buffers, camera, light);
    }

    @Inject(method = "renderAll", at = @At("RETURN"))
    private static void cinematicpenchant$books(PoseStack pose, MultiBufferSource buffers, Camera camera, Frustum frustum, ClientLevel level, CallbackInfo ci) {
        if (SlotBooks.all().isEmpty()) return;
        Vec3 cam = camera.getPosition();
        EnchantStation mine = Scene.station(EnchantStation.class);
        for (var entry : new java.util.ArrayList<>(SlotBooks.all().entrySet())) {
            BlockPos pos = entry.getKey();
            if (mine != null && mine.pos.equals(pos) || pos.distToCenterSqr(cam) > 2304.0 || !level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).is(Blocks.ENCHANTING_TABLE)) {
                SlotBooks.all().remove(pos);
                continue;
            }
            if (!frustum.isVisible(new AABB(pos).inflate(0.5).expandTowards(0.0, 2.0, 0.0))) continue;
            var shown = entry.getValue();
            Direction side = Direction.from2DDataValue(shown.side());
            Vec3 at = PenchantEnchantStation.bookCorner(pos, side);
            PenchantEnchantStation.drawLaid(pose, buffers, cam, shown.stack(), at, side, net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos.above()));
        }
    }

    private static void cinematicpenchant$draw(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack item, Vec3 at, float spin, float size) {
        Minecraft minecraft = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(new Quaternionf().rotationY((float) Math.toRadians(spin)));
        boolean block = minecraft.getItemRenderer().getModel(item, minecraft.level, null, 0).isGui3d();
        float scale = block ? size * 1.4f : size;
        pose.scale(scale, scale, scale);
        minecraft.getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers, minecraft.level, 0);
        pose.popPose();
    }
}
