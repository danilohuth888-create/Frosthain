package net.frosthain.mod.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.frosthain.core.quest.QuestRules;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.client.ClientCharacter;
import net.frosthain.mod.client.ClientQuests;
import net.frosthain.mod.client.gui.Ui;
import net.frosthain.mod.network.QuestMarkerPayload;
import net.frosthain.ui.Theme;
import net.frosthain.ui.Translator;
import net.frosthain.ui.quest.QuestTrackerView;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Quest tracker on the right edge (Design 2.0, Survival-HUD): collapsed by default ("Quests (n)"), expanded it
 * shows up to three pinned quests with their current objectives. Layout from {@link QuestTrackerView}.
 */
public final class QuestTrackerHud implements HudElement {

    /** Minecraft GUI pixels: one font line is 9 px. */
    private static final QuestTrackerView.Style STYLE = new QuestTrackerView.Style(5, 9, 9, 9, 9, 1.2, 4);
    private static final int MAX_WIDTH = 150;
    /** Below the status effect icons in the top-right corner. */
    private static final int TOP = 54;

    private Translator translator;
    private String translatorLanguage;

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        if (ClientCharacter.state().isEmpty() || ClientQuests.log().activeQuests().isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        String language = minecraft.getLanguageManager().getSelected();
        if (translator == null || !language.equals(translatorLanguage)) {
            translator = new Translator(FrosthainMod.content(), language);
            translatorLanguage = language;
        }
        QuestTrackerView.Layout layout = QuestTrackerView.layout(FrosthainMod.content(), new QuestRules(FrosthainMod.content()),
                ClientQuests.log(), translator, (text, role, size) -> font.width(styled(text, role)), MAX_WIDTH,
                !ClientQuests.trackerExpanded(), STYLE);
        int width = layout.collapsed()
                ? layout.lines().stream().mapToInt(line -> font.width(styled(line.text(), line.role()))).max().orElse(40) + 2 * STYLE.padding() + 2
                : layout.width();
        int x = g.guiWidth() - width - 4;
        g.fill(x, TOP, x + width, TOP + layout.height(), Theme.HUD_PANEL_BACKGROUND);
        g.outline(x, TOP, width, layout.height(), Theme.FRAME_GOLD_DIM);
        for (QuestTrackerView.Line line : layout.lines()) {
            int lx = x + line.x();
            int ly = TOP + line.y() + 1;
            marker(g, line.marker(), x + STYLE.padding(), ly);
            g.text(font, styled(line.text(), line.role()), lx, ly, line.color(), true);
        }
        waypoint(g, font, minecraft, TOP + layout.height() + 3);
    }

    /** Distance and compass direction to the current objective's place (shrine, rune stone) under the tracker. */
    private static void waypoint(GuiGraphicsExtractor g, Font font, Minecraft minecraft, int y) {
        QuestMarkerPayload marker = ClientQuests.marker();
        if (!marker.present() || minecraft.player == null) {
            return;
        }
        double dx = marker.x() + 0.5 - minecraft.player.getX();
        double dz = marker.z() + 0.5 - minecraft.player.getZ();
        long distance = Math.round(Math.sqrt(dx * dx + dz * dz));
        // 0° = north (−Z), clockwise; eight sectors of 45°.
        double angle = (Math.toDegrees(Math.atan2(dx, -dz)) + 360) % 360;
        String[] directions = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        String direction = directions[(int) Math.round(angle / 45) % 8];
        Component text = Ui.body(Component.translatable("gui.frosthain.marker.distance", Component.translatable(marker.labelKey()),
                distance, Component.translatable("gui.frosthain.dir." + direction)));
        int w = font.width(text) + 16;
        int x = g.guiWidth() - w - 4;
        g.fill(x, y, x + w, y + font.lineHeight + 5, Theme.HUD_PANEL_BACKGROUND);
        g.outline(x, y, w, font.lineHeight + 5, Theme.FRAME_GOLD_DIM);
        Ui.diamond(g, x + 6, y + 6, Theme.RUNE_BLUE);
        g.text(font, text, x + 12, y + 3, Theme.TEXT_PRIMARY, true);
    }

    private static Component styled(String text, Theme.FontRole role) {
        Component plain = Component.literal(text);
        return switch (role) {
            case HEADING -> Ui.heading(plain);
            case BODY -> Ui.body(plain);
            case BODY_ITALIC -> Ui.body(plain).withStyle(s -> s.withItalic(true));
        };
    }

    static void marker(GuiGraphicsExtractor g, QuestTrackerView.Marker marker, int x, int y) {
        switch (marker) {
            case DIAMOND_MAIN -> Ui.diamond(g, x + 3, y + 4, Theme.FRAME_GOLD_BRIGHT);
            case DIAMOND_OTHER -> Ui.diamond(g, x + 3, y + 4, Theme.TEXT_SECONDARY);
            case BOX_OPEN -> g.outline(x, y + 1, 7, 7, Theme.TEXT_SECONDARY);
            case BOX_DONE -> {
                g.fill(x, y + 1, x + 7, y + 8, Theme.FOREST_GREEN);
                g.outline(x, y + 1, 7, 7, Theme.STATE_AVAILABLE);
            }
            case NONE -> {
            }
        }
    }
}
