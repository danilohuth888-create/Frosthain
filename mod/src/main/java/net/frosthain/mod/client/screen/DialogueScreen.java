package net.frosthain.mod.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.content.DialogueDefinition;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.client.gui.Ui;
import net.frosthain.mod.network.DialogueChoicePayload;
import net.frosthain.mod.network.DialogueStatePayload;
import net.frosthain.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC conversation (Design 2.0, view "NPC-Dialog", reference F): the NPC stays visible in the world, the HUD is
 * hidden, the line is on the left under a name plate, answers on the right (mouse, 1–9, arrows, Enter).
 * The server decides which answers exist and what they do; this screen only shows them.
 */
public final class DialogueScreen extends Screen {

    private static final int MARGIN = 10;
    private static final int ROW_GAP = 3;

    private DialogueStatePayload state;
    private DialogueDefinition dialogue;
    private int selected;
    private boolean hudWasHidden;
    private boolean closedByServer;
    private final List<int[]> rows = new ArrayList<>();

    public DialogueScreen(DialogueStatePayload state) {
        super(Component.translatable("npc.frosthain.eldrin"));
        update(state);
    }

    /** Server → client entry point: opens, updates or closes the conversation. */
    public static void accept(Minecraft client, DialogueStatePayload payload) {
        if (client.gui.screen() instanceof DialogueScreen screen) {
            if (payload.open()) {
                screen.update(payload);
            } else {
                screen.closedByServer = true;
                client.gui.setScreen(null);
            }
        } else if (payload.open() && client.player != null) {
            client.gui.setScreen(new DialogueScreen(payload));
        }
    }

    public void update(DialogueStatePayload payload) {
        state = payload;
        dialogue = FrosthainMod.content().get(ContentType.DIALOGUES, ContentId.parse(payload.dialogue())).orElse(null);
        selected = 0;
    }

    public DialogueStatePayload state() {
        return state;
    }

    /** Sends the answer with the given index (0-based) to the server. */
    public void choose(int index) {
        if (index >= 0 && index < choices().size()) {
            ClientPlayNetworking.send(new DialogueChoicePayload(index));
        }
    }

    private List<Component> choices() {
        if (dialogue == null) {
            return List.of();
        }
        if (state.atEnd()) {
            return List.of(Component.translatable("ui.frosthain.dialogue.continue"));
        }
        return state.choices().stream()
                .map(choice -> (Component) Component.translatable(dialogue.choiceKey(state.node(), choice)))
                .toList();
    }

    @Override
    public void added() {
        super.added();
        hudWasHidden = minecraft.gui.hud.isHidden();
        if (!hudWasHidden) {
            minecraft.gui.hud.toggle();
        }
    }

    @Override
    public void removed() {
        if (!hudWasHidden && minecraft.gui.hud.isHidden()) {
            minecraft.gui.hud.toggle();
        }
        super.removed();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // The NPC and the world stay visible (reference F); only the lower part gets darker.
        g.fillGradient(0, height / 2, width, height, 0x00000000, 0xB0000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (dialogue == null) {
            return;
        }
        int panelH = Math.min(118, Math.max(84, height * 2 / 5));
        int panelY = height - panelH - MARGIN;
        int panelW = width - 2 * MARGIN;
        int textW = panelW * 58 / 100;

        Ui.panel(g, MARGIN, panelY, textW, panelH, Theme.PANEL_BACKGROUND);
        Component name = Ui.heading(Component.translatable("npc." + dialogue.speaker().toTranslationFragment()));
        int plateW = font.width(name) + 24;
        Ui.panel(g, MARGIN + 18, panelY - 9, plateW, 18, 0xF2201A12);
        g.outline(MARGIN + 18, panelY - 9, plateW, 18, Theme.FRAME_GOLD_BRIGHT);
        g.text(font, name, MARGIN + 30, panelY - 4, Theme.FRAME_GOLD_BRIGHT, true);
        Component title = Ui.body(Component.translatable("npc." + dialogue.speaker().toTranslationFragment() + ".title"))
                .withStyle(s -> s.withItalic(true));
        g.text(font, title, MARGIN + 18 + plateW + 8, panelY - 4, Theme.TEXT_SECONDARY, true);
        Ui.paragraph(g, font, Component.translatable(dialogue.lineKey(state.node())), MARGIN + 14, panelY + 16, textW - 28, Theme.TEXT_PRIMARY);

        int choiceX = MARGIN + textW + 6;
        int choiceW = panelW - textW - 6;
        Ui.panel(g, choiceX, panelY, choiceW, panelH, Theme.PANEL_BACKGROUND);
        rows.clear();
        int y = panelY + 8;
        List<Component> choices = choices();
        for (int i = 0; i < choices.size(); i++) {
            List<FormattedCharSequence> lines = font.split(Ui.body(choices.get(i)), choiceW - 30);
            int rowH = Math.max(14, lines.size() * (font.lineHeight + 1) + 4);
            boolean active = i == selected;
            if (active) {
                g.fill(choiceX + 4, y, choiceX + choiceW - 4, y + rowH, 0xF02A2418);
                g.outline(choiceX + 4, y, choiceW - 8, rowH, Theme.FRAME_GOLD_BRIGHT);
            }
            g.text(font, Component.literal(active ? "▶" : String.valueOf(i + 1)), choiceX + 9, y + 3,
                    active ? Theme.FRAME_GOLD_BRIGHT : Theme.TEXT_SECONDARY, false);
            int ly = y + 3;
            for (FormattedCharSequence line : lines) {
                g.text(font, line, choiceX + 22, ly, active ? Theme.FRAME_GOLD_BRIGHT : Theme.TEXT_PRIMARY, false);
                ly += font.lineHeight + 1;
            }
            rows.add(new int[]{choiceX + 4, y, choiceX + choiceW - 4, y + rowH});
            y += rowH + ROW_GAP;
        }
        Component hint = Ui.body(Component.translatable("gui.frosthain.dialogue.hint"));
        boolean roomBelowChoices = y <= panelY + panelH - font.lineHeight - 4;
        if (roomBelowChoices && font.width(hint) <= choiceW - 12) {
            g.text(font, hint, choiceX + 8, panelY + panelH - font.lineHeight - 4, Theme.TEXT_DISABLED, false);
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private int rowAt(double x, double y) {
        for (int i = 0; i < rows.size(); i++) {
            int[] r = rows.get(i);
            if (x >= r[0] && x < r[2] && y >= r[1] && y < r[3]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        int row = rowAt(mouseX, mouseY);
        if (row >= 0) {
            selected = row;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int row = rowAt(event.x(), event.y());
        if (row >= 0) {
            choose(row);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int count = choices().size();
        int key = event.key();
        if (key >= '1' && key <= '9' && key - '1' < count) {
            choose(key - '1');
            return true;
        }
        switch (key) {
            case 265 -> selected = Math.floorMod(selected - 1, Math.max(1, count)); // up
            case 264 -> selected = Math.floorMod(selected + 1, Math.max(1, count)); // down
            case 257, 335, 32 -> choose(selected); // enter, keypad enter, space
            default -> {
                return super.keyPressed(event);
            }
        }
        return true;
    }

    @Override
    public void onClose() {
        if (!closedByServer) {
            ClientPlayNetworking.send(new DialogueChoicePayload(DialogueChoicePayload.LEAVE));
        }
        minecraft.gui.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
