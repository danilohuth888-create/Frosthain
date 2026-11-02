package net.frosthain.prototype;

import net.frosthain.ui.Theme;
import net.frosthain.ui.hud.HudPolicy;
import net.frosthain.ui.quest.QuestTrackerView;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/** Reference image B: discreet survival HUD over the (placeholder) world. */
final class HudScene {

    static final int MINIMAP_RADIUS = 92;

    private final Painter painter;
    private final WorldBackdrop backdrop;

    /** Click area of the tracker collapse arrow, in logical coordinates. */
    Rectangle2D trackerToggle = new Rectangle2D.Double();

    HudScene(Painter painter, WorldBackdrop backdrop) {
        this.painter = painter;
        this.backdrop = backdrop;
    }

    void render(Graphics2D g, PrototypeState state, int w, int h) {
        g.drawImage(backdrop.image(), 0, 0, w, h, null);
        HudPolicy.Mode mode = state.hudMode();
        HudPolicy.Settings settings = HudPolicy.Settings.DEFAULT;
        if (mode == HudPolicy.Mode.DIALOGUE) {
            // Dialogue: darken the world slightly for readability; all HUD elements hidden per policy.
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRect(0, 0, w, h);
        }
        if (HudPolicy.isVisible(HudPolicy.Element.CHAPTER_BANNER, mode, settings)) {
            chapterBanner(g, state);
        }
        int cx = w - 130;
        int cy = 125;
        if (HudPolicy.isVisible(HudPolicy.Element.MINIMAP, mode, settings)) {
            minimap(g, cx, cy);
        }
        int y = cy + MINIMAP_RADIUS + 14;
        if (HudPolicy.isVisible(HudPolicy.Element.COORDINATES, mode, settings)) {
            Font small = painter.font(Theme.FontRole.BODY, 15f);
            painter.textCentered(g, state.translator.t("ui.frosthain.coordinates", 1284, -732), small, Theme.TEXT_PRIMARY, cx, y);
            painter.textCentered(g, state.translator.t("ui.frosthain.region.frosthain_valley"), small, Theme.TEXT_SECONDARY, cx, y + 19);
            y += 48;
        }
        if (HudPolicy.isVisible(HudPolicy.Element.QUEST_TRACKER, mode, settings)) {
            tracker(g, state, cx - MINIMAP_RADIUS - 8, y, 2 * MINIMAP_RADIUS + 16);
        }
        vanillaHud(g, mode, settings, w, h);
        if (HudPolicy.isVisible(HudPolicy.Element.INTERACTION_HINT, mode, settings) && state.scene == PrototypeState.Scene.HUD) {
            interactionHint(g, state, w, h);
        }
        if (HudPolicy.isVisible(HudPolicy.Element.CROSSHAIR, mode, settings) && state.scene == PrototypeState.Scene.HUD) {
            g.setColor(new Color(255, 255, 255, 200));
            g.fillRect(w / 2 - 9, h / 2 - 1, 18, 2);
            g.fillRect(w / 2 - 1, h / 2 - 9, 2, 18);
        }
        Font label = painter.font(Theme.FontRole.BODY_ITALIC, 13f);
        painter.text(g, "Prototyp – Hintergrund ist ein Platzhalter für die Minecraft-Welt", label, 0x99FFFFFF, 12, h - 22);
    }

    private void chapterBanner(Graphics2D g, PrototypeState state) {
        Glyphs.draw(g, "snowflake", 52, 52, 26, Painter.color(Theme.FRAME_GOLD_BRIGHT));
        g.setColor(Painter.color(Theme.withAlpha(Theme.FRAME_GOLD_BRIGHT, 0.5)));
        g.draw(new Ellipse2D.Double(20, 20, 64, 64));
        painter.text(g, state.translator.t("ui.frosthain.brand"), painter.bold(Theme.FontRole.HEADING, 34f), Theme.FRAME_GOLD_BRIGHT, 94, 22);
        painter.text(g, state.translator.t("ui.frosthain.chapter.awakening"), painter.font(Theme.FontRole.BODY_ITALIC, 20f),
                Theme.TEXT_PRIMARY, 132, 64);
    }

    private void minimap(Graphics2D g, int cx, int cy) {
        int r = MINIMAP_RADIUS;
        Shape circle = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        Shape oldClip = g.getClip();
        g.setClip(circle);
        g.drawImage(backdrop.minimap(), cx - r, cy - r, 2 * r, 2 * r, null);
        g.setClip(oldClip);
        g.setStroke(new BasicStroke(7f));
        g.setColor(Painter.color(0xFF2A2116));
        g.draw(circle);
        g.setStroke(new BasicStroke(2f));
        g.setColor(Painter.color(Theme.FRAME_GOLD_BRIGHT));
        g.draw(new Ellipse2D.Double(cx - r - 4, cy - r - 4, 2 * r + 8, 2 * r + 8));
        g.setColor(Painter.color(Theme.FRAME_GOLD));
        g.draw(new Ellipse2D.Double(cx - r + 3, cy - r + 3, 2 * r - 6, 2 * r - 6));
        Font compass = painter.bold(Theme.FontRole.HEADING, 16f);
        String[] letters = {"N", "O", "S", "W"};
        double[][] offsets = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int i = 0; i < 4; i++) {
            double lx = cx + offsets[i][0] * (r - 16);
            double ly = cy + offsets[i][1] * (r - 16);
            painter.textCentered(g, letters[i], compass, Theme.TEXT_PRIMARY, lx, ly - 11);
        }
        // Waypoint of the pinned main quest (Eldrin) and the player arrow.
        painter.diamond(g, cx - 34, cy - 30, 7, Theme.FRAME_GOLD_BRIGHT);
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(cx, cy - 10);
        arrow.lineTo(cx + 7, cy + 8);
        arrow.lineTo(cx, cy + 4);
        arrow.lineTo(cx - 7, cy + 8);
        arrow.closePath();
        g.setColor(Painter.color(0xFFE04A3A));
        g.fill(arrow);
    }

    private void tracker(Graphics2D g, PrototypeState state, int x, int y, int width) {
        QuestTrackerView.Layout layout = QuestTrackerView.layout(state.registry, state.questRules, state.questLog,
                state.translator, painter.measurer(), width, state.trackerCollapsed, QuestTrackerView.Style.DEFAULT);
        int height = layout.height() + 14;
        painter.frame(g, x, y, width, height, Theme.HUD_PANEL_BACKGROUND, false);
        for (QuestTrackerView.Line line : layout.lines()) {
            Font font = painter.font(line.role(), line.size());
            int lineY = y + line.y();
            switch (line.marker()) {
                case DIAMOND_MAIN -> painter.diamond(g, x + line.x() - 9, lineY + line.height() / 2.0, 5, Theme.FRAME_GOLD_BRIGHT);
                case DIAMOND_OTHER -> painter.diamond(g, x + line.x() - 9, lineY + line.height() / 2.0, 5, 0xFFC9CED6);
                case BOX_OPEN, BOX_DONE -> {
                    g.setStroke(new BasicStroke(1.2f));
                    g.setColor(Painter.color(Theme.TEXT_SECONDARY));
                    g.draw(new Rectangle2D.Double(x + line.x() - 14, lineY + 4, 9, 9));
                    if (line.marker() == QuestTrackerView.Marker.BOX_DONE) {
                        g.setColor(Painter.color(Theme.STATE_AVAILABLE));
                        g.fill(new Rectangle2D.Double(x + line.x() - 12, lineY + 6, 5, 5));
                    }
                }
                case NONE -> {
                    // no marker
                }
            }
            if (line.kind() == QuestTrackerView.LineKind.SECTION && line.y() > 12) {
                painter.separator(g, x + 10, lineY - QuestTrackerView.Style.DEFAULT.sectionGap() / 2.0, width - 20);
            }
            painter.text(g, line.text(), font, line.color(), x + line.x(), lineY);
        }
        // Collapse/expand arrow at the bottom (reference image B).
        double ax = x + width / 2.0;
        double ay = y + height - 9;
        Path2D chevron = new Path2D.Double();
        if (state.trackerCollapsed) {
            chevron.moveTo(ax - 6, ay - 4);
            chevron.lineTo(ax, ay + 2);
            chevron.lineTo(ax + 6, ay - 4);
        } else {
            chevron.moveTo(ax - 6, ay + 2);
            chevron.lineTo(ax, ay - 4);
            chevron.lineTo(ax + 6, ay + 2);
        }
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(Painter.color(Theme.FRAME_GOLD_BRIGHT));
        g.draw(chevron);
        trackerToggle = new Rectangle2D.Double(ax - 14, ay - 10, 28, 18);
    }

    private void vanillaHud(Graphics2D g, HudPolicy.Mode mode, HudPolicy.Settings settings, int w, int h) {
        int slot = 44;
        int barX = w / 2 - slot * 9 / 2;
        int barY = h - 64;
        if (HudPolicy.isVisible(HudPolicy.Element.HOTBAR, mode, settings)) {
            for (int i = 0; i < 9; i++) {
                g.setColor(new Color(20, 20, 20, 170));
                g.fillRect(barX + i * slot, barY, slot, slot);
                g.setColor(new Color(150, 150, 150, 200));
                g.drawRect(barX + i * slot, barY, slot, slot);
            }
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3f));
            g.drawRect(barX - 1, barY - 1, slot + 2, slot + 2);
            g.setStroke(new BasicStroke(1f));
        }
        if (HudPolicy.isVisible(HudPolicy.Element.EXPERIENCE, mode, settings)) {
            g.setColor(new Color(30, 30, 30, 200));
            g.fillRect(barX, barY - 12, slot * 9, 7);
            g.setColor(new Color(0x7EFC20));
            g.fillRect(barX, barY - 12, slot * 9 * 2 / 5, 7);
            painter.textCentered(g, "12", painter.bold(Theme.FontRole.BODY, 16f), 0xFF7EFC20, w / 2.0, barY - 32);
        }
        if (HudPolicy.isVisible(HudPolicy.Element.HEALTH, mode, settings)) {
            for (int i = 0; i < 10; i++) {
                pixelIcon(g, barX + i * 18, barY - 34, new Color(0xD62A2A));
            }
        }
        if (HudPolicy.isVisible(HudPolicy.Element.ARMOR, mode, settings)) {
            for (int i = 0; i < 10; i++) {
                pixelIcon(g, barX + i * 18, barY - 54, new Color(0xC8CDD3));
            }
        }
        if (HudPolicy.isVisible(HudPolicy.Element.FOOD, mode, settings)) {
            for (int i = 0; i < 10; i++) {
                pixelIcon(g, barX + slot * 9 - 16 - i * 18, barY - 34, new Color(0xB0702E));
            }
        }
    }

    private static void pixelIcon(Graphics2D g, int x, int y, Color color) {
        g.setColor(Color.BLACK);
        g.fillRect(x, y, 16, 14);
        g.setColor(color);
        g.fillRect(x + 2, y + 2, 12, 10);
    }

    private void interactionHint(Graphics2D g, PrototypeState state, int w, int h) {
        String text = state.translator.t("ui.frosthain.dialogue.hint", state.translator.t("ui.frosthain.key.use"),
                state.translator.t("npc.frosthain.eldrin"));
        Font font = painter.font(Theme.FontRole.BODY, 17f);
        int tw = painter.width(text, font) + 28;
        painter.frame(g, w / 2.0 - tw / 2.0, h * 0.58, tw, 34, Theme.HUD_PANEL_BACKGROUND, false);
        painter.textCentered(g, text, font, Theme.TEXT_PRIMARY, w / 2.0, h * 0.58 + 6);
    }
}
