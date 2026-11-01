package net.frosthain.core.registry;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.ContentDefinition;
import net.frosthain.core.rules.AbilityScoreRules;
import net.frosthain.core.rules.ProgressionRules;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Immutable snapshot of all loaded content. A new registry is built on every (data pack) reload;
 * game code never mutates it.
 */
public final class ContentRegistry {

    private final Map<ContentType<?>, Map<ContentId, ? extends ContentDefinition>> definitions;
    private final Map<String, Map<String, String>> translations;
    private final Optional<ProgressionRules> progression;
    private final Optional<AbilityScoreRules> abilityScores;

    private ContentRegistry(Builder builder) {
        Map<ContentType<?>, Map<ContentId, ? extends ContentDefinition>> copy = new HashMap<>();
        builder.definitions.forEach((type, map) -> copy.put(type, Collections.unmodifiableMap(new TreeMap<>(map))));
        this.definitions = Map.copyOf(copy);
        Map<String, Map<String, String>> translationCopy = new HashMap<>();
        builder.translations.forEach((locale, map) -> translationCopy.put(locale, Map.copyOf(map)));
        this.translations = Map.copyOf(translationCopy);
        this.progression = Optional.ofNullable(builder.progression);
        this.abilityScores = Optional.ofNullable(builder.abilityScores);
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unchecked")
    public <T extends ContentDefinition> Map<ContentId, T> all(ContentType<T> type) {
        return (Map<ContentId, T>) definitions.getOrDefault(type, Map.of());
    }

    public <T extends ContentDefinition> Optional<T> get(ContentType<T> type, ContentId id) {
        return Optional.ofNullable(all(type).get(id));
    }

    public <T extends ContentDefinition> boolean contains(ContentType<T> type, ContentId id) {
        return all(type).containsKey(id);
    }

    /** Translations per locale (e.g. {@code de_de}). */
    public Map<String, String> translations(String locale) {
        return translations.getOrDefault(locale, Map.of());
    }

    public List<String> locales() {
        return translations.keySet().stream().sorted().toList();
    }

    public Optional<ProgressionRules> progression() {
        return progression;
    }

    public Optional<AbilityScoreRules> abilityScoreRules() {
        return abilityScores;
    }

    public int size() {
        return definitions.values().stream().mapToInt(Map::size).sum();
    }

    public static final class Builder {
        private final Map<ContentType<?>, Map<ContentId, ContentDefinition>> definitions = new HashMap<>();
        private final Map<String, Map<String, String>> translations = new HashMap<>();
        private ProgressionRules progression;
        private AbilityScoreRules abilityScores;

        /** Adds or replaces a definition. Returns the replaced definition, if any (later packs override earlier ones). */
        public <T extends ContentDefinition> Optional<ContentDefinition> put(ContentType<T> type, T definition) {
            return Optional.ofNullable(definitions.computeIfAbsent(type, t -> new HashMap<>()).put(definition.id(), definition));
        }

        public Builder translations(String locale, Map<String, String> entries) {
            translations.computeIfAbsent(locale, l -> new HashMap<>()).putAll(entries);
            return this;
        }

        public Builder progression(ProgressionRules rules) {
            this.progression = rules;
            return this;
        }

        public Builder abilityScores(AbilityScoreRules rules) {
            this.abilityScores = rules;
            return this;
        }

        public ContentRegistry build() {
            return new ContentRegistry(this);
        }
    }
}
