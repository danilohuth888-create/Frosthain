package net.frosthain.mod.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.content.RuneDefinition;
import net.frosthain.core.rune.RuneRules;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.character.RuneService;
import net.frosthain.mod.client.ClientQuests;
import net.frosthain.mod.client.gui.FrosthainButton;
import net.frosthain.mod.client.gui.Ui;
import net.frosthain.mod.network.InscribeRunePayload;
import net.frosthain.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rune altar (Design 2.0, view "Runenmagie &amp; Runenaltar"): runes on the left, the glowing rune circle of the
 * selected one in the middle, description, materials (have/need) and effect on the right, "Herstellen" below.
 * Knowledge and materials are shown from client data; the server checks them again when inscribing.
 */
public final class RuneAltarScreen extends Screen {

    private static final int MARGIN = 10;
    private static final int LEFT_W = 120;
    private static final int RIGHT_W = 168;

    private final RuneRules rules = new RuneRules(FrosthainMod.content());
    private ContentId selected;

    public RuneAltarScreen() {
        super(Component.translatable("gui.frosthain.runes.title"));
    }

    public static void open(Minecraft minecraft) {
        minecraft.gui.setScreen(new RuneAltarScreen());
    }

    public ContentId selected() {
        return selected;
    }

    public void select(ContentId rune) {
        selected = rune;
        rebuildWidgets();
    }

    private RuneDefinition rune() {
        return rules.all().stream().filter(r -> r.id().equals(selected)).findFirst().orElseThrow();
    }

    private Map<String, Integer> inventory() {
        return minecraft.player == null ? Map.of() : RuneService.inventoryCounts(minecraft.player.getInventory());
    }

    @Override
    protected void init() {
        List<RuneDefinition> all = rules.all();
        if (selected == null && !all.isEmpty()) {
            List<RuneDefinition> known = rules.known(ClientQuests.log());
            selected = (known.isEmpty() ? all : known).getFirst().id();
        }
        int y = MARGIN + 30;
        for (RuneDefinition rune : all) {
            addRenderableWidget(new FrosthainButton(MARGIN + 8, y, LEFT_W - 8, 18, Component.translatable(rune.translationKey()),
                    FrosthainButton.Style.TILE, b -> select(rune.id())).selected(rune.id().equals(selected)));
            y += 22;
        }
        if (selected != null) {
            RuneDefinition rune = rune();
            FrosthainButton inscribe = new FrosthainButton(width - MARGIN - RIGHT_W + 4, height - MARGIN - 28, RIGHT_W - 16, 18,
                    Component.translatable("gui.frosthain.runes.inscribe"), FrosthainButton.Style.PRIMARY,
                    b -> ClientPlayNetworking.send(new InscribeRunePayload(rune.id().toString())));
            inscribe.active = rules.isKnown(rune, ClientQuests.log()) && rules.missing(rune, inventory()).isEmpty();
            addRenderableWidget(inscribe);
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Materials change while the screen is open (inscribing, picking things up): keep the button in sync.
        if (minecraft.player != null && minecraft.player.tickCount % 10 == 0) {
            rebuildWidgets();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, 0x9010121A, 0xD010121A);
        Ui.panel(g, MARGIN, MARGIN, width - 2 * MARGIN, height - 2 * MARGIN, Theme.PANEL_BACKGROUND);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Ui.ornamentTitle(g, font, title, width / 2, MARGIN + 7, 1.3f);
        if (selected != null) {
            RuneDefinition rune = rune();
            boolean known = rules.isKnown(rune, ClientQuests.log());
            altar(g, rune, known);
            details(g, rune, known);
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    /** Middle: dark stone niche with the rune circle, glowing in the rune's colour when known. */
    private void altar(GuiGraphicsExtractor g, RuneDefinition rune, boolean known) {
        int x0 = MARGIN + LEFT_W + 8;
        int x1 = width - MARGIN - RIGHT_W - 8;
        int y0 = MARGIN + 30;
        int y1 = height - MARGIN - 10;
        g.fill(x0, y0, x1, y1, Theme.PANEL_INNER);
        g.outline(x0, y0, x1 - x0, y1 - y0, Theme.FRAME_GOLD_DIM);
        Ui.corners(g, x0, y0, x1 - x0, y1 - y0, Theme.FRAME_GOLD, 5);
        int cx = (x0 + x1) / 2;
        int cy = (y0 + y1) / 2 - 6;
        int r = Math.max(12, Math.min(x1 - x0, y1 - y0) / 2 - 18);
        int color = known ? color(rune.effect()) : Theme.NODE_LOCKED;
        int glow = (color & 0x00FFFFFF) | 0x30000000;
        g.fillGradient(cx - r - 6, cy - r - 6, cx + r + 6, cy + 4, 0x00000000, glow);
        g.fillGradient(cx - r - 6, cy + 4, cx + r + 6, cy + r + 6, glow, 0x00000000);
        ring(g, cx, cy, r, color);
        ring(g, cx, cy, r * 3 / 4, color);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + (rune.effect().ordinal() * Math.PI / 10);
            for (int s = r / 4; s <= r * 3 / 4; s += 2) {
                int px = cx + (int) Math.round(Math.cos(a) * s);
                int py = cy + (int) Math.round(Math.sin(a) * s);
                g.fill(px, py, px + 1, py + 1, color);
            }
            Ui.diamond(g, cx + (int) Math.round(Math.cos(a) * r), cy + (int) Math.round(Math.sin(a) * r), color);
        }
        Ui.diamond(g, cx, cy, known ? Theme.FRAME_GOLD_BRIGHT : Theme.TEXT_DISABLED);
        // Candles at the foot of the niche.
        for (int side : new int[]{-1, 1}) {
            int kx = cx + side * (r + 2);
            g.fill(kx - 1, y1 - 16, kx + 2, y1 - 6, 0xFFE8DCC0);
            g.fill(kx, y1 - 19, kx + 1, y1 - 16, known ? 0xFFFFC34D : 0xFF555555);
        }
        Ui.scaledCentered(g, font, Ui.heading(Component.translatable(rune.translationKey())), cx, y1 - 14, 0.9f,
                known ? Theme.FRAME_GOLD_BRIGHT : Theme.TEXT_DISABLED);
    }

    private static void ring(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        int steps = Math.max(24, r * 5);
        for (int i = 0; i < steps; i++) {
            double a = Math.PI * 2 * i / steps;
            int px = cx + (int) Math.round(Math.cos(a) * r);
            int py = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    /** Right: name, description, materials with have/need, effect values, seals carried. */
    private void details(GuiGraphicsExtractor g, RuneDefinition rune, boolean known) {
        int x = width - MARGIN - RIGHT_W;
        int y = MARGIN + 30;
        int w = RIGHT_W - 8;
        g.fill(x, y, x + w, height - MARGIN - 34, Theme.PANEL_INNER);
        g.outline(x, y, w, height - MARGIN - 34 - y, Theme.FRAME_GOLD_DIM);
        int tx = x + 6;
        int tw = w - 12;
        int ty = y + 6;
        g.text(font, Ui.heading(Component.translatable(rune.translationKey())), tx, ty, Theme.FRAME_GOLD_BRIGHT, false);
        ty += font.lineHeight + 4;
        ty = Ui.paragraph(g, font, Ui.body(Component.translatable(rune.translationKey() + ".desc")), tx, ty, tw, Theme.TEXT_PRIMARY) + 4;
        if (!known) {
            Component quest = rune.unlockQuest().map(q -> (Component) Component.translatable("quest." + q.toTranslationFragment()))
                    .orElse(Component.empty());
            ty = Ui.paragraph(g, font, Ui.body(Component.translatable("gui.frosthain.runes.locked", quest)), tx, ty, tw, Theme.STATE_BLOCKED) + 4;
        }
        g.text(font, Ui.heading(Component.translatable("gui.frosthain.runes.materials")), tx, ty, Theme.FRAME_GOLD, false);
        ty += font.lineHeight + 2;
        Map<String, Integer> inventory = inventory();
        for (RuneDefinition.Material material : rune.materials()) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(material.item())));
            int have = inventory.getOrDefault(material.item(), 0);
            g.pose().pushMatrix();
            g.pose().translate(tx, ty - 1);
            g.pose().scale(0.625f, 0.625f);
            g.fakeItem(stack, 0, 0);
            g.pose().popMatrix();
            Component line = Ui.body(Component.literal(material.count() + "× ").append(stack.getHoverName())
                    .append("  (" + Math.min(have, 99) + "/" + material.count() + ")"));
            g.text(font, line, tx + 13, ty, have >= material.count() ? Theme.STATE_AVAILABLE : Theme.STATE_BLOCKED, false);
            ty += font.lineHeight + 3;
        }
        ty += 3;
        g.text(font, Ui.heading(Component.translatable("gui.frosthain.runes.effect")), tx, ty, Theme.FRAME_GOLD, false);
        ty += font.lineHeight + 2;
        for (Component value : values(rune)) {
            g.text(font, Ui.body(value), tx + 4, ty, Theme.TEXT_SECONDARY, false);
            ty += font.lineHeight + 1;
        }
        ty += 3;
        g.text(font, Ui.body(Component.translatable("gui.frosthain.runes.carried", carried(rune.id()))), tx, ty, Theme.TEXT_SECONDARY, false);
        ty += font.lineHeight + 2;
        Ui.paragraph(g, font, Ui.body(Component.translatable("gui.frosthain.runes.hint")).withStyle(s -> s.withItalic(true)),
                tx, ty, tw, Theme.TEXT_DISABLED);
    }

    private static List<Component> values(RuneDefinition rune) {
        List<Component> lines = new ArrayList<>();
        if (rune.values().containsKey("radius")) {
            lines.add(Component.translatable("ui.frosthain.rune.value.radius", number(rune.value("radius", 0))));
        }
        if (rune.values().containsKey("damage_max")) {
            lines.add(Component.translatable("ui.frosthain.rune.value.damage", number(rune.value("damage_min", 0)), number(rune.value("damage_max", 0))));
        }
        if (rune.values().containsKey("heal_fraction")) {
            lines.add(Component.translatable("ui.frosthain.rune.value.heal", number(rune.value("heal_fraction", 0) * 100)));
        }
        if (rune.values().containsKey("duration_ticks")) {
            lines.add(Component.translatable("ui.frosthain.rune.value.duration", number(rune.value("duration_ticks", 0) / 20)));
        }
        if (rune.values().containsKey("amplifier")) {
            lines.add(Component.translatable("ui.frosthain.rune.value.level", number(rune.value("amplifier", 0) + 1)));
        }
        return lines;
    }

    private static String number(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }

    private int carried(ContentId rune) {
        if (minecraft.player == null) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (RuneService.sealRune(stack).filter(rune::equals).isPresent()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int color(RuneDefinition.Effect effect) {
        return switch (effect) {
            case FIRE_BURST -> 0xFFE8743B;
            case FROST_BURST -> 0xFF9FD8F0;
            case WARD -> Theme.RUNE_BLUE;
            case HEAL -> 0xFF6FCF7A;
            case SWIFTNESS -> 0xFFE6E0C8;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
