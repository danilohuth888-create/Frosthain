package net.frosthain.core.content;

import com.google.gson.JsonElement;
import net.frosthain.core.ContentId;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A character class (Krieger, Magier, ...). */
public record ClassDefinition(
        ContentId id,
        ContentId source,
        boolean adapted,
        int hitDie,
        List<AbilityScore> primaryAbilities,
        List<AbilityScore> savingThrows,
        List<Map<AbilityScore, Integer>> multiclassRequirements,
        CasterProgression casterProgression,
        Optional<ContentId> resource,
        int subclassLevel,
        List<ContentId> startingPowers,
        SkillChoice skillChoices,
        List<String> armorTraining,
        List<String> weaponProficiencies,
        Optional<ContentId> talentTree,
        List<ContentId> exclusiveWith
) implements ContentDefinition {

    /** How strongly the class contributes to the shared spellcasting level of multiclass characters. */
    public enum CasterProgression {
        NONE(0, 1), THIRD(1, 3), HALF(1, 2), FULL(1, 1);

        private final int numerator;
        private final int denominator;

        CasterProgression(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }

        public int numerator() {
            return numerator;
        }

        public int denominator() {
            return denominator;
        }
    }

    public record SkillChoice(int count, List<ContentId> from) {
    }

    public static final List<String> ARMOR_CATEGORIES = List.of("light", "medium", "heavy", "shields");
    public static final List<String> WEAPON_CATEGORIES = List.of("simple", "martial");
    private static final List<Integer> HIT_DICE = List.of(6, 8, 10, 12);

    public static ClassDefinition parse(ContentId id, JsonObjectReader json) {
        int hitDie = json.integer("hit_die");
        if (!HIT_DICE.contains(hitDie)) {
            throw new ContentParseException(json.childPath("hit_die"), "Trefferwürfel muss einer von " + HIT_DICE + " sein");
        }
        List<AbilityScore> primary = json.list("primary_abilities", (e, p) -> JsonObjectReader.asEnum(e, p, AbilityScore.class));
        if (primary.isEmpty()) {
            throw new ContentParseException(json.childPath("primary_abilities"), "mindestens ein Hauptattribut erwartet");
        }
        List<AbilityScore> saves = json.list("saving_throws", (e, p) -> JsonObjectReader.asEnum(e, p, AbilityScore.class));
        List<Map<AbilityScore, Integer>> multiclass = json.list("multiclass_requirements", ClassDefinition::parseRequirement);
        SkillChoice skills = json.optObject("skill_choices").map(choice -> {
            SkillChoice result = new SkillChoice(choice.integerInRange("count", 0, 0, 6), choice.idList("from"));
            if (result.count() > result.from().size()) {
                throw new ContentParseException(choice.path(), "count ist größer als die Auswahlliste");
            }
            choice.finish();
            return result;
        }).orElse(new SkillChoice(0, List.of()));
        List<String> armor = json.stringList("armor_training");
        checkCategories(json.childPath("armor_training"), armor, ARMOR_CATEGORIES);
        List<String> weapons = json.stringList("weapon_proficiencies");
        checkCategories(json.childPath("weapon_proficiencies"), weapons, WEAPON_CATEGORIES);
        return new ClassDefinition(id, json.id("source"), json.bool("adapted", false), hitDie, primary, saves, multiclass,
                json.enumValue("caster_progression", CasterProgression.class, CasterProgression.NONE),
                json.optId("resource"),
                json.integerInRange("subclass_level", 3, 1, 20),
                json.idList("starting_powers"), skills, armor, weapons,
                json.optId("talent_tree"),
                json.idList("exclusive_with"));
    }

    private static Map<AbilityScore, Integer> parseRequirement(JsonElement element, String path) {
        JsonObjectReader reader = JsonObjectReader.of(element, path);
        Map<AbilityScore, Integer> requirement = new EnumMap<>(AbilityScore.class);
        for (AbilityScore ability : AbilityScore.values()) {
            if (reader.has(ability.key())) {
                requirement.put(ability, reader.integerInRange(ability.key(), 0, 1, 20));
            }
        }
        reader.finish();
        if (requirement.isEmpty()) {
            throw new ContentParseException(path, "leere Voraussetzung");
        }
        return Map.copyOf(requirement);
    }

    private static void checkCategories(String path, List<String> values, List<String> allowed) {
        for (String value : values) {
            if (!allowed.contains(value)) {
                throw new ContentParseException(path, "Unbekannte Kategorie '" + value + "', erlaubt: " + allowed);
            }
        }
    }

    public String translationKey() {
        return "class." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        return List.of(translationKey(), translationKey() + ".desc", translationKey() + ".role");
    }
}
