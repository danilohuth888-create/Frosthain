package net.frosthain.mod.client.gui;

import net.frosthain.mod.FrosthainMod;
import net.frosthain.ui.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;

/**
 * Drawing helpers for "Immersive Fantasy 2.0" (docs/12): slate panels, thin gold lines, ornaments only at
 * titles, Cinzel for headings and EB Garamond for text. Colors come from {@link Theme}.
 */
public final class Ui {

    public static final FontDescription HEADING = new FontDescription.Resource(FrosthainMod.id("heading"));
    public static final FontDescription BODY = new FontDescription.Resource(FrosthainMod.id("body"));

    private Ui() {
    }

    public static MutableComponent heading(Component text) {
        return text.copy().withStyle(style -> style.withFont(HEADING));
    }

    public static MutableComponent body(Component text) {
        return text.copy().withStyle(style -> style.withFont(BODY));
    }

    /** Slate panel with a thin gold frame and small bright corner marks. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int background) {
        g.fill(x, y, x + w, y + h, background);
        g.outline(x, y, w, h, Theme.FRAME_GOLD_DIM);
        corners(g, x, y, w, h, Theme.FRAME_GOLD, 4);
    }

    public static void corners(GuiGraphicsExtractor g, int x, int y, int w, int h, int color, int length) {
        int r = x + w - 1;
        int b = y + h - 1;
        g.horizontalLine(x, x + length - 1, y, color);
        g.verticalLine(x, y, y + length - 1, color);
        g.horizontalLine(r - length + 1, r, y, color);
        g.verticalLine(r, y, y + length - 1, color);
        g.horizontalLine(x, x + length - 1, b, color);
        g.verticalLine(x, b - length + 1, b, color);
        g.horizontalLine(r - length + 1, r, b, color);
        g.verticalLine(r, b - length + 1, b, color);
    }

    /** Title with a thin gold rule and a small diamond on each side (the only ornament, per Design 2.0). */
    public static void ornamentTitle(GuiGraphicsExtractor g, Font font, Component title, int centerX, int y, float scale) {
        MutableComponent text = heading(title);
        int width = Math.round(font.width(text) * scale);
        scaledCentered(g, font, text, centerX, y, scale, Theme.FRAME_GOLD_BRIGHT);
        int lineY = y + Math.round(font.lineHeight * scale / 2f);
        int gap = 8;
        int length = Math.max(16, width / 3);
        g.horizontalLine(centerX - width / 2 - gap - length, centerX - width / 2 - gap, lineY, Theme.FRAME_GOLD_DIM);
        g.horizontalLine(centerX + width / 2 + gap, centerX + width / 2 + gap + length, lineY, Theme.FRAME_GOLD_DIM);
        diamond(g, centerX - width / 2 - gap - length - 3, lineY, Theme.FRAME_GOLD);
        diamond(g, centerX + width / 2 + gap + length + 3, lineY, Theme.FRAME_GOLD);
    }

    public static void diamond(GuiGraphicsExtractor g, int cx, int cy, int color) {
        g.fill(cx - 1, cy - 2, cx + 2, cy + 3, color);
        g.fill(cx - 2, cy - 1, cx + 3, cy + 2, color);
    }

    public static void scaledCentered(GuiGraphicsExtractor g, Font font, Component text, int centerX, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate(centerX, y);
        g.pose().scale(scale, scale);
        g.text(font, text, -font.width(text) / 2, 0, color, true);
        g.pose().popMatrix();
    }

    /** Word-wrapped body text; returns the y below the last line. */
    public static int paragraph(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width, int color) {
        int lineY = y;
        for (var line : font.split(body(text), width)) {
            g.text(font, line, x, lineY, color, false);
            lineY += font.lineHeight + 1;
        }
        return lineY;
    }

    /** Green "+" bullet line as in the reference images. */
    public static int bullet(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int width) {
        g.text(font, Component.literal("+"), x, y, Theme.STATE_AVAILABLE, false);
        return paragraph(g, font, text, x + 8, y, width - 8, Theme.TEXT_PRIMARY);
    }
}
