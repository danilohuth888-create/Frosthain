package net.frosthain.mod.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.frosthain.core.ContentId;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.client.ClientCharacter;
import net.frosthain.mod.client.gui.Ui;
import net.frosthain.ui.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Small character badge (name, class, level, progress to the next level) top left – HUD panel at 70 % slate (Design 2.0). */
public final class CharacterHud implements HudElement {

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        ClientCharacter.state().ifPresent(state -> {
            var font = Minecraft.getInstance().font;
            Component name = Ui.heading(Component.literal(state.name()));
            Component line = Ui.body(Component.translatable("gui.frosthain.hud.class_level",
                    Component.translatable("class." + ContentId.parse(state.characterClass()).toTranslationFragment()), state.level()));
            int w = Math.max(font.width(name), font.width(line)) + 12;
            int h = font.lineHeight * 2 + 13;
            g.fill(4, 4, 4 + w, 4 + h, Theme.HUD_PANEL_BACKGROUND);
            g.outline(4, 4, w, h, Theme.FRAME_GOLD_DIM);
            g.text(font, name, 10, 8, Theme.FRAME_GOLD_BRIGHT, true);
            g.text(font, line, 10, 9 + font.lineHeight, Theme.TEXT_SECONDARY, true);
            // Progress to the next level (FROSTHAIN experience, not vanilla XP).
            int barY = 4 + h - 5;
            g.fill(8, barY, 4 + w - 4, barY + 2, 0xC0101215);
            g.fill(8, barY, 8 + (int) Math.round((w - 12) * levelProgress()), barY + 2, Theme.FRAME_GOLD_BRIGHT);
        });
    }

    private static double levelProgress() {
        return ClientCharacter.sheet().flatMap(sheet -> FrosthainMod.content().progression().map(rules -> {
            int level = sheet.characterLevel();
            List<Integer> thresholds = rules.experienceThresholds();
            if (level >= thresholds.size()) {
                return 1.0;
            }
            double from = thresholds.get(level - 1);
            double to = thresholds.get(level);
            return Math.clamp((sheet.experience() - from) / (to - from), 0.0, 1.0);
        })).orElse(0.0);
    }
}
