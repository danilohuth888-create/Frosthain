package net.frosthain.core.quest;

import net.frosthain.core.ContentId;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Quest state of one character. Immutable; {@link QuestRules} produces new logs.
 *
 * @param quests progress per started quest, in the order the quests were started
 * @param pinned quests shown in the HUD tracker, in display order (a subset of the active quests)
 */
public record QuestLog(Map<ContentId, Progress> quests, List<ContentId> pinned) {

    public enum Status { ACTIVE, COMPLETED }

    /** Progress of one quest: status and the current count per objective key. */
    public record Progress(Status status, Map<String, Integer> counts) {
        public Progress {
            counts = Collections.unmodifiableMap(new TreeMap<>(counts));
        }

        public int count(String objective) {
            return counts.getOrDefault(objective, 0);
        }
    }

    public static final QuestLog EMPTY = new QuestLog(Map.of(), List.of());

    public QuestLog {
        quests = Collections.unmodifiableMap(new LinkedHashMap<>(quests));
        pinned = List.copyOf(pinned);
    }

    public Optional<Progress> progress(ContentId quest) {
        return Optional.ofNullable(quests.get(quest));
    }

    public boolean isActive(ContentId quest) {
        return progress(quest).map(p -> p.status() == Status.ACTIVE).orElse(false);
    }

    public boolean isCompleted(ContentId quest) {
        return progress(quest).map(p -> p.status() == Status.COMPLETED).orElse(false);
    }

    public List<ContentId> activeQuests() {
        return quests.entrySet().stream().filter(e -> e.getValue().status() == Status.ACTIVE).map(Map.Entry::getKey).toList();
    }
}
