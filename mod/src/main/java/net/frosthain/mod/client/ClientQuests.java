package net.frosthain.mod.client;

import com.google.gson.JsonParser;
import net.frosthain.core.persistence.QuestLogCodec;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.mod.client.screen.QuestJournalScreen;
import net.frosthain.mod.network.QuestLogPayload;
import net.frosthain.mod.network.QuestMarkerPayload;
import net.minecraft.client.Minecraft;

/** Client copy of the quest log sent by the server, plus the local tracker setting (collapsed by default). */
public final class ClientQuests {

    private static QuestLog log = QuestLog.EMPTY;
    private static boolean trackerExpanded;
    private static QuestMarkerPayload marker = QuestMarkerPayload.none();

    /** Location of the current objective, if the server knows one (shrine, rune stone). */
    public static QuestMarkerPayload marker() {
        return marker;
    }

    static void acceptMarker(QuestMarkerPayload payload) {
        marker = payload;
    }

    private ClientQuests() {
    }

    public static QuestLog log() {
        return log;
    }

    public static boolean trackerExpanded() {
        return trackerExpanded;
    }

    public static void toggleTracker() {
        trackerExpanded = !trackerExpanded;
    }

    static void accept(Minecraft client, QuestLogPayload payload) {
        log = QuestLogCodec.decode(JsonParser.parseString(payload.logJson()).getAsJsonObject());
        if (client.gui.screen() instanceof QuestJournalScreen screen) {
            screen.refresh();
        }
    }

    static void openJournal(Minecraft client) {
        if (client.player != null && client.gui.screen() == null && ClientCharacter.state().isPresent()) {
            client.gui.setScreen(new QuestJournalScreen());
        }
    }

    static void clear() {
        log = QuestLog.EMPTY;
        marker = QuestMarkerPayload.none();
    }
}
