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
import net.frosthain.mod.client.hud.CharacterHud;
import net.frosthain.mod.client.hud.QuestTrackerHud;
import net.frosthain.mod.client.screen.DialogueScreen;
import net.frosthain.mod.client.screen.FrosthainTitleScreen;
import net.frosthain.mod.network.CharacterRejectedPayload;
import net.frosthain.mod.network.CharacterStatePayload;
import net.frosthain.mod.network.DialogueStatePayload;
import net.frosthain.mod.network.QuestLogPayload;
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
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientCharacter.clear();
            ClientQuests.clear();
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
        });

        HudElementRegistry.addLast(FrosthainMod.id("character"), new CharacterHud());
        HudElementRegistry.addLast(FrosthainMod.id("quest_tracker"), new QuestTrackerHud());
    }
}
