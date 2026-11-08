package net.frosthain.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.frosthain.mod.FrosthainMod;
import net.frosthain.mod.client.hud.AbilityBarHud;
import net.frosthain.mod.client.hud.CharacterHud;
import net.frosthain.mod.client.hud.QuestTrackerHud;
import net.frosthain.mod.client.screen.DialogueScreen;
import net.frosthain.mod.client.screen.RuneAltarScreen;
import net.frosthain.mod.client.screen.FrosthainTitleScreen;
import net.frosthain.mod.client.screen.PowerDetailsScreen;
import net.frosthain.mod.network.CharacterRejectedPayload;
import net.frosthain.mod.network.CharacterStatePayload;
import net.frosthain.mod.network.DialogueStatePayload;
import net.frosthain.mod.network.RuneAltarPayload;
import net.frosthain.mod.network.PowerStatePayload;
import net.frosthain.mod.network.QuestLogPayload;
import net.frosthain.mod.network.QuestMarkerPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Client entry point: FROSTHAIN main menu, character editor, HUD (badge, quest tracker) and the keys
 * I (character), K (talents), J (quest journal), U (expand/collapse tracker).
 */
public final class FrosthainClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(FrosthainMod.id("frosthain"));
        KeyMapping characterKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.frosthain.character", InputConstants.KEY_I, category));
        KeyMapping talentKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.frosthain.talents", InputConstants.KEY_K, category));
        KeyMapping journalKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.frosthain.journal", InputConstants.KEY_J, category));
        KeyMapping trackerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.frosthain.tracker", InputConstants.KEY_U, category));
        // Ability bar: 1–9 belong to the hotbar, so the defaults are free letters (changeable in the controls).
        int[] powerKeys = {InputConstants.KEY_R, InputConstants.KEY_G, InputConstants.KEY_V, InputConstants.KEY_Z, InputConstants.KEY_B, InputConstants.KEY_N};
        KeyMapping[] powerMappings = new KeyMapping[powerKeys.length];
        for (int i = 0; i < powerKeys.length; i++) {
            powerMappings[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.frosthain.power_" + (i + 1), powerKeys[i], category));
        }
        KeyMapping powersKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.frosthain.powers", InputConstants.KEY_Y, category));

        // Every way back to the title screen creates a vanilla TitleScreen; replace exactly that class.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen.getClass() == TitleScreen.class) {
                client.gui.setScreen(new FrosthainTitleScreen());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(CharacterStatePayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientCharacter.accept(context.client(), payload)));
        ClientPlayNetworking.registerGlobalReceiver(CharacterRejectedPayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientCharacter.rejected(context.client(), payload)));
        ClientPlayNetworking.registerGlobalReceiver(QuestLogPayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientQuests.accept(context.client(), payload)));
        ClientPlayNetworking.registerGlobalReceiver(DialogueStatePayload.TYPE,
                (payload, context) -> context.client().execute(() -> DialogueScreen.accept(context.client(), payload)));
        ClientPlayNetworking.registerGlobalReceiver(RuneAltarPayload.TYPE,
                (payload, context) -> context.client().execute(() -> RuneAltarScreen.open(context.client())));
        ClientPlayNetworking.registerGlobalReceiver(PowerStatePayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientPowers.accept(payload)));
        ClientPlayNetworking.registerGlobalReceiver(QuestMarkerPayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientQuests.acceptMarker(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientCharacter.clear();
            ClientQuests.clear();
            ClientPowers.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientCharacter.tick(client);
            while (characterKey.consumeClick()) {
                ClientCharacter.openCharacterScreen(client);
            }
            while (talentKey.consumeClick()) {
                ClientCharacter.openTalentTree(client);
            }
            while (journalKey.consumeClick()) {
                ClientQuests.openJournal(client);
            }
            while (trackerKey.consumeClick()) {
                ClientQuests.toggleTracker();
            }
            for (int i = 0; i < powerMappings.length; i++) {
                while (powerMappings[i].consumeClick()) {
                    ClientPowers.use(client, i);
                }
            }
            while (powersKey.consumeClick()) {
                if (client.player != null && client.gui.screen() == null && ClientPowers.state().isPresent()) {
                    client.gui.setScreen(new PowerDetailsScreen(powerMappings));
                }
            }
        });

        HudElementRegistry.addLast(FrosthainMod.id("character"), new CharacterHud());
        HudElementRegistry.addLast(FrosthainMod.id("quest_tracker"), new QuestTrackerHud());
        HudElementRegistry.addLast(FrosthainMod.id("ability_bar"), new AbilityBarHud(powerMappings));
    }
}
