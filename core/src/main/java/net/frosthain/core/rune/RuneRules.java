package net.frosthain.core.rune;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.RuneDefinition;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rules of the rune altar, shared by server (authoritative check) and client (what the altar shows):
 * which runes a player knows and which materials are still missing.
 */
public final class RuneRules {

    public enum Problem { UNKNOWN_RUNE, NOT_KNOWN, MISSING_MATERIALS }

    private final ContentRegistry registry;

    public RuneRules(ContentRegistry registry) {
        this.registry = registry;
    }

    /** All runes, ordered like the altar list: by effect (fire, frost, ward, heal, swiftness), then id. */
    public List<RuneDefinition> all() {
        return registry.all(ContentType.RUNES).values().stream()
                .sorted(Comparator.comparing(RuneDefinition::effect).thenComparing(rune -> rune.id().toString()))
                .toList();
    }

    public boolean isKnown(RuneDefinition rune, QuestLog log) {
        return rune.unlockQuest().map(log::isCompleted).orElse(true);
    }

    public List<RuneDefinition> known(QuestLog log) {
        return all().stream().filter(rune -> isKnown(rune, log)).toList();
    }

    /** Item id → how many more are needed; empty if everything is there. */
    public Map<String, Integer> missing(RuneDefinition rune, Map<String, Integer> inventory) {
        Map<String, Integer> missing = new LinkedHashMap<>();
        for (RuneDefinition.Material material : rune.materials()) {
            int lacking = material.count() - inventory.getOrDefault(material.item(), 0);
            if (lacking > 0) {
                missing.put(material.item(), lacking);
            }
        }
        return missing;
    }

    /** Why the rune cannot be inscribed right now, or empty if it can. */
    public Optional<Problem> check(ContentId runeId, QuestLog log, Map<String, Integer> inventory) {
        Optional<RuneDefinition> rune = registry.get(ContentType.RUNES, runeId);
        if (rune.isEmpty()) {
            return Optional.of(Problem.UNKNOWN_RUNE);
        }
        if (!isKnown(rune.get(), log)) {
            return Optional.of(Problem.NOT_KNOWN);
        }
        if (!missing(rune.get(), inventory).isEmpty()) {
            return Optional.of(Problem.MISSING_MATERIALS);
        }
        return Optional.empty();
    }
}
