package net.frosthain.prototype;

import net.frosthain.ui.Theme;
import net.frosthain.ui.dialogue.DialogueView;
import net.frosthain.ui.text.TextLayout;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Reference image F: cinematic NPC dialogue. The HUD behind it is hidden by {@code HudPolicy}. */
final class DialogueScene {

    /** 8×11 pixel portrait of Eldrin (head plus long beard), Minecraft-like. */
    private static final String[] PORTRAIT = {
            "HHGHHGHH",
            "HGHHHHGH",
            "HSSSSSSH",
            "SWWSSWWS",
            "SIESSEIS",
            "SSSNNSSS",
            "BBSSSSBB",
            "BCBBBBCB",
            ".BBCBBB.",
            "..BBCB..",
            "...BB...",
    };

    private final Painter painter;
    private final List<Rectangle2D> choiceHits = new ArrayList<>();

    DialogueScene(Painter painter) {
        this.painter = painter;
    }

    void render(Graphics2D g, PrototypeState state, int w, int h) {
        DialogueView view = state.dialogue;
        double panelX = 300;
        double panelY = h - 290;
        double panelW = w - panelX - 50;
        double panelH = 250;

        portrait(g, 60, h - 470, 30);
        painter.frame(g, panelX, panelY, panelW, panelH, 0xF00C0E11, true);

        // Name plate (reference image F: framed tab above the text).
        Font nameFont = painter.bold(Theme.FontRole.HEADING, 28f);
        int nameWidth = painter.width(view.speakerName(), nameFont) + 70;
        painter.frame(g, panelX + 70, panelY - 28, nameWidth, 52, 0xFF15110B, true);
        painter.textCentered(g, view.speakerName(), nameFont, Theme.FRAME_GOLD_BRIGHT, panelX + 70 + nameWidth / 2.0, panelY - 20);

        double textX = panelX + 60;
        double textWidth = panelW * 0.55 - 60;
        Font body = painter.font(Theme.FontRole.BODY, 28f);
        double y = panelY + 46;
        int color = view.isInterrupted() ? Theme.STATE_BLOCKED : Theme.TEXT_PRIMARY;
        for (String line : TextLayout.wrap(view.line(), (int) textWidth, painter.measurer(), Theme.FontRole.BODY, 28)) {
            painter.text(g, line, body, color, textX, y);
            y += 36;
        }
        if (view.isInterrupted()) {
            painter.text(g, "R: Gespräch fortsetzen · Esc: schließen", painter.font(Theme.FontRole.BODY_ITALIC, 18f),
                    Theme.TEXT_SECONDARY, textX, y + 10);
        }

        choiceHits.clear();
        double cx = panelX + panelW * 0.57;
        double cw = panelW * 0.43 - 34;
        double cy = panelY + 24;
        int count = Math.max(1, view.choices().size());
        double rowHeight = Math.min(46, (panelH - 64) / count);
        float choiceSize = (float) Math.min(25, rowHeight * 0.58);
        Font choiceFont = painter.font(Theme.FontRole.BODY, choiceSize);
        Font numberFont = painter.font(Theme.FontRole.BODY_ITALIC, 15f);
        for (DialogueView.ChoiceView choice : view.choices()) {
            Rectangle2D row = new Rectangle2D.Double(cx, cy, cw, rowHeight - 4);
            if (choice.selected()) {
                g.setPaint(new GradientPaint((float) cx, (float) cy, Painter.color(0xFF3B2F1A), (float) (cx + cw), (float) cy,
                        Painter.color(0xFF1A160F)));
                g.fill(row);
                g.setStroke(new BasicStroke(1.6f));
                g.setColor(Painter.color(Theme.FRAME_GOLD_BRIGHT));
                g.draw(row);
                painter.arrow(g, cx + 12, cy + (rowHeight - 4) / 2, 6, Theme.FRAME_GOLD_BRIGHT);
            }
            double textTop = cy + (rowHeight - 4 - choiceSize * 1.25) / 2;
            painter.text(g, choice.text(), choiceFont, choice.selected() ? Theme.TEXT_PRIMARY : 0xFFD9CFBB, cx + 34, textTop);
            painter.text(g, String.valueOf(choice.index() + 1), numberFont, 0x80B9AE98, cx + cw - 18, cy + (rowHeight - 4) / 2 - 9);
            choiceHits.add(row);
            cy += rowHeight;
        }
        Font hint = painter.font(Theme.FontRole.BODY_ITALIC, 14f);
        painter.text(g, "Pfeiltasten oder 1–9 wählen · Enter bestätigen · Esc schließen · F9: Schaden simulieren (Test)", hint,
                Theme.TEXT_SECONDARY, panelX + 24, panelY + panelH - 26);
    }

    private void portrait(Graphics2D g, double x, double y, double pixel) {
        // Shoulders and robe with a gold-trimmed collar.
        g.setPaint(new GradientPaint((float) x, (float) (y + 7 * pixel), new Color(0x3F4F78), (float) x, (float) (y + 16 * pixel),
                new Color(0x1E2740)));
        Path2D robe = new Path2D.Double();
        robe.moveTo(x - 0.5 * pixel, y + 7.5 * pixel);
        robe.lineTo(x + 8.5 * pixel, y + 7.5 * pixel);
        robe.lineTo(x + 10 * pixel, y + 17 * pixel);
        robe.lineTo(x - 2 * pixel, y + 17 * pixel);
        robe.closePath();
        g.fill(robe);
        g.setColor(new Color(0xB8955A));
        g.setStroke(new java.awt.BasicStroke((float) (pixel * 0.35)));
        g.draw(robe);
        for (int row = 0; row < PORTRAIT.length; row++) {
            for (int col = 0; col < 8; col++) {
                Color c = switch (PORTRAIT[row].charAt(col)) {
                    case 'H' -> new Color(0xDCD9D2);
                    case 'G' -> new Color(0xB9B4AA);
                    case 'S' -> new Color(0xC9A07F);
                    case 'N' -> new Color(0xB0836A);
                    case 'W' -> new Color(0xF0EEEA);
                    case 'I' -> new Color(0x3E6CA8);
                    case 'E' -> new Color(0x1E2633);
                    case 'B' -> new Color(0xE4E1DA);
                    case 'C' -> new Color(0xC2BDB3);
                    default -> null;
                };
                if (c != null) {
                    g.setColor(c);
                    g.fill(new Rectangle2D.Double(x + col * pixel, y + row * pixel, pixel + 0.5, pixel + 0.5));
                }
            }
        }
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Rectangle2D.Double(x + 7 * pixel, y, pixel, 8 * pixel));
    }

    // ---- input ----------------------------------------------------------------------------------------

    void hover(PrototypeState state, double x, double y) {
        for (int i = 0; i < choiceHits.size(); i++) {
            if (choiceHits.get(i).contains(x, y)) {
                state.dialogue.select(i);
            }
        }
    }

    void click(PrototypeState state, double x, double y) {
        for (int i = 0; i < choiceHits.size(); i++) {
            if (choiceHits.get(i).contains(x, y)) {
                state.dialogue.select(i);
                if (!state.dialogue.confirm()) {
                    state.closeDialogue();
                }
                return;
            }
        }
    }

    void key(PrototypeState state, int keyCode) {
        DialogueView view = state.dialogue;
        switch (keyCode) {
            case KeyEvent.VK_UP -> view.moveSelection(-1);
            case KeyEvent.VK_DOWN -> view.moveSelection(1);
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                if (!view.confirm() && !view.isInterrupted()) {
                    state.closeDialogue();
                }
            }
            case KeyEvent.VK_F9 -> view.onPlayerDamaged();
            case KeyEvent.VK_R -> view.session().resume();
            case KeyEvent.VK_ESCAPE -> state.closeDialogue();
            default -> {
                if (keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_9 && !view.chooseNumber(keyCode - KeyEvent.VK_0)
                        && !view.isInterrupted()) {
                    state.closeDialogue();
                }
            }
        }
    }
}
