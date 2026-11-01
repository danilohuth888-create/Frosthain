package net.frosthain.mod.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.content.QuestDefinition;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.quest.QuestRules;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.client.ClientQuests;
import net.frosthain.mod.client.gui.FrosthainButton;
import net.frosthain.mod.client.gui.Ui;
import net.frosthain.mod.network.QuestActionPayload;
import net.frosthain.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Quest journal on key J (Design 2.0, view "Questjournal"): categories with counts on the left, an open
 * parchment page with description, checklist and rewards on the right; quests can be tracked in the HUD.
 */
public final class QuestJournalScreen extends Screen {

    public enum Category {
        MAIN("main"), SIDE("side"), DONE("done");

        private final String key;

        Category(String key) {
            this.key = key;
        }
    }

    private static final int MARGIN = 10;
    private static final int LEFT_W = 132;
    private static final int PARCHMENT_FRAME = 0xFF8C6B3E;

    private final ContentRegistry registry = FrosthainMod.content();
    private final QuestRules rules = new QuestRules(registry);
    private Category category = Category.MAIN;
    private ContentId selected;

    public QuestJournalScreen() {
        super(Component.translatable("gui.frosthain.journal.title"));
    }

    public void refresh() {
        rebuildWidgets();
    }

    public void show(Category next) {
        category = next;
        selected = null;
        rebuildWidgets();
    }

    private List<ContentId> quests(Category of) {
        QuestLog log = ClientQuests.log();
        Predicate<ContentId> filter = switch (of) {
            case MAIN -> id -> log.isActive(id) && definition(id).kind().isMain();
            case SIDE -> id -> log.isActive(id) && !definition(id).kind().isMain();
            case DONE -> log::isCompleted;
        };
        return log.quests().keySet().stream().filter(id -> registry.contains(ContentType.QUESTS, id)).filter(filter).toList();
    }

    private QuestDefinition definition(ContentId id) {
        return registry.get(ContentType.QUESTS, id).orElseThrow();
    }

    @Override
    protected void init() {
        List<ContentId> current = quests(category);
        if (selected == null || !current.contains(selected)) {
            selected = current.isEmpty() ? null : current.getFirst();
        }
        int x = MARGIN + 8;
        int y = MARGIN + 30;
        for (Category c : Category.values()) {
            Component label = Component.translatable("gui.frosthain.journal." + c.key).append(" (" + quests(c).size() + ")");
            addRenderableWidget(new FrosthainButton(x, y, LEFT_W - 8, 18, label, FrosthainButton.Style.TAB, b -> show(c))
                    .selected(c == category));
            y += 22;
        }
        y += 8;
        for (ContentId quest : current) {
            addRenderableWidget(new FrosthainButton(x + 6, y, LEFT_W - 14, 16, Component.translatable(questKey(quest)),
                    FrosthainButton.Style.TILE, b -> {
                        selected = quest;
                        rebuildWidgets();
                    }).selected(quest.equals(selected)));
            y += 19;
        }
        addRenderableWidget(new FrosthainButton(x, height - MARGIN - 26, LEFT_W - 8, 16,
                Component.translatable(ClientQuests.trackerExpanded() ? "gui.frosthain.journal.tracker_collapse" : "gui.frosthain.journal.tracker_expand"),
                FrosthainButton.Style.MENU, b -> {
                    ClientQuests.toggleTracker();
                    rebuildWidgets();
                }));
        if (selected != null && ClientQuests.log().isActive(selected)) {
            boolean pinned = ClientQuests.log().pinned().contains(selected);
            FrosthainButton pin = new FrosthainButton(width - MARGIN - 8 - 150, height - MARGIN - 30, 140, 16,
                    Component.translatable(pinned ? "gui.frosthain.journal.unpin" : "gui.frosthain.journal.pin"),
                    FrosthainButton.Style.PRIMARY, b -> ClientPlayNetworking.send(new QuestActionPayload(!pinned, selected.toString())));
            pin.active = pinned || ClientQuests.log().pinned().size() < rules.maxPinned();
            addRenderableWidget(pin);
        }
    }

    private static String questKey(ContentId quest) {
        return "quest." + quest.toTranslationFragment();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0x9010121A, 0xD010121A);
        Ui.panel(g, MARGIN, MARGIN, width - 2 * MARGIN, height - 2 * MARGIN, Theme.PANEL_BACKGROUND);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Ui.ornamentTitle(g, font, title, width / 2, MARGIN + 7, 1.3f);
        page(g);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    /** Parchment page (Design 2.0: parchment #E7DABF, ink #3A2F22). */
    private void page(GuiGraphicsExtractor g) {
        int x = MARGIN + LEFT_W + 10;
        int y = MARGIN + 30;
        int w = width - x - MARGIN - 8;
        int h = height - y - MARGIN - 8;
        g.fill(x, y, x + w, y + h, Theme.PARCHMENT);
        g.fillGradient(x, y, x + w, y + 14, 0x40000000, 0x00000000);
        g.outline(x, y, w, h, PARCHMENT_FRAME);
        Ui.corners(g, x, y, w, h, Theme.FRAME_GOLD, 5);
        int tx = x + 12;
        int tw = w - 24;
        int ty = y + 10;
        if (selected == null) {
            Ui.paragraph(g, font, Component.translatable("gui.frosthain.journal.empty"), tx, ty, tw, Theme.INK);
            return;
        }
        QuestDefinition quest = definition(selected);
        QuestLog.Progress progress = ClientQuests.log().progress(selected).orElseThrow();
        Ui.diamond(g, tx + 3, ty + 6, quest.kind().isMain() ? Theme.FRAME_GOLD : Theme.TEXT_DISABLED);
        g.pose().pushMatrix();
        g.pose().translate(tx + 12, ty);
        g.pose().scale(1.3f, 1.3f);
        g.text(font, Ui.heading(Component.translatable(questKey(selected))), 0, 0, Theme.INK, false);
        g.pose().popMatrix();
        ty += 16;
        g.text(font, Ui.body(Component.translatable("ui.frosthain.quests.kind." + quest.kind().name().toLowerCase(java.util.Locale.ROOT)))
                .withStyle(s -> s.withItalic(true)), tx + 12, ty, 0xFF6B5A44, false);
        ty += font.lineHeight + 6;
        ty = Ui.paragraph(g, font, Component.translatable(questKey(selected) + ".desc"), tx, ty, tw, Theme.INK) + 6;
        g.horizontalLine(tx, tx + tw, ty, 0x608C6B3E);
        ty += 6;

        boolean active = progress.status() == QuestLog.Status.ACTIVE;
        List<QuestDefinition.Objective> current = active ? rules.currentObjectives(quest, progress) : List.of();
        for (QuestDefinition.Objective objective : quest.objectives()) {
            boolean done = progress.count(objective.key()) >= objective.count();
            if (!done && !current.contains(objective)) {
                continue;
            }
            String text = Component.translatable(objective.translationKey(quest)).getString();
            if (objective.count() > 1) {
                text += " (" + progress.count(objective.key()) + "/" + objective.count() + ")";
            }
            g.fill(tx, ty + 1, tx + 8, ty + 9, done ? Theme.FOREST_GREEN : 0x30000000);
            g.outline(tx, ty + 1, 8, 8, Theme.INK);
            if (done) {
                g.text(font, Component.literal("✔"), tx + 1, ty + 1, 0xFFFFFFFF, false);
            }
            ty = Ui.paragraph(g, font, Component.literal(text), tx + 13, ty, tw - 13, done ? 0xFF6B5A44 : Theme.INK) + 3;
        }
        ty += 6;
        g.text(font, Ui.heading(Component.translatable("gui.frosthain.journal.rewards")), tx, ty, 0xFF7A5A26, false);
        ty += font.lineHeight + 3;
        g.text(font, Ui.body(Component.translatable("gui.frosthain.journal.reward_xp", quest.experience())), tx + 4, ty, Theme.INK, false);
        if (!quest.followUps().isEmpty() && active) {
            ty += font.lineHeight + 2;
            g.text(font, Ui.body(Component.translatable("gui.frosthain.journal.follow_up")).withStyle(s -> s.withItalic(true)),
                    tx + 4, ty, 0xFF6B5A44, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
