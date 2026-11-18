package com.woxayz.cinematicpenchant.client.scene;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

/**
 * Wireframe gem over the empty lapis socket, as Cinematic Interactions 0.18.2 draws it ({@code EnchantFx.lapisGhost}).
 * Cinematic Interactions 0.18.30 no longer ships that method.
 */
final class LapisGhost {
    private static final double[][] GEM = {{0.0, 0.052}, {0.035, 0.018}, {0.023, -0.032}, {-0.023, -0.032}, {-0.035, 0.018}};
    private static final int[][] WINDINGS = {{0, 1, 2, 3}, {3, 2, 1, 0}};

    private LapisGhost() {
    }

    static final float[] BLUE = {0.25f, 0.62f, 1.0f};
    static final float[] BROWN = {0.85f, 0.52f, 0.22f};

    static void draw(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 at, float brightness, float[] tint) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        for (int i = 0; i < GEM.length; i++) {
            double[] a = GEM[i];
            double[] b = GEM[(i + 1) % GEM.length];
            line(vc, pose.last(), camera, at, a[0], a[1], b[0], b[1], brightness, tint);
        }
        line(vc, pose.last(), camera, at, -0.035, 0.018, 0.035, 0.018, brightness * 0.65f, tint);
        line(vc, pose.last(), camera, at, 0.0, 0.052, -0.023, -0.032, brightness * 0.65f, tint);
        line(vc, pose.last(), camera, at, 0.0, 0.052, 0.023, -0.032, brightness * 0.65f, tint);
    }

    private static void line(VertexConsumer vc, PoseStack.Pose pose, Camera camera, Vec3 at, double x0, double y0, double x1, double y1, float brightness, float[] tint) {
        double length = Math.hypot(x1 - x0, y1 - y0);
        double dx = -(y1 - y0) / length * 0.0025;
        double dy = (x1 - x0) / length * 0.0025;
        for (int edge = -1; edge <= 1; edge += 2) {
            double[][] ribbon = {{x0, y0}, {x1, y1}, {x1 + dx * edge, y1 + dy * edge}, {x0 + dx * edge, y0 + dy * edge}};
            for (int[] winding : WINDINGS) {
                for (int vertex : winding) {
                    vertex(vc, pose, camera, at, ribbon[vertex][0], ribbon[vertex][1], brightness, tint, vertex < 2 ? 190 : 0);
                }
            }
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Camera camera, Vec3 at, double x, double y, float brightness, float[] tint, int alpha) {
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Vec3 cam = camera.getPosition();
        vc.addVertex(pose, (float) (at.x + left.x() * x + up.x() * y - cam.x), (float) (at.y + left.y() * x + up.y() * y - cam.y), (float) (at.z + left.z() * x + up.z() * y - cam.z))
                .setColor(clamp(tint[0] * brightness), clamp(tint[1] * brightness), clamp(tint[2] * brightness), alpha);
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255.0f)));
    }
}
