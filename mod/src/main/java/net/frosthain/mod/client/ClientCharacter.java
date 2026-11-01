package net.frosthain.mod.client;

import com.google.gson.JsonParser;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.persistence.CharacterSheetCodec;
import net.frosthain.mod.client.screen.CharacterCreationScreen;
import net.frosthain.mod.client.screen.TalentTreeScreen;
import net.frosthain.mod.network.CharacterRejectedPayload;
import net.frosthain.mod.network.CharacterStatePayload;
import net.minecraft.client.Minecraft;

import java.util.Optional;

/** Client copy of the character state sent by the server; opens the editor when the player has none yet. */
public final class ClientCharacter {

    private static CharacterStatePayload state;
    private static CharacterSheet sheet;
    private static boolean creationPending;

    private ClientCharacter() {
    }

    public static Optional<CharacterStatePayload> state() {
        return Optional.ofNullable(state).filter(CharacterStatePayload::present);
    }

    /** The full sheet as stored on the server (talents, levels, experience). */
    public static Optional<CharacterSheet> sheet() {
        return Optional.ofNullable(sheet);
    }

    static void accept(Minecraft client, CharacterStatePayload payload) {
        state = payload;
        sheet = payload.present()
                ? CharacterSheetCodec.decode(JsonParser.parseString(payload.sheetJson()).getAsJsonObject()).sheet()
                : null;
        creationPending = !payload.present();
        if (payload.present() && client.gui.screen() instanceof CharacterCreationScreen screen
                && screen.mode() == CharacterCreationScreen.Mode.IN_WORLD) {
            client.gui.setScreen(null);
        }
        if (client.gui.screen() instanceof TalentTreeScreen screen) {
            screen.refresh();
        }
    }

    static void rejected(Minecraft client, CharacterRejectedPayload payload) {
        if (client.gui.screen() instanceof CharacterCreationScreen screen) {
            screen.rejected(payload.problems());
        } else if (client.gui.screen() instanceof TalentTreeScreen screen) {
            screen.rejected(payload.problems());
        }
    }

    static void openTalentTree(Minecraft client) {
        if (client.player != null && client.gui.screen() == null && sheet != null) {
            client.gui.setScreen(new TalentTreeScreen());
        }
    }

    /** Opens the editor once the world is loaded and no other screen is open (loading screens close themselves). */
    static void tick(Minecraft client) {
        if (creationPending && client.player != null && client.level != null && client.gui.screen() == null) {
            client.gui.setScreen(new CharacterCreationScreen(null, CharacterCreationScreen.Mode.IN_WORLD));
        }
    }

    static void openCharacterScreen(Minecraft client) {
        if (client.player == null || client.gui.screen() != null) {
            return;
        }
        if (state().isPresent()) {
            client.gui.setScreen(new CharacterCreationScreen(null, CharacterCreationScreen.Mode.VIEW));
        } else if (creationPending) {
            client.gui.setScreen(new CharacterCreationScreen(null, CharacterCreationScreen.Mode.IN_WORLD));
        }
    }

    static void clear() {
        state = null;
        sheet = null;
        creationPending = false;
    }
}
