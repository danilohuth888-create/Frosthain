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
import net.frosthain.mod.client.screen.FrosthainTitleScreen;
import net.frosthain.mod.network.CharacterRejectedPayload;
import net.frosthain.mod.network.CharacterStatePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;

/** Client entry point: FROSTHAIN main menu, character editor, HUD badge and the "Charakter" key (I). */
public final class FrosthainClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyMapping characterKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.frosthain.character",
                InputConstants.KEY_I, KeyMapping.Category.register(FrosthainMod.id("frosthain"))));

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
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientCharacter.clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientCharacter.tick(client);
            while (characterKey.consumeClick()) {
                ClientCharacter.openCharacterScreen(client);
            }
        });

        HudElementRegistry.addLast(FrosthainMod.id("character"), new CharacterHud());
    }
}
