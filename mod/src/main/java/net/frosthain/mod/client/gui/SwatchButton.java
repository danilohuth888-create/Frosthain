package net.frosthain.mod.client.gui;

import net.frosthain.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** Small colour field for the appearance step; the selected one has a bright gold frame. */
public final class SwatchButton extends AbstractButton {

    private final int color;
    private final boolean selected;
    private final Runnable action;

    public SwatchButton(int x, int y, int size, String hexColor, boolean selected, Runnable action) {
        super(x, y, size, size, Component.literal(hexColor));
        this.color = 0xFF000000 | Integer.parseInt(hexColor.substring(1), 16);
        this.selected = selected;
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int s = getWidth();
        g.fill(x, y, x + s, y + s, color);
        if (selected) {
            g.outline(x - 1, y - 1, s + 2, s + 2, Theme.FRAME_GOLD_BRIGHT);
            g.outline(x, y, s, s, 0xFF101215);
        } else {
            g.outline(x, y, s, s, isHoveredOrFocused() ? Theme.FRAME_GOLD : 0xFF101215);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
