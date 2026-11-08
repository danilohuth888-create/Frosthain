package net.frosthain.core.persistence;

import com.google.gson.JsonParser;
import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.quest.QuestRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestLogCodecTest {

    @Test
    void roundTripKeepsStatusCountsAndPinnedOrder() {
        QuestRules rules = new QuestRules(TestContent.baseRegistry());
        QuestLog log = rules.start(QuestLog.EMPTY, ContentId.of("awakening")).log();
        log = rules.start(log, ContentId.of("lost_supplies")).log();
        log = rules.advance(log, ContentId.of("lost_supplies"), "find_crates", 2).log();
        log = rules.advance(log, ContentId.of("awakening"), "talk_eldrin", 1).log();
        log = rules.advance(log, ContentId.of("awakening"), "find_shrine", 1).log();

        QuestLog decoded = QuestLogCodec.decode(JsonParser.parseString(QuestLogCodec.encode(log).toString()).getAsJsonObject());

        assertEquals(log, decoded);
        assertEquals(QuestLog.Status.COMPLETED, decoded.progress(ContentId.of("awakening")).orElseThrow().status());
        assertEquals(2, decoded.progress(ContentId.of("lost_supplies")).orElseThrow().count("find_crates"));
        assertEquals(log.pinned(), decoded.pinned());
    }

    @Test
    void emptyLogRoundTrips() {
        assertEquals(QuestLog.EMPTY, QuestLogCodec.decode(QuestLogCodec.encode(QuestLog.EMPTY)));
    }

    @Test
    void unknownStatusIsRejected() {
        String json = "{\"schema_version\":1,\"quests\":{\"frosthain:awakening\":{\"status\":\"lost\",\"counts\":{}}},\"pinned\":[]}";
        assertThrows(ContentParseException.class, () -> QuestLogCodec.decode(JsonParser.parseString(json).getAsJsonObject()));
    }

    @Test
    void pinsOfUnknownQuestsAreDropped() {
        String json = "{\"schema_version\":1,\"quests\":{},\"pinned\":[\"frosthain:awakening\"]}";
        assertEquals(List.of(), QuestLogCodec.decode(JsonParser.parseString(json).getAsJsonObject()).pinned());
    }
}
