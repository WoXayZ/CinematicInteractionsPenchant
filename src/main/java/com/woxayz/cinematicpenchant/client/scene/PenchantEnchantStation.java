package com.woxayz.cinematicpenchant.client.scene;

import java.util.List;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

import archives.tater.penchant.menu.PenchantmentMenu;

import com.woxayz.cinematicpenchant.client.panel.EnchantPanel;
import com.woxayz.cinematicpenchant.mixin.client.EnchantStationPositions;

import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.EnchantStation;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.MenuStation;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.station.StationKind;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * While the open menu is a {@link PenchantmentMenu}, slot 0 and slot 1 are aimed, clicked and drawn from that menu.
 * Any other menu, including the intro before a menu exists, stays on the wrapped {@link EnchantStation}.
 */
public class PenchantEnchantStation extends MenuStation<PenchantmentMenu> {
    private static final String HINT_ITEM = "container.penchant.enchant.slot.enchant";
    private static final String HINT_LAPIS = "cinematicpenchant.hint.lapis";
    private static final String HINT_BOOK = "cinematicpenchant.hint.book";
    private static final String HINT_SCENE = "cinematicpenchant.hint.scene";
    private static final String HINT_SCENE_BOOKS = "cinematicpenchant.hint.scene_books";

    private final EnchantStation vanilla;
    private final EnchantPanel panel = new EnchantPanel();
    private final OfferCloud offers = new OfferCloud();
    private boolean penchantSession;
    private boolean aimOnTable;
    private int hovered = -1;
    private double flashAt = -10;
    private double runeGlow;
    private double runeClock = -1.0;
    private double lapisFocus;
    private double bookFocus;

    public PenchantEnchantStation(BlockPos pos, Direction side, EnchantStation vanilla) {
        super(pos, side, PenchantmentMenu.class, 2);
        this.vanilla = vanilla;
    }

    public EnchantStation enchantStation() {
        return vanilla;
    }

    public boolean drawsPenchant() {
        if (menu() != null) penchantSession = true;
        return penchantSession;
    }

    @Override
    public StationKind kind() {
        return vanilla.kind();
    }

    @Override
    public Vec3 center() {
        return vanilla.center();
    }

    @Override
    public SoundEvent touchSound() {
        return vanilla.touchSound();
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        return vanilla.approach(player);
    }

    @Override
    public List<Vec3> workSamples() {
        return vanilla.workSamples();
    }

    @Override
    public CameraRig.Pose lean() {
        return vanilla.lean();
    }

    @Override
    public double leanFov() {
        return vanilla.leanFov();
    }

    @Override
    public float bodyPitch() {
        return drawsPenchant() && offers.count() > 3 ? 10.0f : vanilla.bodyPitch();
    }

    @Override
    public Vec3 rest(boolean left) {
        return vanilla.rest(left);
    }

    @Override
    public void updateCursor(boolean overPanel) {
        if (!drawsPenchant()) {
            vanilla.updateCursor(overPanel);
            mirrorAim();
            return;
        }
        hovered = -1;
        aimOnTable = false;
        this.aim = null;
        var open = menu();
        if (open != null) {
            panel.bind(open);
            offers.update(open);
        }
        if (overPanel || open == null) {
            offers.clearAim();
            return;
        }
        double[] mouse = Picking.mouseGui();
        if (mouse == null) {
            offers.clearAim();
            return;
        }
        if (offers.pick(mouse, pos, right(), side())) {
            this.aim = offers.aim(pos, right(), side());
            return;
        }
        if (lapisVisible() && overSocket(mouse, vanilla.corner())) {
            hovered = 1;
            this.aim = vanilla.corner();
            return;
        }
        if (bookVisible() && overSocket(mouse, bookCorner())) {
            hovered = 2;
            this.aim = bookCorner();
            return;
        }
        if (!slot(0).isEmpty() && under(mouse, vanilla.itemPos(), 0.22)) {
            hovered = 0;
            this.aim = vanilla.itemPos();
            return;
        }
        if (panel.contains(mouse[0], mouse[1])) return;
        Vec3 top = vanilla.top;
        Vec3 hit = Picking.cursorOnPlane(top.y);
        if (hit == null) return;
        Vec3 rel = hit.subtract(top);
        if (Math.abs(rel.x) > 0.52 || Math.abs(rel.z) > 0.52) return;
        hovered = 0;
        aimOnTable = true;
        this.aim = new Vec3(top.x + Math.max(-0.43, Math.min(0.43, rel.x)), top.y, top.z + Math.max(-0.43, Math.min(0.43, rel.z)));
    }

    @Override
    public void clearCursor() {
        hovered = -1;
        aimOnTable = false;
        offers.clearAim();
        vanilla.clearCursor();
        this.aim = null;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        if (!drawsPenchant()) return vanilla.handTarget(aim, shoulder, carrying, tap);
        if (aimOnTable) {
            Vec3 reach = new Vec3(aim.x - shoulder.x, 0.0, aim.z - shoulder.z);
            if (reach.lengthSqr() < 1.0E-8) return aim;
            reach = reach.normalize();
            double hover = carrying ? 0.075 : 0.045;
            return aim.add(0.0, 0.062 + hover - 0.06 * tap, 0.0).subtract(reach.scale(0.075));
        }
        Vec3 dir = aim.subtract(shoulder);
        if (dir.lengthSqr() < 1.0E-8) return aim;
        return aim.subtract(dir.normalize().scale(0.14 - 0.05 * tap)).add(0.0, -0.03, 0.0);
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        return vanilla.heldItemPoint(hand, shoulder);
    }

    @Override
    public Vec3[] handsOverride() {
        double t = offers.ritualTime();
        if (!drawsPenchant() || t < 0.0) return vanilla.handsOverride();
        double raise = Math.sin(Math.PI * Math.min(1.0, t / 0.85));
        Vec3 center = vanilla.itemPos().add(side().scale(0.07)).add(0.0, -0.1 + 0.05 * raise, 0.0);
        double spread = 0.17 - 0.04 * raise;
        return new Vec3[] { center.add(right().scale(spread)), center.add(right().scale(-spread)) };
    }

    @Override
    public void click(int button, boolean shift) {
        if (!drawsPenchant()) {
            vanilla.click(button, shift);
            mirrorAim();
            return;
        }
        var open = menu();
        if (open == null) return;
        panel.bind(open);
        offers.update(open);
        if (offers.hover() >= 0) {
            if (!offers.busy() && !offers.press()) deny();
            return;
        }
        if (hovered < 0) return;
        ItemStack carried = carried();
        boolean refused = hovered == 1 ? !PenchantmentMenu.isEnchantingIngredient(carried)
                : hovered == 2 ? !PenchantmentMenu.isDisenchantingIngredient(carried)
                : !accepts(0, carried);
        if (!carried.isEmpty() && !shift && refused) {
            deny();
            return;
        }
        slotClick(Math.min(hovered, 1), button, shift);
    }

    @Override
    public boolean busy() {
        return vanilla.busy();
    }

    @Override
    public boolean showsInventory() {
        return vanilla.showsInventory();
    }

    @Override
    public void beforeFrame() {
        vanilla.beforeFrame();
    }

    @Override
    public void tick() {
        vanilla.tick();
        if (!drawsPenchant()) return;
        var open = menu();
        if (open != null) offers.update(open);
        if (offers.tickRitual(vanilla.itemPos())) beginFlash();
        var player = Minecraft.getInstance().player;
        if (!slot(0).isEmpty() && player != null && player.tickCount % 3 == 0) EnchantFx.sparkle(vanilla.itemPos(), 0.2);
    }

    @Override
    public void onEnd() {
        if (drawsPenchant()) super.onEnd();
        vanilla.onEnd();
    }

    @Override
    public double cameraKick() {
        return vanilla.cameraKick();
    }

    public void beginFlash() {
        flashAt = Scene.clock();
    }

    @Override
    public double flash() {
        if (!drawsPenchant()) return vanilla.flash();
        double elapsed = Scene.clock() - flashAt;
        return elapsed >= 0.0 && elapsed < 0.5 ? Math.pow(1.0 - elapsed / 0.5, 2.0) * 0.26 : 0.0;
    }

    @Override
    public int flashColor() {
        return drawsPenchant() ? 15260927 : vanilla.flashColor();
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        vanilla.renderWorld(pose, buffers, camera, light);
        if (!drawsPenchant() || outro()) return;
        var open = menu();
        if (open == null) return;
        offers.update(open);
        offers.render(pose, buffers, Minecraft.getInstance().gameRenderer.getMainCamera(), pos, right(), side(), vanilla.itemPos());
    }

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        if (!drawsPenchant()) {
            vanilla.renderScene(pose, buffers, camera, light);
            return;
        }
        Vec3 cam = camera.getPosition();
        if (outro()) {
            drawLeaving(pose, buffers, camera, light);
            return;
        }
        ItemStack item = slot(0);
        float spin = (float) (Scene.clock() * 40.0);
        float size = 0.34f;
        double ritual = offers.ritualTime();
        if (ritual >= 0.0) {
            double rise = 1.0 - Math.pow(1.0 - ritual, 3.0);
            spin += (float) (900.0 * rise);
            if (ritual * 1.5 > 0.5) size *= (float) (1.0 + 0.18 * Math.sin(Math.PI * Math.min(1.0, (ritual - 0.333) / 0.47)));
            float glow = (float) (0.55 + 0.7 * Math.sin(Math.PI * Math.min(1.0, ritual / 0.75)));
            EnchantFx.billboard(pose, buffers, camera, vanilla.itemPos(), 0.28, Scene.clock() * 0.4, EnchantFx.GLOW, 0.35f * glow, 0.2f * glow, 0.55f * glow);
            double beam = ritual < 0.333 ? 0.0 : 1.0 - (ritual - 0.333) / 0.667;
            if (beam > 0.01) {
                float strength = (float) (beam * 1.3);
                EnchantFx.beam(pose, buffers, cam, new Vec3(vanilla.itemPos().x, EnchantFx.top(pos).y, vanilla.itemPos().z), 1.9, 0.5 + 0.3 * beam, Scene.clock() * 0.9, 0.7f * strength, 0.5f * strength, strength);
            }
        }
        if (!item.isEmpty()) {
            drawFloating(pose, buffers, cam, item, shaken(0, vanilla.itemPos()), spin, size);
        }
        drawRunes(pose, buffers, cam, item, ritual);
        ItemStack ingredient = slot(1);
        if (ingredient.isEmpty()) return;
        if (PenchantmentMenu.isEnchantingIngredient(ingredient)) {
            Vec3 corner = shaken(1, vanilla.corner());
            drawLapis(pose, buffers, cam, corner, ingredient.getCount(), sideDirection(), light);
            drawCount(pose, buffers, camera, corner.add(0.0, 0.115, 0.0), ingredient.getCount());
        } else {
            Vec3 corner = shaken(2, bookCorner());
            drawLaid(pose, buffers, cam, ingredient, corner, sideDirection(), light);
            if (ingredient.getCount() > 1) drawCount(pose, buffers, camera, corner.add(0.0, 0.115, 0.0), ingredient.getCount());
        }
    }

    @Override
    public void renderScreen(GuiGraphics graphics, Font font, float alpha) {
        if (!drawsPenchant()) {
            vanilla.renderScreen(graphics, font, alpha);
            return;
        }
        var open = menu();
        if (open == null) return;
        panel.bind(open);
        panel.render(graphics, alpha, grindstoneNear(pos));
    }

    @Override
    public List<Component> tooltip(ItemStack stack) {
        if (!drawsPenchant()) return vanilla.tooltip(stack);
        if (offers.hover() >= 0) return offers.tooltip();
        if (menu() == null || hovered < 0) return null;
        var mc = Minecraft.getInstance();
        if (hovered == 0) {
            ItemStack item = slot(0);
            if (item.isEmpty()) {
                return stack.isEmpty() ? List.of(Component.translatable("cinematic.enchanting.empty.item").withStyle(ChatFormatting.GRAY)) : null;
            }
            if (!stack.isEmpty() || aimOnTable) return null;
            var lines = new java.util.ArrayList<Component>(Screen.getTooltipFromItem(mc, item));
            lines.add(Component.translatable("cinematic.enchanting.take").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        boolean book = hovered == 2;
        if (!stack.isEmpty()) {
            boolean fits = book ? PenchantmentMenu.isDisenchantingIngredient(stack) : PenchantmentMenu.isEnchantingIngredient(stack);
            return fits ? null : List.of(Component.translatable(book ? HINT_BOOK : "cinematic.enchanting.only_lapis").withStyle(ChatFormatting.RED));
        }
        ItemStack ingredient = slot(1);
        if (ingredient.isEmpty()) return List.of(Component.translatable(book ? HINT_BOOK : "cinematic.enchanting.empty.lapis").withStyle(ChatFormatting.GRAY));
        var lines = new java.util.ArrayList<Component>(Screen.getTooltipFromItem(mc, ingredient));
        lines.set(0, lines.get(0).copy().append(" \u00d7" + ingredient.getCount()));
        return lines;
    }

    @Override
    public Component hint() {
        PenchantmentMenu menu = menu();
        if (menu == null) return vanilla.hint();
        if (hovered < 0 || !slot(Math.min(hovered, 1)).isEmpty()) {
            return Component.translatable(grindstoneNear(pos) ? HINT_SCENE_BOOKS : HINT_SCENE);
        }
        if (hovered == 0) return Component.translatable(HINT_ITEM);
        return Component.translatable(hovered == 2 ? HINT_BOOK : HINT_LAPIS);
    }

    @Override
    protected void act(int button, boolean shift) {
    }

    @Override
    protected Vec3 restPoint(int slot) {
        if (slot == 0) return vanilla.itemPos();
        ItemStack ingredient = slot(1);
        boolean book = ingredient.isEmpty() ? hovered == 2 : !PenchantmentMenu.isEnchantingIngredient(ingredient);
        return book ? bookCorner() : vanilla.corner();
    }

    private void mirrorAim() {
        this.aim = vanilla.aim;
    }

    private Vec3 shaken(int slot, Vec3 at) {
        return hovered == slot ? at.add(right().scale(denyShake())) : at;
    }

    private static ItemStack carried() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY : player.containerMenu.getCarried();
    }

    private void drawRunes(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack item, double ritual) {
        double target = 0.45 + (item.isEmpty() ? 0.0 : 0.2) + (offers.hover() >= 0 ? 0.3 : 0.0)
                + (ritual >= 0.0 ? 1.4 * Math.sin(Math.PI * Math.min(1.0, ritual / 0.75)) : 0.0);
        double clock = Scene.clock();
        double dt = runeClock < 0.0 ? 0.0 : Math.max(0.0, Math.min(0.1, clock - runeClock));
        runeClock = clock;
        double rate = target > runeGlow ? 6.0 : 2.5;
        runeGlow += (target - runeGlow) * (1.0 - Math.exp(-rate * dt));
        float g = (float) runeGlow;
        double spin = clock * 0.25 + (ritual >= 0.0 ? 4.0 * ritual * ritual * (3.0 - 2.0 * ritual) : 0.0);
        Vec3 top = EnchantFx.top(pos);
        EnchantFx.disc(pose, buffers, cam, top.add(0.0, 0.003, 0.0), 0.47, spin, EnchantFx.CIRCLE, 0.62f * g, 0.4f * g, 1.0f * g);
        EnchantFx.disc(pose, buffers, cam, top.add(0.0, 0.004, 0.0), 0.25, -spin * 1.7, EnchantFx.CIRCLE, 0.4f * g, 0.7f * g, 1.0f * g);

        ItemStack carried = carried();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        boolean ghost = slot(1).isEmpty() && ritual < 0.0;

        double focusTarget = ritual >= 0.0 ? 0.0 : hovered == 1 ? 1.0 : PenchantmentMenu.isEnchantingIngredient(carried) ? 0.65 : 0.0;
        lapisFocus += (focusTarget - lapisFocus) * (1.0 - Math.exp(-12.0 * dt));
        if (lapisVisible()) {
            float socket = (float) (0.5 + 0.42 * lapisFocus);
            boolean wrong = hovered == 1 && !carried.isEmpty() && !PenchantmentMenu.isEnchantingIngredient(carried);
            Vec3 corner = vanilla.corner();
            EnchantFx.disc(pose, buffers, cam, corner.add(0.0, 0.006, 0.0), 0.1, clock * 0.3, EnchantFx.CIRCLE,
                    wrong ? socket : socket * 0.2f, wrong ? socket * 0.2f : socket * 0.55f, wrong ? socket * 0.2f : socket);
            if (ghost) LapisGhost.draw(pose, buffers, camera, corner.add(0.0, 0.052 + Math.sin(clock * 1.5) * 0.004, 0.0), socket, LapisGhost.BLUE);
        }

        double bookTarget = ritual >= 0.0 ? 0.0 : hovered == 2 ? 1.0 : PenchantmentMenu.isDisenchantingIngredient(carried) ? 0.65 : 0.0;
        bookFocus += (bookTarget - bookFocus) * (1.0 - Math.exp(-12.0 * dt));
        if (bookVisible()) {
            float socket = (float) (0.5 + 0.42 * bookFocus);
            boolean wrong = hovered == 2 && !carried.isEmpty() && !PenchantmentMenu.isDisenchantingIngredient(carried);
            Vec3 corner = bookCorner();
            EnchantFx.disc(pose, buffers, cam, corner.add(0.0, 0.006, 0.0), 0.1, -clock * 0.3, EnchantFx.CIRCLE,
                    wrong ? socket : socket * 0.62f, socket * (wrong ? 0.2f : 0.36f), socket * (wrong ? 0.2f : 0.14f));
            if (ghost) LapisGhost.draw(pose, buffers, camera, corner.add(0.0, 0.052 + Math.sin(clock * 1.5 + 1.0) * 0.004, 0.0), socket, LapisGhost.BROWN);
        }
    }

    private boolean lapisVisible() {
        ItemStack ingredient = slot(1);
        return ingredient.isEmpty() || PenchantmentMenu.isEnchantingIngredient(ingredient);
    }

    private boolean bookVisible() {
        ItemStack ingredient = slot(1);
        if (!ingredient.isEmpty()) return !PenchantmentMenu.isEnchantingIngredient(ingredient);
        return grindstoneNear(pos);
    }

    /** Whether a disenchanter block really stands near the table; Penchant's menu flag is always on in creative. */
    public static boolean grindstoneNear(BlockPos pos) {
        var level = Minecraft.getInstance().level;
        return level != null && PenchantmentMenu.hasDisenchanter(level, pos);
    }

    private Vec3 bookCorner() {
        return bookCorner(pos, sideDirection());
    }

    /** The lapis corner mirrored to the left of the book. */
    public static Vec3 bookCorner(BlockPos pos, Direction side) {
        Vec3 top = EnchantFx.top(pos);
        Vec3 lapis = EnchantStationPositions.cinematicpenchant$lapisPosition(pos, side);
        Vec3 across = Vec3.atLowerCornerOf(side.getClockWise().getNormal());
        double offset = lapis.subtract(top).dot(across);
        return lapis.subtract(across.scale(2.0 * offset));
    }

    private static boolean overSocket(double[] mouse, Vec3 corner) {
        Vec3 socket = Picking.cursorOnPlane(corner.y);
        return under(mouse, corner.add(0.0, 0.06, 0.0), 0.16) || socket != null && socket.distanceToSqr(corner) <= 0.04;
    }

    private static boolean under(double[] mouse, Vec3 center, double radius) {
        double[] at = Picking.toGui(center);
        if (at == null) return false;
        double[] edge = Picking.toGui(center.add(0.0, radius, 0.0));
        double pixels = edge == null ? 8.0 : Math.max(8.0, Math.hypot(at[0] - edge[0], at[1] - edge[1]));
        return Math.hypot(mouse[0] - at[0], mouse[1] - at[1]) < pixels;
    }

    private static void drawFloating(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack item, Vec3 at, float spin, float size) {
        if (size <= 0.002f) return;
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(new Quaternionf().rotationY((float) Math.toRadians(spin)));
        boolean block = mc.getItemRenderer().getModel(item, mc.level, null, 0).isGui3d();
        float scale = block ? size * 1.4f : size;
        pose.scale(scale, scale, scale);
        mc.getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
        pose.popPose();
    }

    /** Lays the book flat like the lapis, mirrored left to right as seen from the side the player stands on. */
    public static void drawLaid(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack stack, Vec3 corner, Direction side, int light) {
        flipped(pose, cam, corner, side, () ->
                ItemPlacer.render(pose, buffers, cam, stack.copyWithCount(1), corner.add(0.0, 0.001, 0.0), Direction.EAST, 4242L, 0.8f, 0.0f, light));
    }

    /**
     * Turns a flat item over around the axis pointing at the player. Its back face is the mirrored texture,
     * so this mirrors it without a negative scale, which would break face culling and lighting.
     */
    private static void flipped(PoseStack pose, Vec3 cam, Vec3 corner, Direction side, Runnable draw) {
        Vec3 pivot = corner.add(0.0, FLIP_LIFT, 0.0).subtract(cam);
        pose.pushPose();
        pose.translate(pivot.x, pivot.y, pivot.z);
        pose.mulPose(new Quaternionf().rotationAxis((float) Math.PI, side.getStepX(), 0.0f, side.getStepZ()));
        pose.translate(-pivot.x, -pivot.y, -pivot.z);
        draw.run();
        pose.popPose();
    }

    private static final double FLIP_LIFT = 0.03;

    private static void drawLapis(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3 corner, int count, Direction side, int light) {
        int copies = count >= 16 ? 3 : count >= 4 ? 2 : 1;
        flipped(pose, cam, corner, side, () -> {
            for (int i = 0; i < copies; i++) {
                Vec3 at = corner.add((i - (copies - 1) / 2.0) * 0.05, 0.001 + (copies - 1 - i) * 0.008, (i % 2) * 0.03);
                ItemPlacer.render(pose, buffers, cam, new ItemStack(Items.LAPIS_LAZULI), at, Direction.EAST, 4242L + (long) i * 17L, 0.8f, 0.0f, light);
            }
        });
    }

    private static void drawCount(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 at, int count) {
        Font font = Minecraft.getInstance().font;
        Vec3 cam = camera.getPosition();
        String label = "\u00d7" + count;
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(camera.rotation());
        pose.scale(0.0045f, -0.0045f, 0.0045f);
        font.drawInBatch(label, -font.width(label) / 2.0f, -4.0f, 0xE69CC8FF, true, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        pose.popPose();
    }
}
