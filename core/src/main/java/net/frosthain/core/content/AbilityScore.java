package net.frosthain.core.content;

import java.util.Locale;
import java.util.Optional;

/**
 * The six ability scores. They are a fixed part of the rule system (not data-driven),
 * because many rules (multiclassing, saving throws, point buy) refer to exactly these six.
 *
 * <p>Mapping to Minecraft gameplay is documented in docs/06-datenmodell.md.
 */
public enum AbilityScore {
    STRENGTH,
    DEXTERITY,
    CONSTITUTION,
    INTELLIGENCE,
    WISDOM,
    CHARISMA;

    /** JSON key, e.g. {@code strength}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Language key, e.g. {@code ability.frosthain.strength}. */
    public String translationKey() {
        return "ability.frosthain." + key();
    }

    public static Optional<AbilityScore> fromKey(String key) {
        for (AbilityScore ability : values()) {
            if (ability.key().equals(key)) {
                return Optional.of(ability);
            }
        }
        return Optional.empty();
    }

    /** Standard ability modifier: floor((score - 10) / 2). */
    public static int modifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }
}
