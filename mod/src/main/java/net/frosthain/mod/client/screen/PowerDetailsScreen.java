package net.frosthain.mod.client.screen;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.Modifier;
import net.frosthain.core.content.PowerDefinition;
import net.frosthain.core.content.TalentTreeDefinition;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.client.ClientPowers;
import net.frosthain.mod.client.gui.FrosthainButton;
import net.frosthain.mod.client.gui.TalentIcons;
import net.frosthain.mod.client.gui.Ui;
import net.frosthain.ui.Theme;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ability details on key Y (Design 2.0, reference "Fähigkeitsdetails"): known powers with their key and the powers
 * that talents unlock on the left; icon, description and the numbers from the power data on the right.
 */
public final class PowerDetailsScreen extends Screen {

    private static final int MARGIN = 10;
    private static final int LEFT_W = 132;

    private final ContentRegistry registry = FrosthainMod.content();
    private final KeyMapping[] keys;
    private final List<ContentId> known = new ArrayList<>();
    /** Powers granted by talents the character does not have yet, with the talent's name. */
    private final Map<ContentId, Component> lockedByTalent = new LinkedHashMap<>();
    private ContentId selected;

    public PowerDetailsScreen(KeyMapping[] keys) {
        super(Component.translatable("gui.frosthain.powers.title"));
        this.keys = keys;
    }

    public void select(ContentId power) {
        selected = power;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        known.clear();
        lockedByTalent.clear();
        ClientPowers.state().ifPresent(state -> state.powers().forEach(id -> known.add(ContentId.parse(id))));
        for (TalentTreeDefinition tree : registry.all(ContentType.TALENT_TREES).values()) {
            for (TalentTreeDefinition.TalentNode node : tree.nodes().values()) {
                for (Modifier effect : node.effects()) {
                    if (effect instanceof Modifier.GrantPower grant && !known.contains(grant.power())) {
                        lockedByTalent.putIfAbsent(grant.power(), Component.translatable(node.translationKey(tree)));
                    }
                }
            }
        }
        if (selected == null) {
            selected = !known.isEmpty() ? known.getFirst() : lockedByTalent.keySet().stream().findFirst().orElse(null);
        }
        int x = MARGIN + 8;
        int y = MARGIN + 42;
        for (ContentId power : known) {
            addRenderableWidget(new FrosthainButton(x, y, LEFT_W - 8, 16, Component.translatable(nameKey(power)),
                    FrosthainButton.Style.TILE, b -> select(power)).selected(power.equals(selected)));
            y += 19;
        }
        y += 18;
        for (ContentId power : lockedByTalent.keySet()) {
            if (y > height - MARGIN - 24) {
                break;
            }
            addRenderableWidget(new FrosthainButton(x, y, LEFT_W - 8, 15, Component.literal("• ").append(Component.translatable(nameKey(power))),
                    FrosthainButton.Style.TILE, b -> select(power)).selected(power.equals(selected)));
            y += 17;
        }
    }

    private static String nameKey(ContentId power) {
        return "power." + power.toTranslationFragment();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0x9010121A, 0xD010121A);
        Ui.panel(g, MARGIN, MARGIN, width - 2 * MARGIN, height - 2 * MARGIN, Theme.PANEL_BACKGROUND);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Ui.ornamentTitle(g, font, title, width / 2, MARGIN + 7, 1.3f);
        g.text(font, Ui.heading(Component.translatable("gui.frosthain.powers.known")), MARGIN + 8, MARGIN + 30, Theme.FRAME_GOLD, false);
        int lockedHeaderY = MARGIN + 42 + known.size() * 19 + 4;
        if (!lockedByTalent.isEmpty()) {
            Component header = Ui.heading(Component.translatable("gui.frosthain.powers.locked"));
            if (font.width(header) > LEFT_W - 8) {
                header = Ui.body(Component.translatable("gui.frosthain.powers.locked"));
            }
            g.text(font, header, MARGIN + 8, lockedHeaderY, Theme.TEXT_SECONDARY, false);
        }
        if (selected != null) {
            registry.get(ContentType.POWERS, selected).ifPresent(power -> detail(g, power));
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void detail(GuiGraphicsExtractor g, PowerDefinition power) {
        int x = MARGIN + LEFT_W + 12;
        int y = MARGIN + 28;
        int w = width - x - MARGIN - 8;
        int h = height - y - MARGIN - 8;
        Ui.panel(g, x, y, w, h, 0xFF111418);
        boolean isKnown = known.contains(power.id());

        // Icon and title row (reference: square icon left, "Fähigkeit: Runenstoß" right).
        g.fill(x + 10, y + 10, x + 38, y + 38, Theme.mix(Theme.RUNE_BLUE, 0xFF101215, 0.6));
        g.outline(x + 10, y + 10, 28, 28, isKnown ? Theme.FRAME_GOLD_BRIGHT : Theme.FRAME_GOLD_DIM);
        g.pose().pushMatrix();
        g.pose().translate(x + 24, y + 24);
        g.pose().scale(1.4f, 1.4f);
        g.fakeItem(TalentIcons.power(power.id().path()), -8, -8);
        g.pose().popMatrix();
        g.text(font, Ui.heading(Component.translatable("gui.frosthain.powers.ability", Component.translatable(nameKey(power.id())))),
                x + 46, y + 12, Theme.FRAME_GOLD_BRIGHT, true);
        int ty = Ui.paragraph(g, font, Component.translatable(nameKey(power.id()) + ".desc"), x + 46, y + 24, w - 56, Theme.TEXT_PRIMARY);

        int sy = Math.max(ty, y + 42) + 6;
        g.horizontalLine(x + 10, x + w - 11, sy, Theme.FRAME_GOLD_DIM);
        sy += 6;
        List<Component> stats = new ArrayList<>();
        power.resource().ifPresent(resource -> stats.add(Component.translatable("ui.frosthain.power.cost", power.cost(),
                Component.translatable("resource." + resource.toTranslationFragment()))));
        if (power.cooldownTicks() > 0) {
            stats.add(Component.translatable("ui.frosthain.power.cooldown", seconds(power.cooldownTicks())));
        }
        power.value("range").ifPresent(v -> stats.add(Component.translatable("ui.frosthain.power.value.range", number(v))));
        power.value("damage_min").ifPresent(min -> {
            double max = power.value("damage_max", min);
            String text = min.equals(max) ? number(min) : number(min) + "–" + number(max);
            boolean magic = power.school().map(s -> !s.equals("martial")).orElse(false);
            stats.add(Component.translatable(magic ? "ui.frosthain.power.value.damage_magic" : "ui.frosthain.power.value.damage", text));
        });
        power.value("knockback").ifPresent(v -> stats.add(Component.translatable("ui.frosthain.power.value.knockback", number(v))));
        power.value("heal_fraction").ifPresent(v -> stats.add(Component.translatable("ui.frosthain.power.value.heal_fraction", number(v * 100))));
        power.value("duration_ticks").ifPresent(v -> stats.add(Component.translatable("ui.frosthain.power.value.duration_ticks", seconds(v.intValue()))));
        power.value("radius").ifPresent(v -> stats.add(Component.translatable("ui.frosthain.power.value.radius", number(v))));
        power.school().ifPresent(s -> stats.add(Component.translatable("ui.frosthain.power.school",
                Component.translatable("ui.frosthain.power.school." + s))));
        int column = 0;
        int columnW = (w - 20) / 2;
        for (Component stat : stats) {
            g.text(font, Ui.body(stat), x + 10 + column * columnW, sy, Theme.TEXT_PRIMARY, false);
            column = 1 - column;
            if (column == 0) {
                sy += font.lineHeight + 3;
            }
        }
        if (column == 1) {
            sy += font.lineHeight + 3;
        }
        sy += 6;
        if (isKnown) {
            int slot = known.indexOf(power.id());
            if (slot < keys.length) {
                g.text(font, Ui.heading(Component.translatable("ui.frosthain.power.key", keys[slot].getTranslatedKeyMessage())),
                        x + 10, sy, Theme.STATE_AVAILABLE, false);
            }
        } else {
            Component talent = lockedByTalent.getOrDefault(power.id(), Component.literal("?"));
            g.text(font, Ui.heading(Component.translatable("gui.frosthain.powers.from_talent", talent)), x + 10, sy, Theme.STATE_BLOCKED, false);
        }
        Ui.paragraph(g, font, Component.translatable("gui.frosthain.powers.effect_pending").withStyle(s -> s.withItalic(true)),
                x + 10, y + h - 2 * font.lineHeight - 8, w - 20, Theme.TEXT_SECONDARY);
    }

    private String seconds(int ticks) {
        return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format(numberLocale(), "%.1f", ticks / 20.0);
    }

    private String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(numberLocale(), "%.1f", value);
    }

    /** Decimal comma for German, point otherwise – follows the game language, not the system. */
    private Locale numberLocale() {
        return minecraft.getLanguageManager().getSelected().startsWith("de") ? Locale.GERMAN : Locale.ROOT;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
