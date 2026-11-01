package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.HashSet;
import java.util.List;

/**
 * A background (Hintergrund). Following the SRD 5.2.1 structure, the background determines which
 * three ability scores can receive the starting bonus, plus two skill proficiencies.
 */
public record BackgroundDefinition(
        ContentId id,
        ContentId source,
        boolean adapted,
        List<AbilityScore> abilityOptions,
        List<ContentId> skillProficiencies,
        List<String> startingItems
) implements ContentDefinition {

    public static BackgroundDefinition parse(ContentId id, JsonObjectReader json) {
        List<AbilityScore> abilities = json.list("ability_options", (e, p) -> JsonObjectReader.asEnum(e, p, AbilityScore.class));
        if (abilities.size() != 3 || new HashSet<>(abilities).size() != 3) {
            throw new ContentParseException(json.childPath("ability_options"), "genau drei verschiedene Attribute erwartet");
        }
        List<ContentId> skills = json.idList("skill_proficiencies");
        if (skills.size() != 2) {
            throw new ContentParseException(json.childPath("skill_proficiencies"), "genau zwei Fertigkeiten erwartet");
        }
        List<String> items = json.stringList("starting_items");
        for (String item : items) {
            if (!Modifier.MINECRAFT_ID.matcher(item).matches()) {
                throw new ContentParseException(json.childPath("starting_items"), "Item-ID 'namespace:pfad' erwartet: " + item);
            }
        }
        return new BackgroundDefinition(id, json.id("source"), json.bool("adapted", false), abilities, skills, items);
    }

    public String translationKey() {
        return "background." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        return List.of(translationKey(), translationKey() + ".desc");
    }
}
