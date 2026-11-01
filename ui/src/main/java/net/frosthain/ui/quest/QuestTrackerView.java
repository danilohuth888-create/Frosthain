package net.frosthain.ui.quest;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.QuestDefinition;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.quest.QuestRules;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.ui.Theme;
import net.frosthain.ui.Translator;
import net.frosthain.ui.text.TextLayout;
import net.frosthain.ui.text.TextMeasurer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Layout of the compact quest tracker (UI spec B): main and side quests in sections, the current objective
 * per pinned quest with progress, wrapped to at most the minimap width, collapsible.
 */
public final class QuestTrackerView {

    public enum LineKind { HEADER, SECTION, QUEST, OBJECTIVE, MORE }

    public enum Marker { NONE, DIAMOND_MAIN, DIAMOND_OTHER, BOX_OPEN, BOX_DONE }

    /** One rendered text line; {@code y} is the top of the line in pixels relative to the tracker. */
    public record Line(LineKind kind, String text, int x, int y, int height, Theme.FontRole role, int size, int color, Marker marker) {
    }

    public record Layout(List<Line> lines, int width, int height, boolean collapsed) {
    }

    /** Pixel sizes; scaled by the renderer's GUI scale. */
    public record Style(int padding, int markerWidth, int sectionSize, int questSize, int objectiveSize, double lineSpacing,
                        int sectionGap) {
        public static final Style DEFAULT = new Style(10, 16, 17, 16, 15, 1.22, 14);
    }

    private QuestTrackerView() {
    }

    public static Layout layout(ContentRegistry registry, QuestRules rules, QuestLog log, Translator translator,
                                TextMeasurer measurer, int maxWidth, boolean collapsed, Style style) {
        List<Line> lines = new ArrayList<>();
        int y = style.padding();
        int contentWidth = maxWidth - 2 * style.padding();
        if (collapsed) {
            String text = translator.t("ui.frosthain.quests.collapsed", log.activeQuests().size());
            lines.add(new Line(LineKind.HEADER, text, style.padding(), y, lineHeight(style.sectionSize(), style),
                    Theme.FontRole.HEADING, style.sectionSize(), Theme.FRAME_GOLD_BRIGHT, Marker.NONE));
            y += lineHeight(style.sectionSize(), style);
            return new Layout(lines, maxWidth, y + style.padding(), true);
        }
        String currentSection = null;
        for (ContentId questId : log.pinned()) {
            QuestDefinition quest = registry.get(ContentType.QUESTS, questId).orElse(null);
            QuestLog.Progress progress = log.progress(questId).orElse(null);
            if (quest == null || progress == null) {
                continue;
            }
            String section = translator.t("ui.frosthain.quests.kind." + quest.kind().name().toLowerCase(Locale.ROOT));
            if (!section.equals(currentSection)) {
                if (currentSection != null) {
                    y += style.sectionGap();
                }
                lines.add(new Line(LineKind.SECTION, section, style.padding(), y, lineHeight(style.sectionSize(), style),
                        Theme.FontRole.HEADING, style.sectionSize(), Theme.TEXT_PRIMARY, Marker.NONE));
                y += lineHeight(style.sectionSize(), style);
                currentSection = section;
            }
            Marker diamond = quest.kind().isMain() ? Marker.DIAMOND_MAIN : Marker.DIAMOND_OTHER;
            List<String> titleLines = TextLayout.wrap(translator.t(quest.translationKey()), contentWidth - style.markerWidth(),
                    measurer, Theme.FontRole.BODY, style.questSize());
            for (int i = 0; i < titleLines.size(); i++) {
                lines.add(new Line(LineKind.QUEST, titleLines.get(i), style.padding() + style.markerWidth(), y,
                        lineHeight(style.questSize(), style), Theme.FontRole.BODY, style.questSize(), Theme.FRAME_GOLD_BRIGHT,
                        i == 0 ? diamond : Marker.NONE));
                y += lineHeight(style.questSize(), style);
            }
            for (QuestDefinition.Objective objective : rules.currentObjectives(quest, progress)) {
                String text = translator.t(objective.translationKey(quest));
                if (objective.count() > 1) {
                    text += " (" + progress.count(objective.key()) + "/" + objective.count() + ")";
                }
                boolean done = progress.count(objective.key()) >= objective.count();
                List<String> objectiveLines = TextLayout.wrap(text, contentWidth - style.markerWidth(), measurer,
                        Theme.FontRole.BODY, style.objectiveSize());
                for (int i = 0; i < objectiveLines.size(); i++) {
                    lines.add(new Line(LineKind.OBJECTIVE, objectiveLines.get(i), style.padding() + style.markerWidth(), y,
                            lineHeight(style.objectiveSize(), style), Theme.FontRole.BODY, style.objectiveSize(),
                            Theme.TEXT_PRIMARY, i == 0 ? (done ? Marker.BOX_DONE : Marker.BOX_OPEN) : Marker.NONE));
                    y += lineHeight(style.objectiveSize(), style);
                }
            }
        }
        int hidden = log.activeQuests().size() - (int) log.pinned().stream().filter(log::isActive).count();
        if (hidden > 0) {
            y += style.padding() / 2;
            lines.add(new Line(LineKind.MORE, translator.t("ui.frosthain.quests.more", hidden), style.padding(), y,
                    lineHeight(style.objectiveSize(), style), Theme.FontRole.BODY_ITALIC, style.objectiveSize(),
                    Theme.TEXT_SECONDARY, Marker.NONE));
            y += lineHeight(style.objectiveSize(), style);
        }
        return new Layout(List.copyOf(lines), maxWidth, y + style.padding(), false);
    }

    private static int lineHeight(int size, Style style) {
        return (int) Math.ceil(size * style.lineSpacing());
    }
}
