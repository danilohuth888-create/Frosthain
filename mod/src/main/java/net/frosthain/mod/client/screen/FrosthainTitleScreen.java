package net.frosthain.mod.client.screen;

import net.fabricmc.loader.api.FabricLoader;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.client.gui.FrosthainButton;
import net.frosthain.mod.client.gui.Ui;
import net.frosthain.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main menu "FROSTHAIN – Das Erwachen" (Design 2.0, view "Hauptmenü"). Extends {@link TitleScreen} so that
 * Minecraft and test tooling still recognise it as the title screen; the vanilla logo, splash and buttons are
 * replaced entirely.
 */
public final class FrosthainTitleScreen extends TitleScreen {

    private static final int BUTTON_WIDTH = 180;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;
    /** Title block (logo + subtitle) above the buttons. */
    private static final int TITLE_BLOCK = 62;
    /** Five buttons; the whole menu must fit above the two footer lines even at 240 px GUI height. */
    private static final int MENU_HEIGHT = TITLE_BLOCK + 5 * (BUTTON_HEIGHT + GAP);

    private final List<FrosthainButton> buttons = new ArrayList<>();
    private String versionLine = "";

    @Override
    protected void init() {
        buttons.clear();
        int x = width / 2 - BUTTON_WIDTH / 2;
        int y = titleY() + TITLE_BLOCK;
        y = add(x, y, Component.translatable("menu.singleplayer"), b -> minecraft.gui.setScreen(new SelectWorldScreen(this)));
        y = add(x, y, Component.translatable("menu.multiplayer"), b -> minecraft.gui.setScreen(new JoinMultiplayerScreen(this)));
        y = add(x, y, Component.translatable("gui.frosthain.menu.character"),
                b -> minecraft.gui.setScreen(new CharacterCreationScreen(this, CharacterCreationScreen.Mode.PREVIEW)));
        y = add(x, y, Component.translatable("menu.options"), b -> minecraft.gui.setScreen(new OptionsScreen(this, minecraft.options)));
        add(x, y, Component.translatable("menu.quit"), b -> minecraft.stop());
        versionLine = Component.translatable("gui.frosthain.version", modVersion(FrosthainMod.MOD_ID), modVersion("minecraft")).getString();
    }

    private int add(int x, int y, Component label, Consumer<FrosthainButton> action) {
        buttons.add(addRenderableWidget(new FrosthainButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, label, FrosthainButton.Style.MENU, action)));
        return y + BUTTON_HEIGHT + GAP;
    }

    private static String modVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        extractPanorama(g, partialTick);
        // The world stays visible; a slate gradient keeps text readable (Design 2.0: "Welt im Mittelpunkt").
        g.fillGradient(0, 0, width, height, 0x3017191D, 0xC017191D);
        g.fillGradient(0, 0, width / 4, height, 0x6017191D, 0x0017191D);
    }

    /** Centers the menu vertically, leaving room for the footer (two text lines). */
    private int titleY() {
        int footer = 2 * font.lineHeight + 10;
        return Math.max(8, (height - footer - MENU_HEIGHT) / 2);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int titleY = titleY();
        Ui.scaledCentered(g, font, Ui.heading(Component.literal("FROSTHAIN")), width / 2, titleY, 3.4f, Theme.FRAME_GOLD_BRIGHT);
        Ui.ornamentTitle(g, font, Component.translatable("gui.frosthain.title.subtitle"), width / 2, titleY + 38, 1.4f);
        for (FrosthainButton button : buttons) {
            button.extractRenderState(g, mouseX, mouseY, partialTick);
        }
        Component version = Ui.body(Component.literal(versionLine));
        Component credits = Component.translatable("title.credits");
        g.text(font, version, width - font.width(version) - 6, height - 2 * font.lineHeight - 6, Theme.TEXT_SECONDARY, true);
        g.text(font, credits, width - font.width(credits) - 6, height - font.lineHeight - 3, Theme.TEXT_DISABLED, true);
    }

    @Override
    public void tick() {
    }
}
