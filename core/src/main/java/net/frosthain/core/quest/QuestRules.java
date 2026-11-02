package net.frosthain.core.quest;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.QuestDefinition;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Quest rules: starting, progressing, completing and pinning quests.
 *
 * <p>UI spec B: unlimited active quests, but only a few pinned in the HUD. Main quests are preferred when
 * pinning and are listed first. All methods are pure and return an {@link Outcome} with the new log and the
 * events the UI can announce.
 */
public final class QuestRules {

    public static final int DEFAULT_MAX_PINNED = 3;

    public sealed interface Event {
        record QuestStarted(ContentId quest) implements Event {
        }

        record ObjectiveProgressed(ContentId quest, String objective, int count, int required) implements Event {
        }

        record ObjectiveCompleted(ContentId quest, String objective) implements Event {
        }

        record QuestCompleted(ContentId quest, int experience) implements Event {
        }
    }

    public record Outcome(QuestLog log, List<Event> events) {
        public int experienceGained() {
            return events.stream().mapToInt(e -> e instanceof Event.QuestCompleted c ? c.experience() : 0).sum();
        }
    }

    private final ContentRegistry registry;
    private final int maxPinned;

    public QuestRules(ContentRegistry registry, int maxPinned) {
        if (maxPinned < 1 || maxPinned > 5) {
            throw new IllegalArgumentException("maxPinned muss zwischen 1 und 5 liegen");
        }
        this.registry = registry;
        this.maxPinned = maxPinned;
    }

    public QuestRules(ContentRegistry registry) {
        this(registry, DEFAULT_MAX_PINNED);
    }

    public int maxPinned() {
        return maxPinned;
    }

    private QuestDefinition definition(ContentId quest) {
        return registry.get(ContentType.QUESTS, quest)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannte Quest " + quest));
    }

    /** Starts a quest if it was never started. Starting twice is a no-op (no event). */
    public Outcome start(QuestLog log, ContentId quest) {
        QuestDefinition definition = definition(quest);
        if (log.progress(quest).isPresent()) {
            return new Outcome(log, List.of());
        }
        Map<ContentId, QuestLog.Progress> quests = new LinkedHashMap<>(log.quests());
        quests.put(quest, new QuestLog.Progress(QuestLog.Status.ACTIVE, Map.of()));
        List<ContentId> pinned = new ArrayList<>(log.pinned());
        if (pinned.size() < maxPinned) {
            pinned.add(quest);
        } else if (definition.kind().isMain()) {
            // A new main quest replaces the last pinned side quest, if there is one.
            for (int i = pinned.size() - 1; i >= 0; i--) {
                if (!definition(pinned.get(i)).kind().isMain()) {
                    pinned.remove(i);
                    pinned.add(quest);
                    break;
                }
            }
        }
        return new Outcome(new QuestLog(quests, sortPinned(pinned)), List.of(new Event.QuestStarted(quest)));
    }

    /**
     * Adds progress to an objective of an active quest. For sequential quests only the current objective can
     * progress. Completing the last objective completes the quest, unpins it and starts its follow-ups.
     */
    public Outcome advance(QuestLog log, ContentId quest, String objective, int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("amount muss positiv sein");
        }
        QuestDefinition definition = definition(quest);
        QuestDefinition.Objective target = definition.objective(objective)
                .orElseThrow(() -> new IllegalArgumentException("Quest " + quest + " hat kein Ziel " + objective));
        if (!log.isActive(quest)) {
            return new Outcome(log, List.of());
        }
        QuestLog.Progress progress = log.progress(quest).orElseThrow();
        if (definition.sequential() && currentObjectives(definition, progress).stream().noneMatch(o -> o.key().equals(objective))) {
            return new Outcome(log, List.of());
        }
        int before = progress.count(objective);
        if (before >= target.count()) {
            return new Outcome(log, List.of());
        }
        int after = Math.min(target.count(), before + amount);
        Map<String, Integer> counts = new LinkedHashMap<>(progress.counts());
        counts.put(objective, after);
        List<Event> events = new ArrayList<>();
        events.add(new Event.ObjectiveProgressed(quest, objective, after, target.count()));
        if (after == target.count()) {
            events.add(new Event.ObjectiveCompleted(quest, objective));
        }
        boolean allDone = definition.objectives().stream().allMatch(o -> counts.getOrDefault(o.key(), 0) >= o.count());
        Map<ContentId, QuestLog.Progress> quests = new LinkedHashMap<>(log.quests());
        List<ContentId> pinned = new ArrayList<>(log.pinned());
        quests.put(quest, new QuestLog.Progress(allDone ? QuestLog.Status.COMPLETED : QuestLog.Status.ACTIVE, counts));
        if (allDone) {
            pinned.remove(quest);
            events.add(new Event.QuestCompleted(quest, definition.experience()));
        }
        QuestLog result = new QuestLog(quests, sortPinned(pinned));
        if (allDone) {
            for (ContentId followUp : definition.followUps()) {
                Outcome started = start(result, followUp);
                result = started.log();
                events.addAll(started.events());
            }
        }
        return new Outcome(result, List.copyOf(events));
    }

    /** Objectives the player currently works on: all open ones, or only the first open one if sequential. */
    public List<QuestDefinition.Objective> currentObjectives(QuestDefinition definition, QuestLog.Progress progress) {
        List<QuestDefinition.Objective> open = definition.objectives().stream()
                .filter(o -> progress.count(o.key()) < o.count())
                .toList();
        if (definition.sequential() && !open.isEmpty()) {
            return List.of(open.getFirst());
        }
        return open;
    }

    public Optional<QuestLog> pin(QuestLog log, ContentId quest) {
        if (!log.isActive(quest) || log.pinned().contains(quest) || log.pinned().size() >= maxPinned) {
            return Optional.empty();
        }
        List<ContentId> pinned = new ArrayList<>(log.pinned());
        pinned.add(quest);
        return Optional.of(new QuestLog(log.quests(), sortPinned(pinned)));
    }

    public QuestLog unpin(QuestLog log, ContentId quest) {
        List<ContentId> pinned = new ArrayList<>(log.pinned());
        pinned.remove(quest);
        return new QuestLog(log.quests(), pinned);
    }

    /** Main quests first, otherwise keep the pin order. */
    private List<ContentId> sortPinned(List<ContentId> pinned) {
        List<ContentId> sorted = new ArrayList<>();
        pinned.stream().filter(q -> definition(q).kind().isMain()).forEach(sorted::add);
        pinned.stream().filter(q -> !definition(q).kind().isMain()).forEach(sorted::add);
        return sorted;
    }
}
