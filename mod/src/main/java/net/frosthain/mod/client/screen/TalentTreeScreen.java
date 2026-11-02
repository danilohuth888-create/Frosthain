package net.frosthain.mod.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.content.TalentTreeDefinition;
import net.frosthain.core.progression.TalentAllocator;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.client.ClientCharacter;
import net.frosthain.mod.client.gui.FrosthainButton;
import net.frosthain.mod.client.gui.TalentIcons;
import net.frosthain.mod.client.gui.Ui;
import net.frosthain.mod.network.TalentActionPayload;
import net.frosthain.ui.Theme;
import net.frosthain.ui.Translator;
import net.frosthain.ui.talent.Camera;
import net.frosthain.ui.talent.HexShape;
import net.frosthain.ui.talent.NodeState;
import net.frosthain.ui.talent.TalentTreeView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Talent tree on key K (Design 2.0, view "Talentbaum"): path list and points on the left, hexagonal tree with
 * branch tabs in the middle, detail panel with unlock/refund on the right. What is allowed comes from
 * {@link TalentTreeView} (core rules); the server checks every action again.
 */
public final class TalentTreeScreen extends Screen {

    private static final int MARGIN = 10;
    private static final int LEFT_W = 98;
    private static final int RIGHT_W = 128;
    private static final double SQRT3 = Math.sqrt(3);

    private final ContentRegistry registry = FrosthainMod.content();
    private final TalentAllocator allocator = new TalentAllocator(registry);
    private final Translator translator = new Translator(registry, Minecraft.getInstance().getLanguageManager().getSelected());
    private final Camera camera = new Camera(14, 64, 30);
    private final Map<String, double[]> nodeScreen = new HashMap<>();

    private ContentId treeId = ContentId.of("general");
    private String activeBranch = "";
    private String selectedNode;
    private TalentTreeView view;
    private boolean cameraFitted;
    private boolean confirmReset;
    private List<String> problems = List.of();
    private int treeX;
    private int treeY;
    private int treeW;
    private int treeH;
    private double nodeRadius;

    public TalentTreeScreen() {
        super(Component.translatable("ui.frosthain.talents.title"));
    }

    /** Called when the server sends a new sheet (after unlock, refund or reset). */
    public void refresh() {
        problems = List.of();
        confirmReset = false;
        rebuildWidgets();
    }

    public void rejected(List<String> reasons) {
        problems = reasons;
        rebuildWidgets();
    }

    public void selectTree(ContentId id) {
        treeId = id;
        activeBranch = "";
        selectedNode = null;
        cameraFitted = false;
        rebuildWidgets();
    }

    public void selectNode(String key) {
        selectedNode = key;
        problems = List.of();
        rebuildWidgets();
    }

    public void unlockSelected() {
        ClientPlayNetworking.send(new TalentActionPayload(TalentActionPayload.Action.ALLOCATE, treeId.toString(), selectedNode));
    }

    @Override
    protected void init() {
        Optional<CharacterSheet> sheet = ClientCharacter.sheet();
        if (sheet.isEmpty()) {
            minecraft.gui.setScreen(null);
            return;
        }
        view = new TalentTreeView(registry, allocator, translator, sheet.get(), treeId);
        if (selectedNode == null || view.node(selectedNode).isEmpty()) {
            selectedNode = view.tree().root();
        }
        treeX = MARGIN + LEFT_W + 8;
        treeY = MARGIN + 44;
        treeW = width - 2 * MARGIN - LEFT_W - RIGHT_W - 16;
        treeH = height - treeY - MARGIN - 20;
        camera.setViewport(treeW, treeH);
        if (!cameraFitted) {
            double[] b = view.bounds();
            camera.fit(b[0], b[1], b[2], b[3], 0.85);
            cameraFitted = true;
        }

        // Branch tabs above the tree; clicking the active tab again shows all branches.
        List<TalentTreeView.Tab> tabs = view.tabs();
        int tabW = Math.min(76, (treeW - 3 * (tabs.size() - 1)) / Math.max(1, tabs.size()));
        int tabsX = treeX + treeW / 2 - (tabs.size() * (tabW + 3) - 3) / 2;
        for (int i = 0; i < tabs.size(); i++) {
            TalentTreeView.Tab tab = tabs.get(i);
            addRenderableWidget(new FrosthainButton(tabsX + i * (tabW + 3), MARGIN + 24, tabW, 14, Component.literal(tab.name()),
                    FrosthainButton.Style.TAB, b -> selectBranch(tab.branch().equals(activeBranch) ? "" : tab.branch()))
                    .selected(tab.branch().equals(activeBranch)));
        }

        // Paths on the left.
        int y = MARGIN + 96;
        for (TalentTreeView.PathEntry path : TalentTreeView.paths(registry, allocator, translator, sheet.get())) {
            String label = (path.accessible() ? "" : "• ") + path.name();
            addRenderableWidget(new FrosthainButton(MARGIN + 6, y, LEFT_W - 8, 16, Component.literal(label), FrosthainButton.Style.TILE,
                    b -> selectTree(path.tree())).selected(path.tree().equals(treeId)));
            y += 19;
        }
        FrosthainButton reset = new FrosthainButton(MARGIN + 6, height - MARGIN - 24, LEFT_W - 8, 16,
                Component.literal(confirmReset ? translator.t("gui.frosthain.talents.reset_confirm") : translator.t("ui.frosthain.talents.reset")),
                FrosthainButton.Style.MENU, b -> {
                    if (confirmReset) {
                        ClientPlayNetworking.send(new TalentActionPayload(TalentActionPayload.Action.RESET_ALL, treeId.toString(), ""));
                    } else {
                        confirmReset = true;
                        rebuildWidgets();
                    }
                });
        reset.active = !sheet.get().talents().isEmpty();
        addRenderableWidget(reset);

        // Unlock / refund at the bottom of the detail panel.
        TalentTreeView.Detail detail = view.detail(selectedNode);
        int detailX = width - MARGIN - RIGHT_W;
        int bottom = height - MARGIN - 8;
        FrosthainButton unlock = new FrosthainButton(detailX + 6, bottom - 18, RIGHT_W - 12, 16,
                Component.literal(translator.t("ui.frosthain.talents.unlock")), FrosthainButton.Style.PRIMARY, b -> unlockSelected());
        unlock.active = detail.canUnlock();
        addRenderableWidget(unlock);
        if (detail.canRefund()) {
            addRenderableWidget(new FrosthainButton(detailX + 6, bottom - 36, RIGHT_W - 12, 15,
                    Component.literal(translator.t("ui.frosthain.talents.refund")), FrosthainButton.Style.MENU,
                    b -> ClientPlayNetworking.send(new TalentActionPayload(TalentActionPayload.Action.REFUND, treeId.toString(), selectedNode))));
        }
    }

    private void selectBranch(String branch) {
        activeBranch = branch;
        if (branch.isEmpty()) {
            double[] b = view.bounds();
            camera.fit(b[0], b[1], b[2], b[3], 0.85);
        } else {
            double[] center = view.branchCenter(branch);
            camera.centerOn(center[0] * 0.75, center[1] * 0.75);
        }
        rebuildWidgets();
    }

    // ---- rendering ---------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0x9010121A, 0xD010121A);
        Ui.panel(g, MARGIN, MARGIN, width - 2 * MARGIN, height - 2 * MARGIN, Theme.PANEL_BACKGROUND);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (view == null) {
            return;
        }
        Ui.ornamentTitle(g, font, Component.literal(view.title()), treeX + treeW / 2, MARGIN + 7, 1.2f);
        leftColumn(g);
        g.fill(treeX, treeY, treeX + treeW, treeY + treeH, 0x660B0D10);
        g.enableScissor(treeX, treeY, treeX + treeW, treeY + treeH);
        tree(g);
        g.disableScissor();
        g.outline(treeX, treeY, treeW, treeH, Theme.FRAME_GOLD_DIM);
        if (!view.accessible()) {
            Component text = Ui.heading(Component.literal(translator.t("ui.frosthain.talents.preview")));
            g.centeredText(font, text, treeX + treeW / 2, treeY + 6, Theme.STATE_BLOCKED);
        }
        detail(g);
        Component hint = Ui.body(Component.literal(translator.t("gui.frosthain.talents.hint")));
        if (font.width(hint) > treeW + 8) {
            hint = Ui.body(Component.literal(font.plainSubstrByWidth(hint.getString(), treeW) + "…"));
        }
        g.centeredText(font, hint, treeX + treeW / 2, height - MARGIN - 15, Theme.TEXT_SECONDARY);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void leftColumn(GuiGraphicsExtractor g) {
        int x = MARGIN + 6;
        int w = LEFT_W - 8;
        Ui.panel(g, x, MARGIN + 24, w, 54, 0xFF111418);
        g.centeredText(font, Ui.body(Component.literal(translator.t("ui.frosthain.talents.points"))), x + w / 2, MARGIN + 28, Theme.TEXT_PRIMARY);
        Ui.scaledCentered(g, font, Ui.heading(Component.literal(String.valueOf(view.availablePoints()))), x + w / 2, MARGIN + 39, 2f,
                Theme.FRAME_GOLD_BRIGHT);
        g.centeredText(font, Ui.body(Component.literal(translator.t("ui.frosthain.talents.character_level", view.characterLevel()))),
                x + w / 2, MARGIN + 64, Theme.TEXT_SECONDARY);
        g.text(font, Ui.heading(Component.literal(translator.t("ui.frosthain.talents.paths"))), x, MARGIN + 84, Theme.FRAME_GOLD, false);
    }

    private void tree(GuiGraphicsExtractor g) {
        nodeRadius = Math.clamp(camera.zoom() * 0.34, 6, 15);
        nodeScreen.clear();
        for (TalentTreeView.NodeView node : view.nodes()) {
            nodeScreen.put(node.key(), new double[]{treeX + camera.toScreenX(node.x()), treeY + camera.toScreenY(node.y())});
        }
        for (TalentTreeView.EdgeView edge : view.edges()) {
            double[] a = nodeScreen.get(edge.from());
            double[] b = nodeScreen.get(edge.to());
            double dim = dim(view.node(edge.to()).orElseThrow());
            if (edge.active()) {
                line(g, a, b, 4, Theme.withAlpha(edge.color(), 0.3 * dim), false);
                line(g, a, b, 2, Theme.withAlpha(Theme.mix(edge.color(), 0xFFFFFFFF, 0.25), dim), false);
            } else {
                line(g, a, b, 1, Theme.withAlpha(0xFF5A5E65, dim), edge.anyOf());
            }
        }
        for (TalentTreeView.NodeView node : view.nodes()) {
            drawNode(g, node);
        }
    }

    private double dim(TalentTreeView.NodeView node) {
        return activeBranch.isEmpty() || node.branch().isEmpty() || node.branch().equals(activeBranch) ? 1.0 : 0.3;
    }

    private void drawNode(GuiGraphicsExtractor g, TalentTreeView.NodeView node) {
        double[] p = nodeScreen.get(node.key());
        boolean root = node.kind() == TalentTreeDefinition.NodeKind.ROOT;
        double r = root ? nodeRadius * 1.4 : node.kind() == TalentTreeDefinition.NodeKind.KEYSTONE ? nodeRadius * 1.15 : nodeRadius;
        double dim = dim(node);
        boolean lit = node.state().isAllocated();
        int ring = switch (node.state()) {
            case LOCKED -> 0xFF55595F;
            case REACHABLE -> Theme.mix(node.color(), 0xFF55595F, 0.6);
            case AVAILABLE -> Theme.mix(node.color(), 0xFF55595F, 0.35);
            default -> node.color();
        };
        if (node.key().equals(selectedNode)) {
            hex(g, p[0], p[1], r + 4, Theme.FRAME_GOLD_BRIGHT);
            hex(g, p[0], p[1], r + 3, 0xFF0B0D10);
        }
        if (lit) {
            hex(g, p[0], p[1], r + 2.5, Theme.withAlpha(node.color(), 0.45 * dim));
        } else if (node.state() == NodeState.AVAILABLE) {
            hex(g, p[0], p[1], r + 2, Theme.withAlpha(Theme.STATE_AVAILABLE, 0.85 * dim));
        }
        hex(g, p[0], p[1], r, Theme.withAlpha(ring, dim));
        hex(g, p[0], p[1], r - 1.5, Theme.withAlpha(lit ? Theme.mix(node.color(), 0xFF101215, 0.6) : 0xFF22252A, Math.max(dim, 0.6)));
        if (node.state() == NodeState.LOCKED && !root) {
            lock(g, (int) Math.round(p[0]), (int) Math.round(p[1]), Theme.withAlpha(0xFF8A8E94, dim));
        } else if (dim >= 1.0) {
            float scale = (float) Math.max(0.5, r * 1.2 / 16.0);
            g.pose().pushMatrix();
            g.pose().translate((float) p[0], (float) p[1]);
            g.pose().scale(scale, scale);
            g.fakeItem(TalentIcons.item(node.icon()), -8, -8);
            g.pose().popMatrix();
        }
        if (node.maxRank() > 1) {
            int pipY = (int) Math.round(p[1] + r * 0.9);
            for (int i = 0; i < node.maxRank(); i++) {
                int pipX = (int) Math.round(p[0] + (i - (node.maxRank() - 1) / 2.0) * 5);
                g.fill(pipX - 1, pipY - 1, pipX + 2, pipY + 2, i < node.rank() ? Theme.FRAME_GOLD_BRIGHT : 0xFF3B3F45);
            }
        }
    }

    /** Flat-topped hexagon (same geometry as {@link HexShape}) filled row by row. */
    private static void hex(GuiGraphicsExtractor g, double cx, double cy, double r, int color) {
        int half = (int) Math.round(r * SQRT3 / 2);
        for (int dy = -half; dy < half; dy++) {
            double w = r - Math.abs(dy + 0.5) / SQRT3;
            g.fill((int) Math.round(cx - w), (int) Math.round(cy) + dy, (int) Math.round(cx + w), (int) Math.round(cy) + dy + 1, color);
        }
    }

    private static void line(GuiGraphicsExtractor g, double[] a, double[] b, int thickness, int color, boolean dashed) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        int steps = (int) Math.max(Math.abs(dx), Math.abs(dy));
        for (int i = 0; i <= steps; i++) {
            if (dashed && (i / 3) % 2 == 1) {
                continue;
            }
            int x = (int) Math.round(a[0] + dx * i / Math.max(steps, 1)) - thickness / 2;
            int y = (int) Math.round(a[1] + dy * i / Math.max(steps, 1)) - thickness / 2;
            g.fill(x, y, x + thickness, y + thickness, color);
        }
    }

    private static void lock(GuiGraphicsExtractor g, int cx, int cy, int color) {
        g.fill(cx - 3, cy - 1, cx + 4, cy + 4, color);
        g.outline(cx - 2, cy - 5, 5, 5, color);
    }

    private void detail(GuiGraphicsExtractor g) {
        TalentTreeView.Detail d = view.detail(selectedNode);
        int x = width - MARGIN - RIGHT_W;
        int w = RIGHT_W - 6;
        int top = MARGIN + 24;
        Ui.panel(g, x, top, w, height - top - MARGIN - 6, 0xFF111418);
        int cx = x + w / 2;
        int y = top + 6;
        g.fill(cx - 12, y, cx + 12, y + 24, Theme.mix(d.color(), 0xFF101215, 0.55));
        g.outline(cx - 12, y, 24, 24, Theme.FRAME_GOLD_BRIGHT);
        g.fakeItem(TalentIcons.item(d.icon()), cx - 8, y + 4);
        y += 28;
        for (var line : font.split(Ui.heading(Component.literal(d.name())), w - 10)) {
            g.centeredText(font, line, cx, y, Theme.FRAME_GOLD_BRIGHT);
            y += font.lineHeight + 1;
        }
        g.centeredText(font, Ui.body(Component.literal(d.kindLabel())).withStyle(s -> s.withItalic(true)), cx, y, Theme.TEXT_SECONDARY);
        y += font.lineHeight + 4;
        int lines = 0;
        for (var line : font.split(Ui.body(Component.literal(d.description())), w - 12)) {
            if (lines++ == 5) {
                break;
            }
            g.text(font, line, x + 6, y, Theme.TEXT_PRIMARY, false);
            y += font.lineHeight + 1;
        }
        y += 3;
        if (d.state() != NodeState.ORIGIN) {
            g.text(font, Ui.body(Component.literal(d.rankText())), x + 6, y, Theme.TEXT_SECONDARY, false);
            y += font.lineHeight + 3;
        }
        if (!d.requirements().isEmpty()) {
            g.text(font, Ui.heading(Component.literal(translator.t("ui.frosthain.talents.requires"))), x + 6, y, Theme.FRAME_GOLD, false);
            y += font.lineHeight + 2;
            for (TalentTreeView.Requirement requirement : d.requirements()) {
                int color = requirement.met() ? Theme.STATE_AVAILABLE : Theme.STATE_BLOCKED;
                g.text(font, Component.literal(requirement.met() ? "✔" : "✘"), x + 6, y, color, false);
                y = Ui.paragraph(g, font, Component.literal(requirement.text()), x + 16, y, w - 22, color);
            }
        }
        int bottom = height - MARGIN - 8;
        int statusY = bottom - (d.canRefund() ? 62 : 44);
        g.centeredText(font, Ui.heading(Component.literal(d.statusText())), cx, statusY, d.statusColor());
        if (d.state() != NodeState.ORIGIN && d.state() != NodeState.MAXED) {
            g.centeredText(font, Ui.body(Component.literal(d.costText())), cx, statusY + font.lineHeight + 1, Theme.TEXT_PRIMARY);
        }
        int py = statusY - 4;
        for (String problem : problems) {
            py -= font.lineHeight + 1;
            g.centeredText(font, Ui.body(Component.literal(font.plainSubstrByWidth(problem, w - 8))), cx, py, Theme.STATE_BLOCKED);
        }
    }

    // ---- input -------------------------------------------------------------------------------------

    private boolean inTree(double x, double y) {
        return x >= treeX && x < treeX + treeW && y >= treeY && y < treeY + treeH;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (view != null && inTree(event.x(), event.y())) {
            for (Map.Entry<String, double[]> node : nodeScreen.entrySet()) {
                if (HexShape.contains(node.getValue()[0], node.getValue()[1], nodeRadius * 1.2, event.x(), event.y())) {
                    selectNode(node.getKey());
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (inTree(event.x(), event.y())) {
            camera.panPixels(dragX, dragY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inTree(mouseX, mouseY) && scrollY != 0) {
            camera.zoomAt(scrollY > 0 ? 1.15 : 1 / 1.15, mouseX - treeX, mouseY - treeY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
