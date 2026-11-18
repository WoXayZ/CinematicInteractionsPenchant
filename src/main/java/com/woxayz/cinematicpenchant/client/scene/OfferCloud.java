package com.woxayz.cinematicpenchant.client.scene;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import archives.tater.penchant.client.gui.widget.EnchantmentSlotWidget;
import archives.tater.penchant.menu.PenchantmentMenu;
import archives.tater.penchant.util.PenchantmentHelper;

import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

/**
 * Penchant offers drawn as Cinematic Interactions sigils.
 * Three sit where the vanilla offers sit. More of them shrink and spread around the table.
 */
final class OfferCloud {
    private static final int CREAM = -725249;
    private static final int RED_NUMBER = -38037;
    private static final int RUNE = -4610305;
    private static final int RUNE_DIM = -8762795;
    private static final int GEMS = -10778881;
    private static final int GRAY = 0xFFB7B7B7;

    private final List<Offer> offers = new ArrayList<>();
    private int fingerprint = Integer.MIN_VALUE;
    private int hover = -1;
    private Vec3 hoveredAt;
    private int deny = -1;
    private double denyAt = -10;
    private double shownAt = -10;
    private Offer ritual;
    private Vec3 ritualFrom;
    private double ritualAt = -10;
    private boolean burstDone;

    int count() {
        return offers.size();
    }

    boolean busy() {
        return ritualTime() >= 0.0;
    }

    double ritualTime() {
        double t = (Scene.clock() - ritualAt) / 1.5;
        return t >= 0.0 && t <= 1.0 ? t : -1.0;
    }

    int hover() {
        return hover;
    }

    void clearAim() {
        hover = -1;
    }

    void update(PenchantmentMenu menu) {
        int next = fingerprint(menu);
        if (next == fingerprint) return;
        fingerprint = next;
        boolean fresh = offers.isEmpty();
        offers.clear();
        hover = -1;
        if (fresh) shownAt = Scene.clock();
        if (!menu.isEnchanting() && !menu.isDisenchanting()) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        boolean creative = minecraft.player.isCreative();
        var registries = minecraft.level.registryAccess();
        boolean disenchanting = menu.isDisenchanting();
        var stack = disenchanting ? menu.getIngredientStack() : menu.getEnchantingStack();
        for (var enchantment : menu.getDisplayedEnchantments()) {
            if (!disenchanting && PenchantmentHelper.hasEnchantment(menu.getEnchantingStack(), enchantment)) continue;
            var incompatible = incompatible(stack, enchantment, registries);
            EnchantmentSlotWidget rules = disenchanting
                    ? new EnchantmentSlotWidget(0, 0, enchantment, incompatible, !PenchantmentHelper.hasEnchantment(menu.getIngredientStack(), enchantment), creative || PenchantmentHelper.getBookRequirement(enchantment) <= menu.getBookCount())
                    : new EnchantmentSlotWidget(0, 0, enchantment, incompatible,
                            PenchantmentHelper.canEnchant(stack, enchantment, registries),
                            PenchantmentHelper.hasEnchantment(stack, enchantment),
                            creative || !menu.getIngredientStack().isEmpty(),
                            creative || PenchantmentHelper.getBookRequirement(enchantment) <= menu.getBookCount(),
                            creative || PenchantmentHelper.getXpLevelCost(enchantment) <= menu.getPlayerXp(),
                            creative || menu.isAvailable(enchantment));
            boolean locked = !creative && !menu.isAvailable(enchantment) && !disenchanting;
            offers.add(new Offer(enchantment, rules, PenchantmentHelper.getXpLevelCost(enchantment), PenchantmentHelper.getBookRequirement(enchantment), locked, disenchanting));
        }
    }

    boolean pick(double[] mouse, BlockPos pos, Vec3 across, Vec3 out) {
        int previous = hover;
        hover = -1;
        double best = Double.MAX_VALUE;
        float fit = fit();
        double radius = Math.max(0.09, 0.15 * fit);
        for (int i = 0; i < offers.size(); i++) {
            Vec3 at = position(i, pos, across, out);
            if (!under(mouse, at, radius)) continue;
            double[] gui = Picking.toGui(at);
            if (gui == null) continue;
            double distance = Math.hypot(mouse[0] - gui[0], mouse[1] - gui[1]);
            if (distance >= best) continue;
            best = distance;
            hover = i;
            hoveredAt = at;
        }
        if (hover != previous && hover >= 0) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.35f, 0.9f + (hover % 5) * 0.05f);
                player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.2f, 1.5f + (hover % 4) * 0.08f);
            }
        }
        return hover >= 0;
    }

    Vec3 aim(BlockPos pos, Vec3 across, Vec3 out) {
        return hover < 0 ? null : position(hover, pos, across, out);
    }

    boolean press() {
        if (hover < 0 || hover >= offers.size()) return false;
        var offer = offers.get(hover);
        if (offer.rules.active && hoveredAt != null) {
            ritual = offer;
            ritualFrom = hoveredAt;
            ritualAt = Scene.clock();
            burstDone = false;
            offer.rules.onPress();
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.playSound(SoundEvents.BEACON_ACTIVATE, 0.45f, 1.7f);
                player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 0.9f);
            }
            return true;
        }
        deny = hover;
        denyAt = Scene.clock();
        return false;
    }

    List<Component> tooltip() {
        if (hover < 0 || hover >= offers.size()) return null;
        var offer = offers.get(hover);
        if (offer.locked) {
            var shown = offer.enchantment.value().description().getString();
            shown = shown.substring(0, shown.length() * 2 / 3).trim();
            return List.of(
                    Component.literal(shown).withStyle(ChatFormatting.GRAY).append(Component.translatable("widget.penchant.enchantment_slot.tooltip.name_locked")),
                    Component.translatable("widget.penchant.enchantment_slot.tooltip.locked").withStyle(ChatFormatting.RED)
            );
        }
        var lines = new ArrayList<Component>();
        lines.add(offer.disenchant
                ? Component.translatable("widget.penchant.enchantment_slot.tooltip.remove", offer.enchantment.value().description())
                : PenchantmentHelper.getName(offer.enchantment));
        lines.add(Component.translatable("widget.penchant.enchantment_slot.tooltip.book_requirement", offer.books)
                .withColor(offer.rules.active ? EnchantmentSlotWidget.BOOK_COLOR : EnchantmentSlotWidget.INSUFFICIENT_COLOR));
        if (!offer.disenchant) {
            lines.add(Component.translatable("widget.penchant.enchantment_slot.tooltip.xp_cost", offer.xp)
                    .withColor(offer.rules.active ? EnchantmentSlotWidget.XP_COLOR : EnchantmentSlotWidget.INSUFFICIENT_COLOR));
        }
        return lines;
    }

    void render(PoseStack pose, MultiBufferSource buffers, Camera camera, BlockPos pos, Vec3 across, Vec3 out, Vec3 itemAt) {
        float fit = fit();
        float appear = (float) Math.min(1.0, Math.max(0.0, (Scene.clock() - shownAt) / 0.4));
        if (appear > 0.01f) {
            for (int i = 0; i < offers.size(); i++) {
                if (ritual != null && ritualTime() >= 0.0 && offers.get(i).enchantment().equals(ritual.enchantment())) continue;
                draw(pose, buffers, camera, offers.get(i), position(i, pos, across, out), i, appear, fit, i == hover);
            }
        }
        double t = ritualTime();
        if (t < 0.0 || ritual == null || ritualFrom == null) return;
        double flight = inOut(Math.min(1.0, t * 1.5 / 0.45));
        if (flight >= 1.0) return;
        Vec3 at = lerp(ritualFrom, itemAt, flight).add(0.0, 0.12 * Math.sin(Math.PI * flight), 0.0);
        draw(pose, buffers, camera, ritual, at, 0, (float) (1.0 - 0.8 * flight), 1.15f, true);
    }

    boolean tickRitual(Vec3 itemAt) {
        double t = ritualTime();
        if (t < 0.0 || ritualFrom == null) return false;
        if (t * 1.5 < 0.5) {
            EnchantFx.stream(ritualFrom, itemAt, 1);
            EnchantFx.gather(itemAt, 0.55);
            return false;
        }
        if (burstDone) return false;
        burstDone = true;
        EnchantFx.burst(itemAt, 1.4);
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.3f);
            player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
        }
        return true;
    }

    private float fit() {
        int count = offers.size();
        if (count <= 3) return 1.0f;
        return (float) Math.max(0.42, Math.min(0.82, Math.sqrt(3.0 / count)));
    }

    private Vec3 position(int index, BlockPos pos, Vec3 across, Vec3 out) {
        int count = offers.size();
        double bob = 0.015 * Math.sin(Scene.clock() * 1.3 + index * 2.1);
        Vec3 origin = Vec3.atBottomCenterOf(pos).add(0.0, 0.75, 0.0);
        int visual = count == 1 ? 1 : count == 2 ? (index == 0 ? 0 : 2) : index;
        if (count <= 3) {
            int off = visual - 1;
            return shaken(index, origin.add(0.0, (off == 0 ? 0.92 : 0.74) + bob, 0.0).add(out.scale(off == 0 ? -0.12 : 0.02)).add(across.scale(off * 0.4)), across);
        }
        int columns = Math.max(3, (int) Math.ceil(Math.sqrt(count * 2.4)));
        int rows = (int) Math.ceil(count / (double) columns);
        int row = index / columns;
        int col = index % columns;
        int inRow = row == rows - 1 ? count - row * columns : columns;
        double xGap = Math.min(0.4, 1.9 / Math.max(1, columns - 1));
        double x = (col - (inRow - 1) / 2.0) * xGap;
        if ((row & 1) == 1) x += xGap * 0.35;
        double span = rows <= 1 ? 0.0 : Math.min(0.34, 0.16 * (rows - 1));
        double y = 0.92 + span / 2.0 - (rows <= 1 ? 0.0 : row * span / (rows - 1));
        double z = -0.02 + (row - (rows - 1) / 2.0) * 0.04;
        return shaken(index, origin.add(0.0, y + bob, 0.0).add(across.scale(x)).add(out.scale(z)), across);
    }

    private Vec3 shaken(int index, Vec3 at, Vec3 across) {
        if (index != deny) return at;
        double elapsed = Scene.clock() - denyAt;
        if (elapsed < 0.0 || elapsed >= 0.35) return at;
        return at.add(across.scale(0.02 * Math.sin(elapsed * 60.0) * (1.0 - elapsed / 0.35)));
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, Camera camera, Offer offer, Vec3 at, int index, float appear, float fit, boolean hovered) {
        double clock = Scene.clock();
        boolean usable = offer.rules.active;
        float size = (float) (0.13 * appear * (hovered ? 1.2 : 1.0) * fit);
        float brightness = offer.locked ? 0.38f : 1.0f;
        float b = brightness * (hovered ? 1.35f : 0.85f) * Math.min(1.0f, appear);
        float red = offer.locked ? 0.55f : usable ? 0.55f : 0.5f;
        float green = offer.locked ? 0.55f : usable ? 0.45f : 0.12f;
        float blue = offer.locked ? 0.62f : usable ? 1.0f : 0.16f;
        EnchantFx.billboard(pose, buffers, camera, at, size * 1.8, 0.0, EnchantFx.GLOW, red * b * 0.35f, green * b * 0.35f, blue * b * 0.35f);
        double turn = clock * (hovered ? 1.8 : 0.5) * (index % 2 == 0 ? 1 : -1);
        if (offer.locked) {
            float gray = 0.5f * b;
            EnchantFx.billboard(pose, buffers, camera, at, size, turn, EnchantFx.PIXEL_CIRCLE, gray, gray, gray);
        } else {
            var palette = usable ? EnchantFx.CirclePalette.VIOLET : EnchantFx.CirclePalette.RED;
            EnchantFx.circleBillboard(pose, buffers, camera, at, size, turn, palette, b);
        }
        var font = Minecraft.getInstance().font;
        Vec3 cam = camera.getPosition();
        var look = camera.getLookVector();
        Vec3 text = at.subtract(look.x() * 0.01, look.y() * 0.01, look.z() * 0.01);
        pose.pushPose();
        pose.translate(text.x - cam.x, text.y - cam.y, text.z - cam.z);
        pose.mulPose(camera.rotation());
        float scale = 0.01f * appear * (hovered ? 1.2f : 1.0f) * fit;
        pose.scale(scale, -scale, scale);
        String number = Integer.toString(offer.disenchant ? offer.books : offer.xp);
        int numberColor = offer.locked ? GRAY : usable ? CREAM : RED_NUMBER;
        font.drawInBatch(number, -font.width(number) / 2.0f, -4.5f, numberColor, true, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        var name = offer.name();
        float nameScale = 0.55f;
        int nameWidth = font.width(name);
        if (nameWidth > 0) nameScale = Math.min(nameScale, 78.0f / nameWidth);
        pose.pushPose();
        pose.scale(nameScale, nameScale, nameScale);
        int nameColor = offer.locked ? GRAY : usable ? RUNE : RUNE_DIM;
        float nameY = 26.0f * 0.55f / nameScale;
        font.drawInBatch(name, -nameWidth / 2.0f, nameY, nameColor, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        if (!offer.disenchant && offer.books > 0) {
            String gems = "\u25c6 " + offer.books;
            int gemColor = offer.locked ? GRAY : usable ? GEMS : RED_NUMBER;
            font.drawInBatch(gems, -font.width(gems) / 2.0f, nameY + 12.0f, gemColor, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
        pose.popPose();
        pose.popPose();
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, double t) {
        return new Vec3(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t, from.z + (to.z - from.z) * t);
    }

    private static double inOut(double t) {
        return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
    }

    private static boolean under(double[] mouse, Vec3 center, double radius) {
        double[] at = Picking.toGui(center);
        if (at == null) return false;
        double[] edge = Picking.toGui(center.add(0.0, radius, 0.0));
        double pixels = edge == null ? 8.0 : Math.max(8.0, Math.hypot(at[0] - edge[0], at[1] - edge[1]));
        return Math.hypot(mouse[0] - at[0], mouse[1] - at[1]) < pixels;
    }

    private static int fingerprint(PenchantmentMenu menu) {
        int value = menu.getBookCount();
        value = value * 31 + menu.getPlayerXp();
        value = value * 31 + (menu.isEnchanting() ? 1 : 0);
        value = value * 31 + (menu.isDisenchanting() ? 1 : 0);
        value = value * 31 + ItemStack.hashItemAndComponents(menu.getEnchantingStack());
        value = value * 31 + menu.getEnchantingStack().getCount();
        value = value * 31 + ItemStack.hashItemAndComponents(menu.getIngredientStack());
        value = value * 31 + menu.getIngredientStack().getCount();
        for (var enchantment : menu.getDisplayedEnchantments()) value = value * 31 + System.identityHashCode(enchantment);
        return value;
    }

    private static List<Holder<Enchantment>> incompatible(ItemStack stack, Holder<Enchantment> enchantment, HolderLookup.Provider registries) {
        if (PenchantmentHelper.hasEnchantment(stack, enchantment)) return List.of();
        return PenchantmentHelper.getEnchantments(stack).keySet().stream()
                .filter(other -> !PenchantmentHelper.areCompatible(enchantment, other, stack, registries))
                .toList();
    }

    private record Offer(Holder<Enchantment> enchantment, EnchantmentSlotWidget rules, int xp, int books, boolean locked, boolean disenchant) {
        Component name() {
            var name = enchantment.value().description().copy();
            if (locked) name.withStyle(style -> style.withFont(Minecraft.ALT_FONT));
            else if (enchantment.is(EnchantmentTags.CURSE)) name.withColor(0xFFFF8A80);
            return name;
        }
    }
}
