package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A data-driven effect granted by species traits, lineages and talent nodes.
 *
 * <p>The set of modifier types is closed on purpose: every type needs a concrete implementation in the
 * mod layer, so new types are added together with code, while new <em>content</em> using existing types
 * needs no code at all. Numeric modifiers on talent nodes scale with the node rank.
 */
public sealed interface Modifier {

    String type();

    /** Minecraft attribute modifier, e.g. {@code minecraft:max_health}. Validated against the registry in the mod. */
    record AttributeModifier(String attribute, Operation operation, double amount) implements Modifier {
        public enum Operation { ADD_VALUE, ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL }

        @Override
        public String type() {
            return "attribute";
        }
    }

    /** Bonus to one of the six ability scores. */
    record AbilityScoreBonus(AbilityScore ability, int amount) implements Modifier {
        @Override
        public String type() {
            return "ability_score";
        }
    }

    /** Grants an active or passive power. */
    record GrantPower(ContentId power) implements Modifier {
        @Override
        public String type() {
            return "grant_power";
        }
    }

    /** Grants proficiency in a skill. */
    record SkillProficiency(ContentId skill) implements Modifier {
        @Override
        public String type() {
            return "skill_proficiency";
        }
    }

    /**
     * Damage reduction against a damage type or damage type tag ({@code #minecraft:is_fire}).
     * {@code amount} is a fraction in (0, 1].
     */
    record Resistance(String damageType, double amount) implements Modifier {
        @Override
        public String type() {
            return "resistance";
        }
    }

    /** Switches on an engine feature implemented in code, e.g. {@code frosthain:darkvision}. See {@link EngineFlags}. */
    record Flag(ContentId flag) implements Modifier {
        @Override
        public String type() {
            return "flag";
        }
    }

    Set<String> TYPES = Set.of("attribute", "ability_score", "grant_power", "skill_proficiency", "resistance", "flag");

    Pattern MINECRAFT_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    static Modifier parse(JsonObjectReader json) {
        String type = json.string("type");
        return switch (type) {
            case "attribute" -> {
                String attribute = json.string("attribute");
                if (!MINECRAFT_ID.matcher(attribute).matches()) {
                    throw new ContentParseException(json.childPath("attribute"), "Attribut-ID 'namespace:pfad' erwartet");
                }
                yield new AttributeModifier(attribute,
                        json.enumValue("operation", AttributeModifier.Operation.class),
                        json.number("amount"));
            }
            case "ability_score" -> {
                int amount = json.integerInRange("amount", 1, -4, 4);
                yield new AbilityScoreBonus(json.enumValue("ability", AbilityScore.class), amount);
            }
            case "grant_power" -> new GrantPower(json.id("power"));
            case "skill_proficiency" -> new SkillProficiency(json.id("skill"));
            case "resistance" -> {
                String damageType = json.string("damage_type");
                String bare = damageType.startsWith("#") ? damageType.substring(1) : damageType;
                if (!MINECRAFT_ID.matcher(bare).matches()) {
                    throw new ContentParseException(json.childPath("damage_type"),
                            "Schadensart 'namespace:pfad' oder Tag '#namespace:pfad' erwartet");
                }
                double amount = json.number("amount");
                if (amount <= 0 || amount > 1) {
                    throw new ContentParseException(json.childPath("amount"), "Anteil in (0, 1] erwartet");
                }
                yield new Resistance(damageType, amount);
            }
            case "flag" -> new Flag(json.id("flag"));
            default -> throw new ContentParseException(json.childPath("type"),
                    "Unbekannter Modifikator-Typ '" + type + "', erlaubt: " + TYPES.stream().sorted().toList());
        };
    }

    static String describe(Modifier modifier) {
        return switch (modifier) {
            case AttributeModifier m -> m.attribute() + " " + m.operation().name().toLowerCase(Locale.ROOT) + " " + m.amount();
            case AbilityScoreBonus m -> m.ability().key() + " " + (m.amount() >= 0 ? "+" : "") + m.amount();
            case GrantPower m -> "power " + m.power();
            case SkillProficiency m -> "skill " + m.skill();
            case Resistance m -> "resistance " + m.damageType() + " " + m.amount();
            case Flag m -> "flag " + m.flag();
        };
    }
}
