package net.frosthain.mod.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.frosthain.core.ContentId;
import net.frosthain.core.content.PowerDefinition;
import net.frosthain.core.content.ResourceDefinition;
import net.frosthain.core.power.PowerRules;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.client.ClientPowers;
import net.frosthain.mod.client.gui.TalentIcons;
import net.frosthain.mod.client.gui.Ui;
import net.frosthain.mod.network.PowerStatePayload;
import net.frosthain.ui.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Ability bar (Design 2.0, reference "Fähigkeitenleiste"): six slots in the bottom-right corner next to the hotbar,
 * locked slots with a lock, cooldown darkening with seconds, red tint without enough resource, the key below each
 * slot and the class resource as a thin bar above.
 */
public final class AbilityBarHud implements HudElement {

    private static final int SLOT = 18;
    private static final int GAP = 2;
    private static final int WIDTH = PowerRules.SLOTS * SLOT + (PowerRules.SLOTS - 1) * GAP;

    private final KeyMapping[] keys;

    public AbilityBarHud(KeyMapping[] keys) {
        this.keys = keys;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        if (ClientPowers.state().isEmpty()) {
            return;
        }
        PowerStatePayload state = ClientPowers.state().get();
        Font font = Minecraft.getInstance().font;
        int x0 = g.guiWidth() - WIDTH - 4;
        int y0 = g.guiHeight() - SLOT - 11;

        if (!state.resource().isEmpty() && state.maximum() > 0) {
            ResourceDefinition resource = FrosthainMod.content().get(ContentType.RESOURCES, ContentId.parse(state.resource())).orElse(null);
            int color = resource == null ? Theme.RUNE_BLUE : 0xFF000000 | Integer.parseInt(resource.color().substring(1), 16);
            int barY = y0 - 6;
            g.fill(x0, barY, x0 + WIDTH, barY + 3, 0xC0101215);
            g.fill(x0, barY, x0 + WIDTH * Math.min(state.current(), state.maximum()) / state.maximum(), barY + 3, color);
            Component label = Ui.body(Component.translatable("gui.frosthain.power.resource",
                    resource == null ? Component.literal("") : Component.translatable(resource.translationKey()), state.current(), state.maximum()));
            g.text(font, label, x0 + WIDTH - font.width(label), barY - font.lineHeight - 1, Theme.TEXT_PRIMARY, true);
        }

        for (int i = 0; i < PowerRules.SLOTS; i++) {
            int x = x0 + i * (SLOT + GAP);
            g.fill(x, y0, x + SLOT, y0 + SLOT, Theme.HUD_PANEL_BACKGROUND);
            g.outline(x, y0, SLOT, SLOT, i < state.powers().size() ? Theme.FRAME_GOLD_DIM : 0xFF3A3D42);
            if (i < state.powers().size()) {
                g.fakeItem(TalentIcons.power(ContentId.parse(state.powers().get(i)).path()), x + 1, y0 + 1);
            } else {
                g.fill(x + 6, y0 + 8, x + 13, y0 + 13, Theme.TEXT_DISABLED);
                g.outline(x + 7, y0 + 4, 5, 5, Theme.TEXT_DISABLED);
            }
        }
        g.nextStratum();
        for (int i = 0; i < state.powers().size() && i < PowerRules.SLOTS; i++) {
            int x = x0 + i * (SLOT + GAP);
            PowerDefinition power = FrosthainMod.content().get(ContentType.POWERS, ContentId.parse(state.powers().get(i))).orElse(null);
            if (power == null) {
                continue;
            }
            if (power.cost() > state.current()) {
                g.fill(x + 1, y0 + 1, x + SLOT - 1, y0 + SLOT - 1, 0x60B02020);
            }
            int remaining = state.cooldowns().get(i);
            if (remaining > 0 && power.cooldownTicks() > 0) {
                int covered = (SLOT - 2) * Math.min(remaining, power.cooldownTicks()) / power.cooldownTicks();
                g.fill(x + 1, y0 + 1, x + SLOT - 1, y0 + 1 + covered, 0xB0000000);
                String seconds = String.valueOf((remaining + 19) / 20);
                g.text(font, seconds, x + (SLOT - font.width(seconds)) / 2 + 1, y0 + 5, 0xFFFFFFFF, true);
            }
        }
        for (int i = 0; i < PowerRules.SLOTS && i < keys.length; i++) {
            String key = keys[i].getTranslatedKeyMessage().getString();
            if (key.length() > 2) {
                key = key.substring(0, 2);
            }
            int x = x0 + i * (SLOT + GAP);
            g.text(font, key, x + (SLOT - font.width(key)) / 2, y0 + SLOT + 1, i < state.powers().size() ? Theme.TEXT_SECONDARY : Theme.TEXT_DISABLED, true);
        }
    }
}
