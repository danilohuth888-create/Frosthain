package net.frosthain.core.dialogue;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.content.DialogueDefinition;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.quest.QuestRules;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueSessionTest {

    private static final ContentId AWAKENING = ContentId.of("awakening");

    private final ContentRegistry registry = TestContent.baseRegistry();
    private final DialogueDefinition eldrin = registry.get(ContentType.DIALOGUES, ContentId.parse("eldrin/awakening")).orElseThrow();

    /** Context backed by the real quest rules, like the prototype and the mod. */
    private static final class QuestContext implements DialogueSession.Context {
        private final QuestRules rules;
        QuestLog log;
        int experience;

        QuestContext(ContentRegistry registry, QuestLog log) {
            this.rules = new QuestRules(registry);
            this.log = log;
        }

        @Override
        public DialogueDefinition.QuestState questState(ContentId quest) {
            if (log.isCompleted(quest)) {
                return DialogueDefinition.QuestState.COMPLETED;
            }
            return log.isActive(quest) ? DialogueDefinition.QuestState.ACTIVE : DialogueDefinition.QuestState.NOT_STARTED;
        }

        @Override
        public void apply(DialogueDefinition.Effect effect) {
            switch (effect) {
                case DialogueDefinition.Effect.StartQuest(ContentId quest) -> log = rules.start(log, quest).log();
                case DialogueDefinition.Effect.AdvanceObjective(ContentId quest, String objective, int amount) -> {
                    QuestRules.Outcome outcome = rules.advance(log, quest, objective, amount);
                    log = outcome.log();
                    experience += outcome.experienceGained();
                }
                case DialogueDefinition.Effect.GrantExperience(int amount) -> experience += amount;
            }
        }
    }

    private static List<String> keys(DialogueSession session) {
        return session.visibleChoices().stream().map(DialogueDefinition.Choice::key).toList();
    }

    @Test
    void greetingMatchesReferenceImageChoices() {
        QuestContext context = new QuestContext(registry, new QuestRules(registry).start(QuestLog.EMPTY, AWAKENING).log());
        DialogueSession session = new DialogueSession(eldrin, context);
        assertEquals("dialogue.frosthain.eldrin.awakening.greeting", session.lineKey());
        assertEquals(List.of("what_happened", "signs", "expect", "ready", "goodbye"), keys(session));
    }

    @Test
    void readyIsOnlyOfferedWhileTheQuestIsActive() {
        DialogueSession session = new DialogueSession(eldrin, new QuestContext(registry, QuestLog.EMPTY));
        assertEquals(List.of("what_happened", "signs", "expect", "goodbye"), keys(session));
    }

    @Test
    void choosingReadyProgressesTheQuestAndEndsWithAFinalLine() {
        QuestContext context = new QuestContext(registry, new QuestRules(registry).start(QuestLog.EMPTY, AWAKENING).log());
        DialogueSession session = new DialogueSession(eldrin, context);
        session.choose(keys(session).indexOf("ready"));
        assertEquals("ready_reply", session.currentNode());
        assertEquals(1, context.log.progress(AWAKENING).orElseThrow().count("talk_eldrin"));
        assertTrue(session.isAtEndLine());
        session.acknowledgeEnd();
        assertEquals(DialogueSession.State.FINISHED, session.state());
    }

    @Test
    void followUpQuestionsReturnToTheGreeting() {
        DialogueSession session = new DialogueSession(eldrin, new QuestContext(registry, QuestLog.EMPTY));
        session.choose(0);
        assertEquals("what_happened", session.currentNode());
        assertEquals(List.of("back"), keys(session));
        session.choose(0);
        assertEquals("greeting", session.currentNode());
    }

    @Test
    void goodbyeEndsImmediately() {
        DialogueSession session = new DialogueSession(eldrin, new QuestContext(registry, QuestLog.EMPTY));
        session.choose(keys(session).indexOf("goodbye"));
        assertEquals(DialogueSession.State.FINISHED, session.state());
        assertThrows(IllegalStateException.class, () -> session.choose(0));
    }

    @Test
    void interruptionKeepsTheCurrentLineForResuming() {
        DialogueSession session = new DialogueSession(eldrin, new QuestContext(registry, QuestLog.EMPTY));
        session.choose(1);
        session.interrupt("damage");
        assertEquals(DialogueSession.State.INTERRUPTED, session.state());
        assertEquals(List.of(), session.visibleChoices());
        assertThrows(IllegalStateException.class, () -> session.choose(0));
        session.resume();
        assertEquals("signs", session.currentNode());
        assertEquals(List.of("back"), keys(session));
    }

    @Test
    void invalidChoiceIndexIsRejected() {
        DialogueSession session = new DialogueSession(eldrin, new QuestContext(registry, QuestLog.EMPTY));
        assertThrows(IndexOutOfBoundsException.class, () -> session.choose(9));
    }
}
