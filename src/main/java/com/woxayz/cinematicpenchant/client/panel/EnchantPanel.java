package com.woxayz.cinematicpenchant.client.panel;

import archives.tater.penchant.menu.PenchantmentMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Book count and grindstone state, drawn like a cinematic sigil: rune marks around the words.
 */
public class EnchantPanel {
    private static final int WIDTH = 104;

    private PenchantmentMenu menu;

    public void bind(PenchantmentMenu menu) {
        this.menu = menu;
    }

    public boolean contains(double mouseX, double mouseY) {
        if (menu == null) return false;
        int[] box = box();
        return mouseX >= box[0] && mouseX < box[0] + WIDTH && mouseY >= box[1] && mouseY < box[1] + 36;
    }

    public void render(GuiGraphics graphics, float alpha, boolean ready) {
        if (menu == null || alpha <= 0.15f) return;
        var font = Minecraft.getInstance().font;
        int x = box()[0];
        int y = box()[1];
        var wing = wing(0xFF7EB6FF);
        int wingWidth = font.width(wing);
        graphics.fill(x, y, x + WIDTH, y + 1, 0xFF9CC8FF);
        graphics.fill(x, y + 34, x + WIDTH, y + 35, 0x669CC8FF);
        drawLine(graphics, font, wing, wingWidth, x, y + 6, Component.translatable("cinematicpenchant.table.books", menu.getBookCount()), 0xFFE7D6FF);
        int state = ready ? 0xFF3DFF7A : 0xFFFF3B3B;
        drawLine(graphics, font, wing(state), wingWidth(font, state), x, y + 20, Component.translatable(ready ? "cinematicpenchant.table.grindstone.ready" : "cinematicpenchant.table.grindstone.absent"), state);
    }

    private static void drawLine(GuiGraphics graphics, net.minecraft.client.gui.Font font, Component wing, int wingWidth, int x, int y, Component text, int color) {
        graphics.drawString(font, wing, x, y, 0xFFFFFFFF, false);
        graphics.drawString(font, text, x + wingWidth + 6, y, color, true);
        graphics.drawString(font, wing, x + WIDTH - wingWidth, y, 0xFFFFFFFF, false);
    }

    private static int wingWidth(net.minecraft.client.gui.Font font, int color) {
        return font.width(wing(color));
    }

    private static MutableComponent wing(int color) {
        return Component.literal("aa").withStyle(style -> style.withFont(Minecraft.ALT_FONT).withColor(color));
    }

    private static int[] box() {
        var window = Minecraft.getInstance().getWindow();
        int height = window.getGuiScaledHeight();
        int width = window.getGuiScaledWidth();
        int y = Math.max(6, (height - 120) / 2 + 78);
        return new int[] { Math.max(4, width - WIDTH - 6), y };
    }
}
