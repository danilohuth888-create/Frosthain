package net.frosthain.mod.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.frosthain.core.ContentId;
import net.frosthain.core.character.Appearance;
import net.frosthain.core.character.AppearanceOptions;
import net.frosthain.core.character.CharacterFactory;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.QuickCreation;
import net.frosthain.core.content.AbilityScore;
import net.frosthain.core.content.BackgroundDefinition;
import net.frosthain.core.content.ClassDefinition;
import net.frosthain.core.content.SpeciesDefinition;
import net.frosthain.core.persistence.CharacterSheetCodec;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.client.ClientCharacter;
import net.frosthain.mod.client.gui.FrosthainButton;
import net.frosthain.mod.client.gui.HeadPortrait;
import net.frosthain.mod.client.gui.SwatchButton;
import net.frosthain.mod.client.gui.Ui;
import net.frosthain.mod.network.CreateCharacterPayload;
import net.frosthain.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Character creation (Design 2.0, views "Charaktererstellung – Spezies/Klasse"): step tabs, choice tiles,
 * a detail panel and – in the world – the player model. The server validates the result (CharacterService).
 *
 * <p>Modes: {@link Mode#IN_WORLD} on first entering a world (cannot be skipped), {@link Mode#PREVIEW} from the
 * main menu (browse only), {@link Mode#VIEW} on key I for an existing character (read only).
 */
public final class CharacterCreationScreen extends Screen {

    public enum Mode { PREVIEW, IN_WORLD, VIEW }

    public enum Step {
        SPECIES("species"), APPEARANCE("appearance"), CLASS("class"), BACKGROUND("background"), FINISH("finish");

        private final String key;

        Step(String key) {
            this.key = key;
        }
    }

    private static final int MARGIN = 10;
    private static final int TILE_GAP = 4;
    private static final int ROW = 17;
    private static final int LABEL_W = 58;

    /** One labelled row of the appearance step; {@code value} is shown between the arrow buttons. */
    private record Row(int y, Component label, Component value) {
    }

    private final List<Row> rows = new java.util.ArrayList<>();
    private Appearance appearance;

    private final Screen parent;
    private final Mode mode;
    private final ContentRegistry registry = FrosthainMod.content();
    private Step step = Step.SPECIES;
    private ContentId species = ContentId.of("human");
    private ContentId characterClass = ContentId.of("fighter");
    private ContentId background = ContentId.of("soldier");
    private String name;
    private List<String> problems = List.of();
    private boolean waiting;

    private int contentTop;
    private int contentBottom;
    private int gridX;
    private int gridW;
    private int detailX;
    private int detailW;
    private int previewX;
    private int previewW;

    public CharacterCreationScreen(Screen parent, Mode mode) {
        super(Component.translatable(mode == Mode.VIEW ? "gui.frosthain.view.title" : "gui.frosthain.creation.title"));
        this.parent = parent;
        this.mode = mode;
        this.name = defaultName();
        if (mode == Mode.VIEW) {
            ClientCharacter.state().ifPresent(state -> {
                species = ContentId.parse(state.species());
                characterClass = ContentId.parse(state.characterClass());
                background = ContentId.parse(state.background());
                name = state.name();
            });
            step = Step.FINISH;
        }
        appearance = mode == Mode.VIEW
                ? ClientCharacter.sheet().map(CharacterSheet::appearance).orElseGet(() -> Appearance.defaultFor(speciesDefinition()))
                : Appearance.defaultFor(speciesDefinition());
    }

    private SpeciesDefinition speciesDefinition() {
        return registry.get(ContentType.SPECIES, species).orElseThrow();
    }

    public Appearance appearance() {
        return appearance;
    }

    public void setAppearance(Appearance next) {
        appearance = next;
        rebuildWidgets();
    }

    /** Minecraft names allow digits and underscores, character names do not (CharacterFactory). */
    private static String defaultName() {
        String player = Minecraft.getInstance().getUser().getName().replaceAll("[^\\p{L} '\\-]", "").strip();
        return player.isEmpty() ? "Abenteurer" : player;
    }

    public Mode mode() {
        return mode;
    }

    public Step step() {
        return step;
    }

    public void show(Step next) {
        step = next;
        problems = List.of();
        rebuildWidgets();
    }

    public void select(ContentId id) {
        switch (step) {
            case SPECIES -> {
                if (!id.equals(species)) {
                    species = id;
                    appearance = Appearance.defaultFor(speciesDefinition());
                }
            }
            case CLASS -> characterClass = id;
            case BACKGROUND -> background = id;
            case APPEARANCE, FINISH -> {
            }
        }
        rebuildWidgets();
    }

    public void submit() {
        if (mode != Mode.IN_WORLD || waiting) {
            return;
        }
        waiting = true;
        problems = List.of();
        ClientPlayNetworking.send(new CreateCharacterPayload(name.strip(), species.toString(), characterClass.toString(), background.toString(),
                CharacterSheetCodec.encodeAppearance(appearance).toString()));
        rebuildWidgets();
    }

    public void rejected(List<String> reasons) {
        waiting = false;
        problems = reasons;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        int left = MARGIN;
        int right = width - MARGIN;
        int tabsY = MARGIN + 24;
        contentTop = tabsY + 24;
        contentBottom = height - MARGIN - 28;
        previewW = mode != Mode.PREVIEW && minecraft.player != null ? Math.min(96, width / 6) : 0;
        gridX = left + 8;
        gridW = Math.min(228, (right - left) * 42 / 100);
        detailX = gridX + gridW + 10;
        previewX = right - 8 - previewW;
        detailW = (previewW > 0 ? previewX - 10 : right - 8) - detailX;

        Step[] steps = Step.values();
        int tabW = Math.min(96, (right - left - 24) / steps.length - 4);
        int tabsX = width / 2 - (steps.length * tabW + (steps.length - 1) * 4) / 2;
        for (int i = 0; i < steps.length; i++) {
            Step target = steps[i];
            Component label = Component.literal((i + 1) + "  ").append(Component.translatable("gui.frosthain.creation.step." + target.key));
            FrosthainButton tab = new FrosthainButton(tabsX + i * (tabW + 4), tabsY, tabW, 16, label, FrosthainButton.Style.TAB,
                    b -> show(target)).selected(target == step);
            tab.active = !waiting;
            addRenderableWidget(tab);
        }

        rows.clear();
        switch (step) {
            case SPECIES -> tiles(registry.all(ContentType.SPECIES).keySet(), "species", species);
            case APPEARANCE -> appearanceControls();
            case CLASS -> tiles(registry.all(ContentType.CLASSES).keySet(), "class", characterClass);
            case BACKGROUND -> tiles(registry.all(ContentType.BACKGROUNDS).keySet(), "background", background);
            case FINISH -> {
                if (mode != Mode.VIEW) {
                    EditBox nameBox = new EditBox(font, gridX, contentTop + 14, gridW, 18, Component.translatable("gui.frosthain.creation.name"));
                    nameBox.setMaxLength(CharacterFactory.MAX_NAME_LENGTH);
                    nameBox.setValue(name);
                    nameBox.setResponder(value -> name = value);
                    addRenderableWidget(nameBox);
                }
            }
        }

        int footerY = height - MARGIN - 24;
        boolean firstStep = step == Step.SPECIES;
        if (!(firstStep && mode == Mode.IN_WORLD)) {
            Component back = firstStep || mode == Mode.VIEW && step == Step.FINISH
                    ? Component.translatable(mode == Mode.PREVIEW ? "gui.frosthain.creation.to_menu" : "gui.frosthain.creation.close")
                    : Component.literal("← ").append(Component.translatable("gui.frosthain.creation.back"));
            FrosthainButton backButton = new FrosthainButton(left + 8, footerY, 110, 18, back, FrosthainButton.Style.MENU,
                    b -> {
                        if (firstStep || mode == Mode.VIEW && step == Step.FINISH) {
                            onClose();
                        } else {
                            show(steps[step.ordinal() - 1]);
                        }
                    });
            backButton.active = !waiting;
            addRenderableWidget(backButton);
        }
        if (step != Step.FINISH) {
            addRenderableWidget(new FrosthainButton(right - 8 - 110, footerY, 110, 18,
                    Component.translatable("gui.frosthain.creation.next").append(" →"), FrosthainButton.Style.PRIMARY,
                    b -> show(steps[step.ordinal() + 1])));
        } else if (mode == Mode.IN_WORLD) {
            FrosthainButton start = new FrosthainButton(right - 8 - 150, footerY, 150, 18,
                    Component.translatable(waiting ? "gui.frosthain.creation.waiting" : "gui.frosthain.creation.start"),
                    FrosthainButton.Style.PRIMARY, b -> submit());
            start.active = !waiting;
            addRenderableWidget(start);
        }
    }

    private void tiles(java.util.Collection<ContentId> ids, String prefix, ContentId selected) {
        List<ContentId> sorted = ids.stream()
                .sorted(Comparator.comparing(id -> Component.translatable(key(prefix, id)).getString()))
                .toList();
        int columns = 3;
        int rows = (sorted.size() + columns - 1) / columns;
        int tileW = (gridW - (columns - 1) * TILE_GAP) / columns;
        int tileH = Math.min(24, (contentBottom - contentTop - (rows - 1) * TILE_GAP) / rows);
        for (int i = 0; i < sorted.size(); i++) {
            ContentId id = sorted.get(i);
            int x = gridX + (i % columns) * (tileW + TILE_GAP);
            int y = contentTop + (i / columns) * (tileH + TILE_GAP);
            FrosthainButton tile = new FrosthainButton(x, y, tileW, tileH, Component.translatable(key(prefix, id)),
                    FrosthainButton.Style.TILE, b -> select(id)).selected(id.equals(selected));
            tile.active = mode != Mode.VIEW;
            addRenderableWidget(tile);
        }
    }

    private static String key(String prefix, ContentId id) {
        return prefix + "." + id.toTranslationFragment();
    }

    /** Step "Aussehen" (reference "Charaktererstellung – Aussehen"): build, colours, hair, height, species features. */
    private void appearanceControls() {
        SpeciesDefinition s = speciesDefinition();
        Appearance a = appearance;
        int cx = gridX + LABEL_W;
        int controlsW = gridW - LABEL_W;
        int y = contentTop;

        rows.add(new Row(y, Component.translatable("gui.frosthain.appearance.body"), null));
        int bodyW = Math.min(56, controlsW / 2 - 2);
        for (Appearance.BodyType type : Appearance.BodyType.values()) {
            FrosthainButton button = new FrosthainButton(cx + type.ordinal() * (bodyW + 4), y, bodyW, 13,
                    Component.translatable("gui.frosthain.appearance.body." + type.name().toLowerCase(java.util.Locale.ROOT)),
                    FrosthainButton.Style.TAB, b -> setAppearance(AppearanceOptions.with(a, type, a.heightScale(), a.skinColor(),
                    a.eyeColor(), a.hairStyle(), a.hairColor()))).selected(a.bodyType() == type);
            button.active = mode != Mode.VIEW;
            addRenderableWidget(button);
        }
        y += ROW;
        y = swatches("gui.frosthain.appearance.skin", AppearanceOptions.SKIN_COLORS, a.skinColor(), cx, controlsW, y,
                hex -> AppearanceOptions.with(a, a.bodyType(), a.heightScale(), hex, a.eyeColor(), a.hairStyle(), a.hairColor()));
        y = swatches("gui.frosthain.appearance.eyes", AppearanceOptions.EYE_COLORS, a.eyeColor(), cx, controlsW, y,
                hex -> AppearanceOptions.with(a, a.bodyType(), a.heightScale(), a.skinColor(), hex, a.hairStyle(), a.hairColor()));
        y = cycleRow("gui.frosthain.appearance.hair", Component.translatable("appearance." + a.hairStyle().toTranslationFragment()), cx, controlsW, y,
                dir -> AppearanceOptions.with(a, a.bodyType(), a.heightScale(), a.skinColor(), a.eyeColor(),
                        AppearanceOptions.cycle(AppearanceOptions.HAIR_STYLES, a.hairStyle(), dir), a.hairColor()));
        y = swatches("gui.frosthain.appearance.hair_color", AppearanceOptions.HAIR_COLORS, a.hairColor(), cx, controlsW, y,
                hex -> AppearanceOptions.with(a, a.bodyType(), a.heightScale(), a.skinColor(), a.eyeColor(), a.hairStyle(), hex));
        y = cycleRow("gui.frosthain.appearance.height",
                Component.translatable("gui.frosthain.appearance.height_value", Math.round(a.heightScale() * 100)), cx, controlsW, y,
                dir -> AppearanceOptions.with(a, a.bodyType(), AppearanceOptions.stepHeight(s, a.heightScale(), dir), a.skinColor(),
                        a.eyeColor(), a.hairStyle(), a.hairColor()));
        for (String slot : s.appearance().featureSlots().keySet()) {
            List<java.util.Optional<ContentId>> options = AppearanceOptions.featureOptions(s, slot);
            java.util.Optional<ContentId> current = java.util.Optional.ofNullable(a.features().get(slot));
            Component value = current.map(part -> (Component) Component.translatable("appearance." + part.toTranslationFragment()))
                    .orElse(Component.translatable("gui.frosthain.appearance.none"));
            y = cycleRow("gui.frosthain.appearance.slot." + slot, value, cx, controlsW, y,
                    dir -> AppearanceOptions.withFeature(a, slot, AppearanceOptions.cycle(options, current, dir)));
        }
    }

    private int swatches(String labelKey, List<String> colors, String selected, int x, int width, int y,
                         java.util.function.Function<String, Appearance> change) {
        rows.add(new Row(y, Component.translatable(labelKey), null));
        int size = Math.min(11, width / colors.size() - 2);
        for (int i = 0; i < colors.size(); i++) {
            String hex = colors.get(i);
            SwatchButton swatch = new SwatchButton(x + i * (size + 2), y + 1, size, hex, hex.equalsIgnoreCase(selected),
                    () -> setAppearance(change.apply(hex)));
            swatch.active = mode != Mode.VIEW;
            addRenderableWidget(swatch);
        }
        return y + ROW;
    }

    private int cycleRow(String labelKey, Component value, int x, int width, int y,
                         java.util.function.IntFunction<Appearance> change) {
        rows.add(new Row(y, Component.translatable(labelKey), value));
        FrosthainButton previous = new FrosthainButton(x, y, 14, 13, Component.literal("<"), FrosthainButton.Style.MENU,
                b -> setAppearance(change.apply(-1)));
        FrosthainButton next = new FrosthainButton(x + width - 14, y, 14, 13, Component.literal(">"), FrosthainButton.Style.MENU,
                b -> setAppearance(change.apply(1)));
        previous.active = mode != Mode.VIEW;
        next.active = mode != Mode.VIEW;
        addRenderableWidget(previous);
        addRenderableWidget(next);
        return y + ROW;
    }

    private void appearanceDetails(GuiGraphicsExtractor g) {
        for (Row row : rows) {
            g.text(font, Ui.body(row.label()), gridX, row.y() + 3, Theme.TEXT_SECONDARY, false);
            if (row.value() != null) {
                int cx = gridX + LABEL_W + (gridW - LABEL_W) / 2;
                g.centeredText(font, Ui.body(row.value()), cx, row.y() + 3, Theme.TEXT_PRIMARY);
            }
        }
        int y = detailHeader(g, Component.translatable("gui.frosthain.appearance.title"), null);
        y = Ui.paragraph(g, font, Component.translatable("gui.frosthain.appearance.hint"), detailX, y, detailW, Theme.TEXT_SECONDARY) + 8;
        if (previewW == 0) {
            int pixel = Math.max(4, Math.min(8, (contentBottom - y - 24) / 11));
            HeadPortrait.draw(g, detailX + detailW / 2 - 4 * pixel, y + 2 * pixel, pixel, appearance);
        }
    }

    /** Portrait in the preview column (instead of the player model, which still wears the Minecraft skin). */
    private void portraitPreview(GuiGraphicsExtractor g) {
        Ui.panel(g, previewX, contentTop, previewW, contentBottom - contentTop, 0x8017191D);
        g.centeredText(font, Ui.heading(Component.translatable("gui.frosthain.appearance.preview")), previewX + previewW / 2, contentTop + 5, Theme.FRAME_GOLD);
        int pixel = Math.max(3, Math.min((previewW - 24) / 8, (contentBottom - contentTop - 40) / 11));
        HeadPortrait.draw(g, previewX + previewW / 2 - 4 * pixel, contentTop + 22 + 2 * pixel, pixel, appearance);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) {
            extractPanorama(g, partialTick);
            g.fill(0, 0, width, height, 0x9010121A);
        } else {
            g.fillGradient(0, 0, width, height, 0x7010121A, 0xB010121A);
        }
        Ui.panel(g, MARGIN, MARGIN, width - 2 * MARGIN, height - 2 * MARGIN, Theme.PANEL_BACKGROUND);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        Ui.ornamentTitle(g, font, title, width / 2, MARGIN + 7, 1.3f);
        switch (step) {
            case SPECIES -> speciesDetails(g);
            case APPEARANCE -> appearanceDetails(g);
            case CLASS -> classDetails(g);
            case BACKGROUND -> backgroundDetails(g);
            case FINISH -> finish(g);
        }
        if (previewW > 0 && step == Step.APPEARANCE) {
            portraitPreview(g);
        } else if (previewW > 0 && minecraft.player != null) {
            Ui.panel(g, previewX, contentTop, previewW, contentBottom - contentTop, 0x8017191D);
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, previewX + 2, contentTop + 2, previewX + previewW - 2,
                    contentBottom - 2, Math.max(24, (contentBottom - contentTop) / 3), 0.0625f, mouseX, mouseY, minecraft.player);
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private int detailHeader(GuiGraphicsExtractor g, Component heading, Component subtitle) {
        int y = contentTop;
        Ui.scaledCentered(g, font, Ui.heading(heading), detailX + detailW / 2, y, 1.25f, Theme.FRAME_GOLD_BRIGHT);
        y += 15;
        if (subtitle != null) {
            g.centeredText(font, Ui.body(subtitle).withStyle(s -> s.withItalic(true)), detailX + detailW / 2, y, Theme.TEXT_SECONDARY);
            y += font.lineHeight + 2;
        }
        g.horizontalLine(detailX + 8, detailX + detailW - 9, y + 2, Theme.FRAME_GOLD_DIM);
        return y + 8;
    }

    private int section(GuiGraphicsExtractor g, String key, int y) {
        g.text(font, Ui.heading(Component.translatable(key)), detailX, y, Theme.FRAME_GOLD, false);
        return y + font.lineHeight + 3;
    }

    private void speciesDetails(GuiGraphicsExtractor g) {
        SpeciesDefinition s = registry.get(ContentType.SPECIES, species).orElseThrow();
        int y = detailHeader(g, Component.translatable(key("species", species)), null);
        y = Ui.paragraph(g, font, Component.translatable(key("species", species) + ".desc"), detailX, y, detailW, Theme.TEXT_PRIMARY) + 6;
        y = section(g, "gui.frosthain.creation.traits", y);
        y = Ui.bullet(g, font, Component.translatable("gui.frosthain.creation.height",
                Math.round(s.appearance().heightScaleMin() * 100), Math.round(s.appearance().heightScaleMax() * 100)), detailX, y, detailW);
        if (s.lineageRequired() && !s.lineages().isEmpty()) {
            Ui.bullet(g, font, Component.translatable("gui.frosthain.creation.lineage",
                    Component.translatable(s.lineages().getFirst().translationKey(s))), detailX, y, detailW);
        }
    }

    private void classDetails(GuiGraphicsExtractor g) {
        ClassDefinition c = registry.get(ContentType.CLASSES, characterClass).orElseThrow();
        String k = key("class", characterClass);
        int y = detailHeader(g, Component.translatable(k), Component.translatable(k + ".role"));
        y = Ui.paragraph(g, font, Component.translatable(k + ".desc"), detailX, y, detailW, Theme.TEXT_PRIMARY) + 6;
        y = section(g, "gui.frosthain.creation.primary", y);
        y = Ui.bullet(g, font, abilityList(c.primaryAbilities()), detailX, y, detailW);
        Ui.bullet(g, font, Component.translatable(c.talentTree().isPresent()
                ? "gui.frosthain.creation.talent_tree.yes" : "gui.frosthain.creation.talent_tree.no"), detailX, y, detailW);
    }

    private void backgroundDetails(GuiGraphicsExtractor g) {
        BackgroundDefinition b = registry.get(ContentType.BACKGROUNDS, background).orElseThrow();
        ClassDefinition c = registry.get(ContentType.CLASSES, characterClass).orElseThrow();
        String k = key("background", background);
        int y = detailHeader(g, Component.translatable(k), null);
        y = Ui.paragraph(g, font, Component.translatable(k + ".desc"), detailX, y, detailW, Theme.TEXT_PRIMARY) + 6;
        y = section(g, "gui.frosthain.creation.bonus", y);
        for (Map.Entry<AbilityScore, Integer> bonus : QuickCreation.backgroundBonus(b.abilityOptions(), c.primaryAbilities()).entrySet()) {
            y = Ui.bullet(g, font, Component.translatable(bonus.getKey().translationKey()).append(" +" + bonus.getValue()), detailX, y, detailW);
        }
    }

    private void finish(GuiGraphicsExtractor g) {
        int y = contentTop;
        g.text(font, Ui.heading(Component.translatable("gui.frosthain.creation.name")), gridX, y, Theme.FRAME_GOLD, false);
        y += 14;
        if (mode == Mode.VIEW) {
            g.text(font, Ui.heading(Component.literal(name)), gridX, y + 4, Theme.FRAME_GOLD_BRIGHT, false);
        }
        y += 26;
        y = summaryLine(g, "gui.frosthain.creation.step.species", Component.translatable(key("species", species)), y);
        y = summaryLine(g, "gui.frosthain.creation.step.class", Component.translatable(key("class", characterClass)), y);
        y = summaryLine(g, "gui.frosthain.creation.step.background", Component.translatable(key("background", background)), y);
        y += 6;
        if (mode == Mode.PREVIEW) {
            Ui.paragraph(g, font, Component.translatable("gui.frosthain.creation.preview_hint"), gridX, y, gridW, Theme.TEXT_SECONDARY);
        }
        for (String problem : problems) {
            y = Ui.paragraph(g, font, Component.literal(problem), gridX, y, gridW, Theme.STATE_BLOCKED);
        }

        int dy = detailHeader(g, Component.translatable("gui.frosthain.creation.attributes"), null);
        ClassDefinition c = registry.get(ContentType.CLASSES, characterClass).orElseThrow();
        BackgroundDefinition b = registry.get(ContentType.BACKGROUNDS, background).orElseThrow();
        Map<AbilityScore, Integer> base = QuickCreation.baseScores(registry.abilityScoreRules().orElseThrow().standardArray(), c.primaryAbilities());
        Map<AbilityScore, Integer> bonus = QuickCreation.backgroundBonus(b.abilityOptions(), c.primaryAbilities());
        for (AbilityScore ability : AbilityScore.values()) {
            int extra = bonus.getOrDefault(ability, 0);
            int value = base.get(ability) + extra;
            int modifier = AbilityScore.modifier(value);
            g.text(font, Ui.body(Component.translatable(ability.translationKey())), detailX, dy, Theme.TEXT_PRIMARY, false);
            String numbers = value + "  (" + (modifier >= 0 ? "+" : "") + modifier + ")";
            g.text(font, Ui.body(Component.literal(numbers)), detailX + detailW - font.width(numbers) - 4, dy,
                    extra > 0 ? Theme.STATE_AVAILABLE : Theme.TEXT_SECONDARY, false);
            dy += font.lineHeight + 3;
        }
    }

    private int summaryLine(GuiGraphicsExtractor g, String labelKey, Component value, int y) {
        g.text(font, Ui.body(Component.translatable(labelKey)), gridX, y, Theme.TEXT_SECONDARY, false);
        g.text(font, Ui.heading(value), gridX + 80, y, Theme.TEXT_PRIMARY, false);
        return y + font.lineHeight + 4;
    }

    private static Component abilityList(List<AbilityScore> abilities) {
        return Component.literal(abilities.stream()
                .map(a -> Component.translatable(a.translationKey()).getString())
                .collect(Collectors.joining(", ")));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return mode != Mode.IN_WORLD;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
