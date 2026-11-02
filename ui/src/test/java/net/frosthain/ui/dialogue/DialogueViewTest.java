package net.frosthain.ui.dialogue;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.DialogueDefinition;
import net.frosthain.core.dialogue.DialogueSession;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.quest.QuestRules;
import net.frosthain.core.registry.ContentType;
import net.frosthain.ui.UiTestContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueViewTest {

    private static final ContentId AWAKENING = ContentId.of("awakening");

    private final QuestRules rules = new QuestRules(UiTestContent.registry());
    private QuestLog log = rules.start(QuestLog.EMPTY, AWAKENING).log();

    private DialogueView open() {
        DialogueDefinition eldrin = UiTestContent.registry().get(ContentType.DIALOGUES, ContentId.parse("eldrin/awakening")).orElseThrow();
        DialogueSession session = new DialogueSession(eldrin, new DialogueSession.Context() {
            @Override
            public DialogueDefinition.QuestState questState(ContentId quest) {
                return log.isCompleted(quest) ? DialogueDefinition.QuestState.COMPLETED
                        : log.isActive(quest) ? DialogueDefinition.QuestState.ACTIVE : DialogueDefinition.QuestState.NOT_STARTED;
            }

            @Override
            public void apply(DialogueDefinition.Effect effect) {
                if (effect instanceof DialogueDefinition.Effect.AdvanceObjective(ContentId quest, String objective, int amount)) {
                    log = rules.advance(log, quest, objective, amount).log();
                }
            }
        });
        return new DialogueView(UiTestContent.registry(), session, UiTestContent.german());
    }

    private static List<String> texts(DialogueView view) {
        return view.choices().stream().map(DialogueView.ChoiceView::text).toList();
    }

    @Test
    void showsReferenceImageDialogue() {
        DialogueView view = open();
        assertEquals("Eldrin", view.speakerName());
        assertEquals("Endlich bist du da. Die Zeichen haben sich verdichtet … Frosthain erwacht erneut. "
                + "Es ist Zeit, dass du deinen Weg gehst. Bist du bereit?", view.line());
        assertEquals(List.of("Was ist im Tal geschehen?", "Erzähle mir von den Zeichen.", "Was erwartest du von mir?",
                "Ich bin bereit.", "Später vielleicht. Leb wohl."), texts(view));
        assertTrue(view.choices().getFirst().selected());
    }

    @Test
    void keyboardSelectionWrapsAround() {
        DialogueView view = open();
        view.moveSelection(-1);
        assertEquals(4, view.selected());
        view.moveSelection(1);
        assertEquals(0, view.selected());
    }

    @Test
    void readyAdvancesTheQuestAndEndsWithContinue() {
        DialogueView view = open();
        assertTrue(view.chooseNumber(4));
        assertEquals(1, log.progress(AWAKENING).orElseThrow().count("talk_eldrin"));
        assertEquals(List.of("Weiter"), texts(view));
        assertFalse(view.confirm(), "Weiter schließt den Dialog");
    }

    @Test
    void damageInterruptsWithAMessage() {
        DialogueView view = open();
        view.onPlayerDamaged();
        assertTrue(view.isInterrupted());
        assertEquals("Du wirst angegriffen! Das Gespräch wurde unterbrochen.", view.line());
        assertEquals(List.of(), view.choices());
    }
}
