package net.frosthain.mod.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.frosthain.core.ContentId;
import net.frosthain.mod.client.ClientCharacter;
import net.frosthain.mod.client.gui.Ui;
import net.frosthain.ui.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Small character badge (name, class, level) in the top-left corner – HUD panel at 70 % slate (Design 2.0). */
public final class CharacterHud implements HudElement {

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        ClientCharacter.state().ifPresent(state -> {
            var font = Minecraft.getInstance().font;
            Component name = Ui.heading(Component.literal(state.name()));
            Component line = Ui.body(Component.translatable("gui.frosthain.hud.class_level",
                    Component.translatable("class." + ContentId.parse(state.characterClass()).toTranslationFragment()), state.level()));
            int w = Math.max(font.width(name), font.width(line)) + 12;
            int h = font.lineHeight * 2 + 9;
            g.fill(4, 4, 4 + w, 4 + h, Theme.HUD_PANEL_BACKGROUND);
            g.outline(4, 4, w, h, Theme.FRAME_GOLD_DIM);
            g.text(font, name, 10, 8, Theme.FRAME_GOLD_BRIGHT, true);
            g.text(font, line, 10, 9 + font.lineHeight, Theme.TEXT_SECONDARY, true);
        });
    }
}
