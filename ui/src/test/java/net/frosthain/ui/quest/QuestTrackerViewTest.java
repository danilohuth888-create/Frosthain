package net.frosthain.ui.quest;

import net.frosthain.core.ContentId;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.quest.QuestRules;
import net.frosthain.ui.UiTestContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestTrackerViewTest {

    private final QuestRules rules = new QuestRules(UiTestContent.registry());

    private QuestLog referenceLog() {
        QuestLog log = rules.start(QuestLog.EMPTY, ContentId.of("awakening")).log();
        return rules.start(log, ContentId.of("lost_supplies")).log();
    }

    private QuestTrackerView.Layout layout(QuestRules rules, QuestLog log, int width, boolean collapsed) {
        return QuestTrackerView.layout(UiTestContent.registry(), rules, log, UiTestContent.german(), UiTestContent.MONO,
                width, collapsed, QuestTrackerView.Style.DEFAULT);
    }

    @Test
    void matchesReferenceImageStructure() {
        QuestTrackerView.Layout layout = layout(rules, referenceLog(), 400, false);
        List<String> texts = layout.lines().stream().map(QuestTrackerView.Line::text).toList();
        assertEquals(List.of("Hauptquest", "Das Erwachen", "Sprich mit Eldrin im Dorf.",
                "Nebenquest", "Verlorene Vorräte", "Finde die verlorenen Kisten im Wald. (0/3)"), texts);
        assertEquals(QuestTrackerView.Marker.DIAMOND_MAIN, layout.lines().get(1).marker());
        assertEquals(QuestTrackerView.Marker.BOX_OPEN, layout.lines().get(2).marker());
    }

    @Test
    void neverExceedsTheMinimapWidth() {
        int width = 200;
        QuestTrackerView.Layout layout = layout(rules, referenceLog(), width, false);
        assertTrue(layout.lines().size() > 6, "lange Zeilen werden umbrochen");
        for (QuestTrackerView.Line line : layout.lines()) {
            assertTrue(line.x() + line.text().length() * 8 <= width - QuestTrackerView.Style.DEFAULT.padding(), line.text());
        }
    }

    @Test
    void showsHowManyActiveQuestsAreNotPinned() {
        QuestRules onePinned = new QuestRules(UiTestContent.registry(), 1);
        QuestLog log = onePinned.start(QuestLog.EMPTY, ContentId.of("awakening")).log();
        log = onePinned.start(log, ContentId.of("lost_supplies")).log();
        List<QuestTrackerView.Line> lines = layout(onePinned, log, 400, false).lines();
        assertEquals("+1 weitere im Journal", lines.getLast().text());
    }

    @Test
    void collapsedShowsOnlyTheHeader() {
        QuestTrackerView.Layout layout = layout(rules, referenceLog(), 400, true);
        assertEquals(List.of("Quests (2)"), layout.lines().stream().map(QuestTrackerView.Line::text).toList());
    }

    @Test
    void progressUpdatesTheShownObjective() {
        QuestLog log = rules.advance(referenceLog(), ContentId.of("awakening"), "talk_eldrin", 1).log();
        List<String> texts = layout(rules, log, 400, false).lines().stream().map(QuestTrackerView.Line::text).toList();
        assertTrue(texts.contains("Finde den alten Schrein nördlich des Dorfes."), texts::toString);
    }
}
