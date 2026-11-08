package net.frosthain.prototype;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.TalentTreeDefinition;
import net.frosthain.ui.Theme;
import net.frosthain.ui.talent.Camera;
import net.frosthain.ui.talent.FocusNavigator;
import net.frosthain.ui.talent.HexShape;
import net.frosthain.ui.talent.NodeState;
import net.frosthain.ui.talent.TalentTreeView;
import net.frosthain.ui.text.TextLayout;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reference image D: branching talent tree with path list, tabs and detail panel. */
final class TalentScene {

    private static final int MARGIN = 22;
    private static final int LEFT_WIDTH = 270;
    private static final int RIGHT_WIDTH = 330;
    private static final int HEADER = 96;

    private final Painter painter;

    private Rectangle2D treeArea = new Rectangle2D.Double();
    private final List<Map.Entry<Rectangle2D, ContentId>> pathHits = new ArrayList<>();
    private final List<Map.Entry<Rectangle2D, String>> tabHits = new ArrayList<>();
    private final Map<String, double[]> nodeScreen = new HashMap<>();
    private Rectangle2D unlockButton = new Rectangle2D.Double();
    private Rectangle2D refundButton = new Rectangle2D.Double();
    private Rectangle2D resetButton = new Rectangle2D.Double();
    private boolean cameraInitialised;
    private double nodeRadius;

    TalentScene(Painter painter) {
        this.painter = painter;
    }

    void render(Graphics2D g, PrototypeState state, int w, int h) {
        TalentTreeView view = state.talentView();
        g.setPaint(new RadialGradientPaint(w / 2f, h / 2f, w * 0.7f, new float[]{0f, 1f},
                new Color[]{new Color(0x1A1F26), new Color(0x07090B)}));
        g.fillRect(0, 0, w, h);
        painter.frame(g, MARGIN, MARGIN, w - 2 * MARGIN, h - 2 * MARGIN, 0xF20C0E11, true);

        header(g, state, view, w);
        leftColumn(g, state, view, h);
        treeArea = new Rectangle2D.Double(MARGIN + LEFT_WIDTH + 18, HEADER + 10,
                w - 2 * MARGIN - LEFT_WIDTH - RIGHT_WIDTH - 36, h - HEADER - 10 - MARGIN - 46);
        Camera camera = state.camera;
        camera.setViewport(treeArea.getWidth(), treeArea.getHeight());
        if (!cameraInitialised) {
            double[] b = view.bounds();
            camera.fit(b[0], b[1], b[2], b[3], 0.9);
            cameraInitialised = true;
        }
        tree(g, state, view);
        detail(g, state, view, w, h);
        Font hint = painter.font(Theme.FontRole.BODY_ITALIC, 14f);
        painter.textCentered(g, state.translator.t("ui.frosthain.talents.keys") + " · Esc: zurück", hint, Theme.TEXT_SECONDARY,
                treeArea.getCenterX(), h - MARGIN - 34);
    }

    private void header(Graphics2D g, PrototypeState state, TalentTreeView view, int w) {
        Glyphs.draw(g, "snowflake", MARGIN + 38, MARGIN + 40, 20, Painter.color(Theme.FRAME_GOLD_BRIGHT));
        painter.text(g, state.translator.t("ui.frosthain.brand"), painter.bold(Theme.FontRole.HEADING, 30f), Theme.FRAME_GOLD_BRIGHT,
                MARGIN + 66, MARGIN + 12);
        painter.text(g, state.translator.t("ui.frosthain.talents.title"), painter.font(Theme.FontRole.HEADING, 20f),
                Theme.TEXT_PRIMARY, MARGIN + 110, MARGIN + 50);

        tabHits.clear();
        List<TalentTreeView.Tab> tabs = view.tabs();
        Font tabFont = painter.font(Theme.FontRole.HEADING, 17f);
        int tabWidth = 150;
        for (TalentTreeView.Tab tab : tabs) {
            tabWidth = Math.max(tabWidth, painter.width(tab.name(), tabFont) + 56);
        }
        double totalWidth = tabs.size() * tabWidth;
        double x = MARGIN + LEFT_WIDTH + 18 + (w - 2 * MARGIN - LEFT_WIDTH - RIGHT_WIDTH - 36 - totalWidth) / 2;
        double y = MARGIN + 22;
        painter.frame(g, x - 6, y - 6, totalWidth + 12, 50, 0xFF101317, false);
        for (TalentTreeView.Tab tab : tabs) {
            boolean active = tab.branch().equals(state.activeTab);
            if (active) {
                g.setPaint(new GradientPaint((float) x, (float) y, Painter.color(0xFF3A2E1C), (float) x, (float) (y + 38),
                        Painter.color(0xFF1E1810)));
                g.fill(new Rectangle2D.Double(x + 2, y, tabWidth - 4, 38));
                g.setColor(Painter.color(Theme.FRAME_GOLD_BRIGHT));
                g.draw(new Rectangle2D.Double(x + 2, y, tabWidth - 4, 38));
            }
            painter.diamond(g, x + 16, y + 19, 4, tab.color());
            painter.textCentered(g, tab.name(), tabFont, active ? Theme.FRAME_GOLD_BRIGHT : Theme.TEXT_PRIMARY,
                    x + tabWidth / 2.0 + 6, y + 8);
            tabHits.add(Map.entry(new Rectangle2D.Double(x, y, tabWidth, 38), tab.branch()));
            x += tabWidth;
        }
    }

    private void leftColumn(Graphics2D g, PrototypeState state, TalentTreeView view, int h) {
        double x = MARGIN + 16;
        double y = HEADER + 8;
        painter.frame(g, x, y, LEFT_WIDTH - 10, 170, 0xFF111418, false);
        painter.textCentered(g, state.translator.t("ui.frosthain.talents.points"), painter.font(Theme.FontRole.BODY, 19f),
                Theme.TEXT_PRIMARY, x + (LEFT_WIDTH - 10) / 2.0, y + 14);
        double cx = x + (LEFT_WIDTH - 10) / 2.0;
        g.setColor(Painter.color(0xFF0B0D10));
        g.fill(new Ellipse2D.Double(cx - 30, y + 48, 60, 60));
        g.setStroke(new BasicStroke(2f));
        g.setColor(Painter.color(Theme.FRAME_GOLD_BRIGHT));
        g.draw(new Ellipse2D.Double(cx - 30, y + 48, 60, 60));
        painter.textCentered(g, String.valueOf(view.availablePoints()), painter.bold(Theme.FontRole.HEADING, 30f),
                Theme.FRAME_GOLD_BRIGHT, cx, y + 58);
        painter.textCentered(g, state.translator.t("ui.frosthain.talents.character_level", view.characterLevel()),
                painter.font(Theme.FontRole.BODY, 18f), Theme.TEXT_PRIMARY, cx, y + 120);

        y += 190;
        painter.text(g, state.translator.t("ui.frosthain.talents.paths"), painter.font(Theme.FontRole.HEADING, 16f),
                Theme.TEXT_SECONDARY, x + 6, y);
        y += 28;
        pathHits.clear();
        for (TalentTreeView.PathEntry path : state.paths()) {
            boolean selected = path.tree().equals(state.selectedTree);
            Rectangle2D row = new Rectangle2D.Double(x, y, LEFT_WIDTH - 10, 44);
            if (selected) {
                g.setPaint(new GradientPaint((float) x, (float) y, Painter.color(0xFF2E2416), (float) (x + LEFT_WIDTH), (float) y,
                        Painter.color(0x00000000)));
                g.fill(row);
                painter.arrow(g, x - 1, y + 22, 6, Theme.FRAME_GOLD_BRIGHT);
            }
            String icon = switch (path.category()) {
                case GENERAL -> "tree";
                case RUNE -> "rune";
                case CLASS -> "swords";
            };
            int color = path.accessible() ? categoryColor(path.category()) : Theme.TEXT_DISABLED;
            g.setColor(Painter.color(0xFF15181C));
            g.fill(new Rectangle2D.Double(x + 14, y + 6, 32, 32));
            g.setColor(Painter.color(Theme.FRAME_GOLD_DIM));
            g.draw(new Rectangle2D.Double(x + 14, y + 6, 32, 32));
            Glyphs.draw(g, path.accessible() ? icon : "lock", x + 30, y + 22, 11, Painter.color(color));
            painter.text(g, path.name(), painter.font(Theme.FontRole.BODY, 19f),
                    path.accessible() ? Theme.TEXT_PRIMARY : Theme.TEXT_DISABLED, x + 56, y + 9);
            pathHits.add(Map.entry(row, path.tree()));
            y += 50;
        }

        double rulesY = h - MARGIN - 200;
        Font small = painter.font(Theme.FontRole.BODY_ITALIC, 14f);
        List<String> lines = TextLayout.wrap(state.translator.t("ui.frosthain.talents.respec_rules"), LEFT_WIDTH - 30,
                painter.measurer(), Theme.FontRole.BODY_ITALIC, 14);
        for (String line : lines) {
            painter.text(g, line, small, Theme.TEXT_SECONDARY, x + 6, rulesY);
            rulesY += 18;
        }
        String resetLabel = state.confirmReset ? "Wirklich zurücksetzen?" : state.translator.t("ui.frosthain.talents.reset");
        resetButton = new Rectangle2D.Double(x + 6, rulesY + 10, LEFT_WIDTH - 30, 36);
        painter.button(g, resetLabel, resetButton.getX(), resetButton.getY(), resetButton.getWidth(), resetButton.getHeight(),
                !state.sheet.talents().isEmpty(), false);
    }

    private static int categoryColor(TalentTreeDefinition.Category category) {
        return switch (category) {
            case GENERAL -> Theme.FRAME_GOLD_BRIGHT;
            case RUNE -> Theme.BRANCH_RUNE;
            case CLASS -> Theme.BRANCH_COMBAT;
        };
    }

    private void tree(Graphics2D g, PrototypeState state, TalentTreeView view) {
        Shape oldClip = g.getClip();
        g.clip(treeArea);
        Camera camera = state.camera;
        double ox = treeArea.getX();
        double oy = treeArea.getY();
        nodeRadius = Math.clamp(camera.zoom() * 0.36, 16, 48);
        nodeScreen.clear();
        List<TalentTreeView.NodeView> nodes = view.nodes();
        for (TalentTreeView.NodeView node : nodes) {
            nodeScreen.put(node.key(), new double[]{ox + camera.toScreenX(node.x()), oy + camera.toScreenY(node.y())});
        }
        // Subtle radial glow behind the root (reference image D).
        double[] root = nodeScreen.get(view.tree().root());
        g.setPaint(new RadialGradientPaint((float) root[0], (float) root[1], (float) (nodeRadius * 6), new float[]{0f, 1f},
                new Color[]{Painter.color(0x33E6C27A), Painter.color(0x00E6C27A)}));
        g.fill(new Ellipse2D.Double(root[0] - nodeRadius * 6, root[1] - nodeRadius * 6, nodeRadius * 12, nodeRadius * 12));

        for (TalentTreeView.EdgeView edge : view.edges()) {
            double[] a = nodeScreen.get(edge.from());
            double[] b = nodeScreen.get(edge.to());
            double dim = dimFactor(state, view.node(edge.to()).orElseThrow());
            Line2D line = new Line2D.Double(a[0], a[1], b[0], b[1]);
            if (edge.active()) {
                g.setStroke(new BasicStroke((float) (nodeRadius * 0.32), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Painter.color(Theme.withAlpha(edge.color(), 0.28 * dim)));
                g.draw(line);
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Painter.color(Theme.withAlpha(Theme.mix(edge.color(), 0xFFFFFFFF, 0.25), dim)));
            } else {
                float[] dash = edge.anyOf() ? new float[]{6f, 6f} : null;
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
                g.setColor(Painter.color(Theme.withAlpha(0xFF4A4E55, dim)));
            }
            g.draw(line);
        }
        for (TalentTreeView.NodeView node : nodes) {
            drawNode(g, state, node);
        }
        g.setClip(oldClip);
        if (!view.accessible()) {
            Font f = painter.font(Theme.FontRole.HEADING, 18f);
            String text = state.translator.t("ui.frosthain.talents.preview");
            int tw = painter.width(text, f) + 30;
            painter.frame(g, treeArea.getCenterX() - tw / 2.0, treeArea.getY() + 8, tw, 36, Theme.PANEL_BACKGROUND, false);
            painter.textCentered(g, text, f, Theme.STATE_BLOCKED, treeArea.getCenterX(), treeArea.getY() + 14);
        }
    }

    private static double dimFactor(PrototypeState state, TalentTreeView.NodeView node) {
        if (state.activeTab.isEmpty() || node.branch().isEmpty() || node.branch().equals(state.activeTab)) {
            return 1.0;
        }
        return 0.3;
    }

    private void drawNode(Graphics2D g, PrototypeState state, TalentTreeView.NodeView node) {
        double[] p = nodeScreen.get(node.key());
        boolean root = node.state() == NodeState.ORIGIN || node.kind() == TalentTreeDefinition.NodeKind.ROOT;
        double r = root ? nodeRadius * 1.45 : node.kind() == TalentTreeDefinition.NodeKind.KEYSTONE ? nodeRadius * 1.15 : nodeRadius;
        double dim = dimFactor(state, node);
        boolean lit = node.state().isAllocated();
        int ring = switch (node.state()) {
            case LOCKED -> 0xFF55595F;
            case REACHABLE -> Theme.mix(node.color(), 0xFF55595F, 0.6);
            case AVAILABLE -> Theme.mix(node.color(), 0xFF55595F, 0.35);
            default -> node.color();
        };
        if (lit) {
            g.setPaint(new RadialGradientPaint((float) p[0], (float) p[1], (float) (r * 1.9), new float[]{0f, 1f},
                    new Color[]{Painter.color(Theme.withAlpha(node.color(), 0.55 * dim)), Painter.color(Theme.withAlpha(node.color(), 0))}));
            g.fill(new Ellipse2D.Double(p[0] - r * 1.9, p[1] - r * 1.9, r * 3.8, r * 3.8));
        }
        Path2D hex = hexPath(p[0], p[1], r);
        g.setPaint(new GradientPaint((float) p[0], (float) (p[1] - r), Painter.color(lit ? Theme.mix(node.color(), 0xFF101215, 0.62) : 0xFF22252A),
                (float) p[0], (float) (p[1] + r), Painter.color(0xFF101215)));
        g.fill(hex);
        g.setStroke(new BasicStroke(root ? 3.2f : 2.4f));
        g.setColor(Painter.color(Theme.withAlpha(ring, dim)));
        g.draw(hex);
        if (node.state() == NodeState.AVAILABLE) {
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 4f}, 0f));
            g.setColor(Painter.color(Theme.withAlpha(Theme.STATE_AVAILABLE, 0.85 * dim)));
            g.draw(hexPath(p[0], p[1], r + 5));
        }
        if (node.key().equals(state.selectedNode)) {
            g.setStroke(new BasicStroke(2.2f));
            g.setColor(Painter.color(Theme.FRAME_GOLD_BRIGHT));
            g.draw(hexPath(p[0], p[1], r + 9));
        }
        int iconColor = switch (node.state()) {
            case LOCKED -> 0xFF6A6E74;
            case REACHABLE, AVAILABLE -> Theme.mix(node.color(), 0xFF8A8E94, 0.5);
            default -> Theme.mix(node.color(), 0xFFFFFFFF, 0.35);
        };
        Glyphs.draw(g, node.state() == NodeState.LOCKED && !root ? "lock" : node.icon(), p[0], p[1], r * 0.5,
                Painter.color(Theme.withAlpha(iconColor, dim)));
        if (node.maxRank() > 1) {
            double pipY = p[1] + r * 0.98;
            for (int i = 0; i < node.maxRank(); i++) {
                double pipX = p[0] + (i - (node.maxRank() - 1) / 2.0) * 9;
                painter.diamond(g, pipX, pipY, 3.4, i < node.rank() ? Theme.FRAME_GOLD_BRIGHT : 0xFF3B3F45);
            }
        }
    }

    private static Path2D hexPath(double cx, double cy, double r) {
        double[] c = HexShape.corners(cx, cy, r);
        Path2D path = new Path2D.Double();
        path.moveTo(c[0], c[1]);
        for (int i = 2; i < c.length; i += 2) {
            path.lineTo(c[i], c[i + 1]);
        }
        path.closePath();
        return path;
    }

    private void detail(Graphics2D g, PrototypeState state, TalentTreeView view, int w, int h) {
        TalentTreeView.Detail d = view.detail(state.selectedNode);
        double x = w - MARGIN - RIGHT_WIDTH - 6;
        double y = HEADER + 8;
        double width = RIGHT_WIDTH - 10;
        double height = h - y - MARGIN - 56;
        painter.frame(g, x, y, width, height, 0xFF111418, true);
        double cx = x + width / 2;
        double iy = y + 26;
        g.setPaint(new GradientPaint((float) cx, (float) iy, Painter.color(Theme.mix(d.color(), 0xFF101215, 0.55)), (float) cx,
                (float) (iy + 76), Painter.color(0xFF101215)));
        g.fill(new Rectangle2D.Double(cx - 38, iy, 76, 76));
        g.setStroke(new BasicStroke(2f));
        g.setColor(Painter.color(Theme.FRAME_GOLD_BRIGHT));
        g.draw(new Rectangle2D.Double(cx - 38, iy, 76, 76));
        Glyphs.draw(g, d.icon(), cx, iy + 38, 24, Painter.color(Theme.mix(d.color(), 0xFFFFFFFF, 0.3)));

        double ty = iy + 92;
        painter.textCentered(g, d.name(), painter.bold(Theme.FontRole.HEADING, 22f), Theme.FRAME_GOLD_BRIGHT, cx, ty);
        ty += 32;
        painter.textCentered(g, d.kindLabel(), painter.font(Theme.FontRole.BODY_ITALIC, 17f), Theme.TEXT_SECONDARY, cx, ty);
        ty += 34;
        Font body = painter.font(Theme.FontRole.BODY, 18f);
        for (String line : TextLayout.wrap(d.description(), (int) width - 44, painter.measurer(), Theme.FontRole.BODY, 18)) {
            painter.text(g, line, body, Theme.TEXT_PRIMARY, x + 22, ty);
            ty += 23;
        }
        ty += 8;
        Font small = painter.font(Theme.FontRole.BODY, 16f);
        if (d.state() != NodeState.ORIGIN) {
            painter.text(g, d.rankText(), small, Theme.TEXT_SECONDARY, x + 22, ty);
            ty += 24;
        }
        if (!d.requirements().isEmpty()) {
            painter.separator(g, x + 22, ty + 4, width - 44);
            ty += 14;
            painter.text(g, state.translator.t("ui.frosthain.talents.requires"), painter.font(Theme.FontRole.HEADING, 14f),
                    Theme.TEXT_SECONDARY, x + 22, ty);
            ty += 22;
            for (TalentTreeView.Requirement requirement : d.requirements()) {
                int color = requirement.met() ? Theme.STATE_AVAILABLE : Theme.STATE_BLOCKED;
                painter.mark(g, requirement.met(), x + 24, ty + 11, 10, color);
                for (String line : TextLayout.wrap(requirement.text(), (int) width - 70, painter.measurer(), Theme.FontRole.BODY, 16)) {
                    painter.text(g, line, small, color, x + 42, ty);
                    ty += 20;
                }
            }
        }
        double bottom = y + height - 22;
        unlockButton = new Rectangle2D.Double(x + 22, bottom - 42, width - 44, 40);
        refundButton = new Rectangle2D.Double(x + 22, bottom - 88, width - 44, 34);
        double statusY = bottom - 150;
        painter.textCentered(g, d.statusText(), painter.bold(Theme.FontRole.HEADING, 18f), d.statusColor(), cx, statusY);
        if (d.state() != NodeState.ORIGIN && d.state() != NodeState.MAXED) {
            painter.textCentered(g, d.costText(), small, Theme.TEXT_PRIMARY, cx, statusY + 28);
        }
        List<String> otherReasons = d.reasons().stream()
                .filter(r -> d.requirements().stream().noneMatch(req -> r.endsWith(req.text())))
                .toList();
        double ry = statusY - 8 - otherReasons.size() * 19;
        for (String reason : otherReasons) {
            painter.textCentered(g, reason, painter.font(Theme.FontRole.BODY_ITALIC, 15f), Theme.STATE_BLOCKED, cx, ry);
            ry += 19;
        }
        if (d.canRefund()) {
            painter.button(g, state.translator.t("ui.frosthain.talents.refund"), refundButton.getX(), refundButton.getY(),
                    refundButton.getWidth(), refundButton.getHeight(), true, false);
        }
        painter.button(g, state.translator.t("ui.frosthain.talents.unlock"), unlockButton.getX(), unlockButton.getY(),
                unlockButton.getWidth(), unlockButton.getHeight(), d.canUnlock(), true);
    }

    // ---- input ----------------------------------------------------------------------------------------

    void click(PrototypeState state, double x, double y) {
        for (Map.Entry<Rectangle2D, ContentId> hit : pathHits) {
            if (hit.getKey().contains(x, y)) {
                state.selectTree(hit.getValue());
                return;
            }
        }
        for (Map.Entry<Rectangle2D, String> hit : tabHits) {
            if (hit.getKey().contains(x, y)) {
                selectTab(state, hit.getValue());
                return;
            }
        }
        if (unlockButton.contains(x, y)) {
            state.unlockSelected();
            return;
        }
        if (refundButton.contains(x, y)) {
            state.refundSelected();
            return;
        }
        if (resetButton.contains(x, y)) {
            if (state.confirmReset) {
                state.resetAll();
            } else {
                state.confirmReset = true;
            }
            return;
        }
        for (Map.Entry<String, double[]> node : nodeScreen.entrySet()) {
            if (HexShape.contains(node.getValue()[0], node.getValue()[1], nodeRadius * 1.2, x, y)) {
                state.selectedNode = node.getKey();
                return;
            }
        }
    }

    void selectTab(PrototypeState state, String branch) {
        TalentTreeView view = state.talentView();
        if (branch.equals(state.activeTab)) {
            state.activeTab = "";
            double[] b = view.bounds();
            state.camera.fit(b[0], b[1], b[2], b[3], 0.9);
        } else {
            state.activeTab = branch;
            double[] center = view.branchCenter(branch);
            state.camera.centerOn(center[0] * 0.75, center[1] * 0.75);
        }
    }

    boolean inTree(double x, double y) {
        return treeArea.contains(x, y);
    }

    void zoom(PrototypeState state, double factor, double x, double y) {
        state.camera.zoomAt(factor, x - treeArea.getX(), y - treeArea.getY());
    }

    void key(PrototypeState state, int keyCode) {
        TalentTreeView view = state.talentView();
        switch (keyCode) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN -> {
                FocusNavigator.Direction direction = switch (keyCode) {
                    case KeyEvent.VK_LEFT -> FocusNavigator.Direction.LEFT;
                    case KeyEvent.VK_RIGHT -> FocusNavigator.Direction.RIGHT;
                    case KeyEvent.VK_UP -> FocusNavigator.Direction.UP;
                    default -> FocusNavigator.Direction.DOWN;
                };
                view.node(state.selectedNode).flatMap(current -> FocusNavigator.next(view.nodes(), current, direction))
                        .ifPresent(next -> state.selectedNode = next.key());
            }
            case KeyEvent.VK_ENTER -> state.unlockSelected();
            case KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE -> state.refundSelected();
            case KeyEvent.VK_PLUS, KeyEvent.VK_ADD, KeyEvent.VK_EQUALS ->
                    state.camera.zoomAt(1.15, treeArea.getWidth() / 2, treeArea.getHeight() / 2);
            case KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT ->
                    state.camera.zoomAt(1 / 1.15, treeArea.getWidth() / 2, treeArea.getHeight() / 2);
            case KeyEvent.VK_0, KeyEvent.VK_NUMPAD0 -> {
                state.activeTab = "";
                double[] b = view.bounds();
                state.camera.fit(b[0], b[1], b[2], b[3], 0.9);
            }
            case KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4 -> {
                int index = keyCode - KeyEvent.VK_1;
                if (index < view.tabs().size()) {
                    selectTab(state, view.tabs().get(index).branch());
                }
            }
            default -> {
                // not handled
            }
        }
    }
}
