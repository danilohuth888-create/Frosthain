package net.frosthain.prototype;

import net.frosthain.ui.Theme;
import net.frosthain.ui.text.TextMeasurer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Shared drawing helpers: fonts from the content pack, ornate frames, text. Purely visual, no game logic. */
final class Painter {

    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    private final Map<Theme.FontRole, Font> baseFonts = new EnumMap<>(Theme.FontRole.class);
    private final Map<String, Font> derived = new HashMap<>();

    Painter(Path contentRoot) {
        Path fonts = contentRoot.resolve("frosthain_base/assets/frosthain/font");
        baseFonts.put(Theme.FontRole.HEADING, load(fonts.resolve("Cinzel.ttf")));
        baseFonts.put(Theme.FontRole.BODY, load(fonts.resolve("EBGaramond.ttf")));
        baseFonts.put(Theme.FontRole.BODY_ITALIC, load(fonts.resolve("EBGaramond-Italic.ttf")));
    }

    private static Font load(Path file) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, file.toFile());
        } catch (FontFormatException | IOException e) {
            throw new UncheckedIOException(new IOException("Schrift nicht ladbar: " + file, e));
        }
    }

    Font font(Theme.FontRole role, float size) {
        return derived.computeIfAbsent(role + "@" + size, k -> baseFonts.get(role).deriveFont(size));
    }

    Font bold(Theme.FontRole role, float size) {
        return derived.computeIfAbsent(role + "@" + size + "b", k -> baseFonts.get(role).deriveFont(Font.BOLD, size));
    }

    TextMeasurer measurer() {
        return (text, role, size) -> (int) Math.ceil(font(role, size).getStringBounds(text, FRC).getWidth());
    }

    int width(String text, Font font) {
        return (int) Math.ceil(font.getStringBounds(text, FRC).getWidth());
    }

    static Color color(int argb) {
        return new Color(argb, true);
    }

    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /** Text with its top-left corner at (x, y); returns the width. */
    int text(Graphics2D g, String text, Font font, int argb, double x, double y) {
        g.setFont(font);
        g.setColor(color(argb));
        g.drawString(text, (float) x, (float) (y + g.getFontMetrics().getAscent()));
        return width(text, font);
    }

    void textCentered(Graphics2D g, String text, Font font, int argb, double centerX, double y) {
        text(g, text, font, argb, centerX - width(text, font) / 2.0, y);
    }

    /** Dark panel with a double gold border and small corner ornaments (reference images B, D, F). */
    void frame(Graphics2D g, double x, double y, double w, double h, int background, boolean ornate) {
        g.setPaint(new GradientPaint((float) x, (float) y, color(Theme.mix(background, 0xFF20252B, 0.25)),
                (float) x, (float) (y + h), color(background)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 6, 6));
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(color(Theme.FRAME_GOLD));
        g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 6, 6));
        g.setStroke(new BasicStroke(0.8f));
        g.setColor(color(Theme.withAlpha(Theme.FRAME_GOLD, 0.55)));
        g.draw(new RoundRectangle2D.Double(x + 4.5, y + 4.5, w - 9, h - 9, 4, 4));
        if (ornate) {
            corner(g, x, y, 1, 1);
            corner(g, x + w, y, -1, 1);
            corner(g, x, y + h, 1, -1);
            corner(g, x + w, y + h, -1, -1);
        }
    }

    private void corner(Graphics2D g, double x, double y, int sx, int sy) {
        g.setColor(color(Theme.FRAME_GOLD_BRIGHT));
        g.setStroke(new BasicStroke(1.4f));
        Path2D curl = new Path2D.Double();
        curl.moveTo(x + sx * 2, y + sy * 22);
        curl.curveTo(x + sx * 2, y + sy * 8, x + sx * 8, y + sy * 2, x + sx * 22, y + sy * 2);
        curl.moveTo(x + sx * 8, y + sy * 18);
        curl.curveTo(x + sx * 9, y + sy * 12, x + sx * 12, y + sy * 9, x + sx * 18, y + sy * 8);
        g.draw(curl);
        diamond(g, x + sx * 9, y + sy * 9, 3.2, Theme.FRAME_GOLD_BRIGHT);
    }

    void diamond(Graphics2D g, double cx, double cy, double r, int argb) {
        Path2D d = new Path2D.Double();
        d.moveTo(cx, cy - r);
        d.lineTo(cx + r, cy);
        d.lineTo(cx, cy + r);
        d.lineTo(cx - r, cy);
        d.closePath();
        g.setColor(color(argb));
        g.fill(d);
    }

    /** Right-pointing triangle (the font has no ▸ glyph). */
    void arrow(Graphics2D g, double x, double centerY, double size, int argb) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, centerY - size);
        p.lineTo(x + size * 1.3, centerY);
        p.lineTo(x, centerY + size);
        p.closePath();
        g.setColor(color(argb));
        g.fill(p);
    }

    /** Check mark or cross as vector shape (the font has no ✓/✗ glyphs). */
    void mark(Graphics2D g, boolean ok, double x, double centerY, double size, int argb) {
        g.setColor(color(argb));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D p = new Path2D.Double();
        if (ok) {
            p.moveTo(x, centerY);
            p.lineTo(x + size * 0.4, centerY + size * 0.45);
            p.lineTo(x + size, centerY - size * 0.5);
        } else {
            p.moveTo(x, centerY - size * 0.45);
            p.lineTo(x + size * 0.9, centerY + size * 0.45);
            p.moveTo(x + size * 0.9, centerY - size * 0.45);
            p.lineTo(x, centerY + size * 0.45);
        }
        g.draw(p);
    }

    /** Thin gold separator with a diamond in the middle. */
    void separator(Graphics2D g, double x, double y, double w) {
        g.setStroke(new BasicStroke(1f));
        g.setColor(color(Theme.withAlpha(Theme.FRAME_GOLD, 0.6)));
        g.drawLine((int) x, (int) y, (int) (x + w), (int) y);
        diamond(g, x + w / 2, y, 3, Theme.FRAME_GOLD_BRIGHT);
    }

    /** Button in FROSTHAIN style; disabled buttons are dimmed. */
    void button(Graphics2D g, String label, double x, double y, double w, double h, boolean enabled, boolean primary) {
        int background = primary && enabled ? 0xFF3A2C17 : 0xFF1C1F24;
        g.setPaint(new GradientPaint((float) x, (float) y, color(Theme.mix(background, 0xFF5A4523, primary && enabled ? 0.4 : 0.1)),
                (float) x, (float) (y + h), color(background)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 5, 5));
        g.setStroke(new BasicStroke(1.3f));
        g.setColor(color(enabled ? Theme.FRAME_GOLD_BRIGHT : Theme.FRAME_GOLD_DIM));
        g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 5, 5));
        Font f = font(Theme.FontRole.HEADING, 15f);
        int textColor = enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_DISABLED;
        text(g, label, f, textColor, x + (w - width(label, f)) / 2, y + (h - g.getFontMetrics(f).getHeight()) / 2 + 1);
    }
}
