package net.frosthain.core.quest;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.content.QuestDefinition;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRulesTest {

    private static final ContentId AWAKENING = ContentId.of("awakening");
    private static final ContentId FIRST_RUNE = ContentId.of("first_rune");
    private static final ContentId LOST_SUPPLIES = ContentId.of("lost_supplies");

    private final ContentRegistry registry = TestContent.baseRegistry();
    private final QuestRules rules = new QuestRules(registry);

    /** A pack with many side quests to test pin limits and unlimited active quests. */
    private static ContentRegistry manySideQuests(int count) {
        TestContent.Pack pack = new TestContent.Pack();
        for (int i = 0; i < count; i++) {
            pack.file("quests/side_" + i, """
                    {"source": "frosthain:original", "kind": "side",
                     "objectives": [{"key": "do", "type": "custom", "target": "frosthain:thing"}]}""");
        }
        pack.file("quests/main", """
                {"source": "frosthain:original", "kind": "main",
                 "objectives": [{"key": "do", "type": "custom", "target": "frosthain:thing"}]}""");
        return pack.load().registry();
    }

    @Test
    void startingPinsUntilTheLimitButActiveQuestsAreUnlimited() {
        ContentRegistry many = manySideQuests(8);
        QuestRules limited = new QuestRules(many, 3);
        QuestLog log = QuestLog.EMPTY;
        for (int i = 0; i < 8; i++) {
            log = limited.start(log, ContentId.of("side_" + i)).log();
        }
        assertEquals(8, log.activeQuests().size());
        assertEquals(List.of(ContentId.of("side_0"), ContentId.of("side_1"), ContentId.of("side_2")), log.pinned());
    }

    @Test
    void newMainQuestReplacesAPinnedSideQuestAndIsListedFirst() {
        ContentRegistry many = manySideQuests(3);
        QuestRules limited = new QuestRules(many, 3);
        QuestLog log = QuestLog.EMPTY;
        for (int i = 0; i < 3; i++) {
            log = limited.start(log, ContentId.of("side_" + i)).log();
        }
        log = limited.start(log, ContentId.of("main")).log();
        assertEquals(List.of(ContentId.of("main"), ContentId.of("side_0"), ContentId.of("side_1")), log.pinned());
    }

    @Test
    void sequentialObjectivesProgressInOrder() {
        QuestLog log = rules.start(QuestLog.EMPTY, AWAKENING).log();
        QuestDefinition awakening = registry.get(ContentType.QUESTS, AWAKENING).orElseThrow();
        assertEquals("talk_eldrin", rules.currentObjectives(awakening, log.progress(AWAKENING).orElseThrow()).getFirst().key());

        // The shrine cannot be found before talking to Eldrin.
        QuestRules.Outcome early = rules.advance(log, AWAKENING, "find_shrine", 1);
        assertEquals(log, early.log());
        assertTrue(early.events().isEmpty());

        QuestRules.Outcome talked = rules.advance(log, AWAKENING, "talk_eldrin", 1);
        assertTrue(talked.events().contains(new QuestRules.Event.ObjectiveCompleted(AWAKENING, "talk_eldrin")));
        assertEquals("find_shrine", rules.currentObjectives(awakening, talked.log().progress(AWAKENING).orElseThrow()).getFirst().key());
    }

    @Test
    void completingAQuestUnpinsItGrantsExperienceAndStartsFollowUps() {
        QuestLog log = rules.start(QuestLog.EMPTY, AWAKENING).log();
        log = rules.start(log, LOST_SUPPLIES).log();
        log = rules.advance(log, AWAKENING, "talk_eldrin", 1).log();
        QuestRules.Outcome done = rules.advance(log, AWAKENING, "find_shrine", 1);

        assertTrue(done.log().isCompleted(AWAKENING));
        assertEquals(150, done.experienceGained());
        assertTrue(done.log().isActive(FIRST_RUNE), "Folgequest wird automatisch gestartet");
        assertEquals(List.of(FIRST_RUNE, LOST_SUPPLIES), done.log().pinned());
        assertTrue(done.events().contains(new QuestRules.Event.QuestStarted(FIRST_RUNE)));
    }

    @Test
    void countedObjectivesClampAndReportProgress() {
        QuestLog log = rules.start(QuestLog.EMPTY, LOST_SUPPLIES).log();
        QuestRules.Outcome one = rules.advance(log, LOST_SUPPLIES, "find_crates", 1);
        assertEquals(List.of(new QuestRules.Event.ObjectiveProgressed(LOST_SUPPLIES, "find_crates", 1, 3)), one.events());
        QuestRules.Outcome rest = rules.advance(one.log(), LOST_SUPPLIES, "find_crates", 10);
        assertEquals(3, rest.log().progress(LOST_SUPPLIES).orElseThrow().count("find_crates"));
        assertTrue(rest.log().isCompleted(LOST_SUPPLIES));
        // Further progress on a completed quest changes nothing.
        assertEquals(rest.log(), rules.advance(rest.log(), LOST_SUPPLIES, "find_crates", 1).log());
    }

    @Test
    void startingTwiceIsANoOp() {
        QuestLog log = rules.start(QuestLog.EMPTY, AWAKENING).log();
        QuestRules.Outcome again = rules.start(log, AWAKENING);
        assertEquals(log, again.log());
        assertTrue(again.events().isEmpty());
    }

    @Test
    void pinRespectsLimitAndUnpinWorks() {
        QuestRules single = new QuestRules(registry, 1);
        QuestLog log = single.start(QuestLog.EMPTY, LOST_SUPPLIES).log();
        log = single.start(log, AWAKENING).log();
        assertEquals(List.of(AWAKENING), log.pinned());
        assertFalse(single.pin(log, LOST_SUPPLIES).isPresent());
        QuestLog unpinned = single.unpin(log, AWAKENING);
        assertEquals(List.of(LOST_SUPPLIES), single.pin(unpinned, LOST_SUPPLIES).orElseThrow().pinned());
    }
}
