package net.frosthain.mod.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.frosthain.core.ContentId;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.registry.ContentType;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.character.CharacterService;
import net.frosthain.mod.client.ClientCharacter;
import net.frosthain.mod.client.screen.CharacterCreationScreen;
import net.frosthain.mod.client.screen.FrosthainTitleScreen;
import net.frosthain.mod.client.screen.TalentTreeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Real client test: boots Minecraft with FROSTHAIN, checks the main menu, walks through character creation in a
 * new singleplayer world, verifies that the server stored the character and takes screenshots of every view.
 * Runs headless in CI (xvfb + Mesa) and locally with the real graphics card.
 */
public final class FrosthainClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        if (FrosthainMod.content() == null || FrosthainMod.content().all(ContentType.CLASSES).isEmpty()) {
            throw new AssertionError("FROSTHAIN-Inhalte wurden im Client nicht geladen");
        }
        context.waitForScreen(FrosthainTitleScreen.class);
        context.takeScreenshot("frosthain-01-hauptmenue");

        // clickScreenButton only finds vanilla Button instances; FROSTHAIN buttons are AbstractButtons.
        context.runOnClient(client -> client.gui.setScreen(
                new CharacterCreationScreen(client.gui.screen(), CharacterCreationScreen.Mode.PREVIEW)));
        context.waitForScreen(CharacterCreationScreen.class);
        context.takeScreenshot("frosthain-02-vorschau-spezies");
        context.runOnClient(client -> client.gui.screen().onClose());
        context.waitForScreen(FrosthainTitleScreen.class);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.waitForScreen(CharacterCreationScreen.class);
            context.waitTicks(20);
            context.takeScreenshot("frosthain-03-spezies");
            context.runOnClient(client -> {
                CharacterCreationScreen screen = creation(client);
                screen.select(ContentId.of("elf"));
                screen.show(CharacterCreationScreen.Step.CLASS);
                screen.select(ContentId.of("rune_mage"));
            });
            context.takeScreenshot("frosthain-04-klasse");
            context.runOnClient(client -> {
                creation(client).show(CharacterCreationScreen.Step.BACKGROUND);
                creation(client).select(ContentId.of("sage"));
            });
            context.takeScreenshot("frosthain-05-hintergrund");
            context.runOnClient(client -> creation(client).show(CharacterCreationScreen.Step.FINISH));
            context.takeScreenshot("frosthain-06-fertig");

            context.runOnClient(client -> creation(client).submit());
            context.waitFor(client -> ClientCharacter.state().isPresent() && client.gui.screen() == null);
            context.waitTicks(40);
            context.takeScreenshot("frosthain-07-hud");

            singleplayer.getServer().runOnServer(server -> {
                CharacterSheet sheet = CharacterService.sheet(server.getPlayerList().getPlayers().getFirst())
                        .orElseThrow(() -> new AssertionError("Server hat keinen Charakter gespeichert"));
                if (!sheet.classLevels().containsKey(ContentId.of("rune_mage")) || !sheet.species().equals(ContentId.of("elf"))) {
                    throw new AssertionError("Gespeicherter Charakter weicht ab: " + sheet.species() + " " + sheet.classLevels());
                }
            });

            context.setScreen(() -> new CharacterCreationScreen(null, CharacterCreationScreen.Mode.VIEW));
            context.takeScreenshot("frosthain-08-charakter");
            context.setScreen(() -> null);

            // Talent tree (K): 2000 EP = level 4 = 4 points; unlock two talents and check rules and effects on the server.
            singleplayer.getServer().runCommand("execute as @a run frosthain ep 2000");
            context.waitFor(client -> ClientCharacter.sheet().map(s -> s.characterLevel() >= 4).orElse(false));
            context.setScreen(TalentTreeScreen::new);
            context.waitTicks(5);
            context.takeScreenshot("frosthain-09-talentbaum");
            unlock(context, "combat_stance");
            unlock(context, "toughness");
            context.waitTicks(5);
            context.takeScreenshot("frosthain-10-talente-freigeschaltet");
            singleplayer.getServer().runOnServer(server -> {
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                CharacterSheet sheet = CharacterService.sheet(player).orElseThrow();
                if (!sheet.talents().containsKey(new TalentRef(ContentId.of("general"), "combat_stance"))) {
                    throw new AssertionError("Talent wurde nicht gespeichert: " + sheet.talents());
                }
                boolean applied = player.getAttribute(Attributes.ATTACK_DAMAGE).getModifiers().stream()
                        .anyMatch(modifier -> modifier.id().getNamespace().equals(FrosthainMod.MOD_ID));
                if (!applied) {
                    throw new AssertionError("Kampfhaltung wirkt nicht auf den Angriffsschaden");
                }
            });
            context.setScreen(() -> null);
        }
    }

    private static void unlock(ClientGameTestContext context, String node) {
        TalentRef ref = new TalentRef(ContentId.of("general"), node);
        context.runOnClient(client -> {
            if (!(client.gui.screen() instanceof TalentTreeScreen screen)) {
                throw new AssertionError("Talentbaum ist nicht offen: " + client.gui.screen());
            }
            screen.selectNode(node);
            screen.unlockSelected();
        });
        context.waitFor(client -> ClientCharacter.sheet().map(s -> s.talents().containsKey(ref)).orElse(false));
    }

    private static CharacterCreationScreen creation(Minecraft client) {
        if (client.gui.screen() instanceof CharacterCreationScreen screen) {
            return screen;
        }
        throw new AssertionError("Charaktererstellung ist nicht offen: " + client.gui.screen());
    }
}
