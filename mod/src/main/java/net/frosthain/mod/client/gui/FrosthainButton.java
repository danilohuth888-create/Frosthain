package net.frosthain.mod.client.gui;

import net.frosthain.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Button in Design 2.0: slate surface, thin gold frame, gold text when hovered or selected. */
public class FrosthainButton extends AbstractButton {

    public enum Style { MENU, TILE, TAB, PRIMARY }

    private final Consumer<FrosthainButton> action;
    private final Style style;
    private boolean selected;

    public FrosthainButton(int x, int y, int width, int height, Component label, Style style, Consumer<FrosthainButton> action) {
        super(x, y, width, height, label);
        this.style = style;
        this.action = action;
    }

    public FrosthainButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.accept(this);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        boolean highlight = active && (isHoveredOrFocused() || selected);
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int background = switch (style) {
            case PRIMARY -> highlight ? 0xF03A3020 : 0xE62B2418;
            case TAB -> selected ? 0xE62A2620 : 0xB317191D;
            default -> highlight ? 0xF0262A30 : Theme.PANEL_BACKGROUND;
        };
        g.fill(x, y, x + w, y + h, background);
        int frame = !active ? Theme.NODE_LOCKED : highlight || style == Style.PRIMARY ? Theme.FRAME_GOLD_BRIGHT : Theme.FRAME_GOLD_DIM;
        g.outline(x, y, w, h, frame);
        if (highlight) {
            Ui.corners(g, x, y, w, h, Theme.FRAME_GOLD_BRIGHT, 4);
        }
        if (selected && style == Style.TAB) {
            g.horizontalLine(x + 2, x + w - 3, y + h - 2, Theme.FRAME_GOLD_BRIGHT);
        }
        int color = !active ? Theme.TEXT_DISABLED : highlight ? Theme.FRAME_GOLD_BRIGHT : Theme.TEXT_PRIMARY;
        // Cinzel for menus and tabs; narrow buttons fall back to EB Garamond, then to a shortened label.
        Font font = Minecraft.getInstance().font;
        Component shown = style == Style.TILE ? Ui.body(getMessage()) : Ui.heading(getMessage());
        if (font.width(shown) > w - 6) {
            shown = Ui.body(getMessage());
        }
        if (font.width(shown) > w - 6) {
            shown = Ui.body(Component.literal(font.plainSubstrByWidth(getMessage().getString(), w - 10) + "…"));
        }
        g.centeredText(font, shown, x + w / 2, y + (h - font.lineHeight) / 2 + 1, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
