package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.List;
import java.util.Optional;

/**
 * Metadata of an active or passive power (Fähigkeit/Zauber).
 *
 * <p>Phase 3 scope: identity, cost, cooldown and categorisation, enough for talent trees, the spell book
 * UI and validation. The executable effect pipeline (damage, projectiles, runes) is added in Phase 4;
 * until then powers have no in-game effect (see docs/07-roadmap.md).
 */
public record PowerDefinition(
        ContentId id,
        ContentId source,
        boolean adapted,
        Kind kind,
        Optional<ContentId> resource,
        int cost,
        int cooldownTicks,
        Optional<String> school,
        List<String> tags
) implements ContentDefinition {

    public enum Kind { ACTIVE, PASSIVE, TOGGLE }

    public static final List<String> SCHOOLS = List.of(
            "abjuration", "conjuration", "divination", "enchantment", "evocation", "illusion", "necromancy",
            "transmutation", "rune", "martial", "nature", "divine");

    public static PowerDefinition parse(ContentId id, JsonObjectReader json) {
        Kind kind = json.enumValue("kind", Kind.class);
        Optional<ContentId> resource = json.optId("resource");
        int cost = json.integerInRange("cost", 0, 0, 1000);
        int cooldown = json.integerInRange("cooldown_ticks", 0, 0, 20 * 60 * 60);
        if (kind == Kind.PASSIVE && (cost > 0 || cooldown > 0 || resource.isPresent())) {
            throw new ContentParseException(json.path(), "passive Fähigkeiten haben keine Kosten/Abklingzeit");
        }
        if (cost > 0 && resource.isEmpty()) {
            throw new ContentParseException(json.childPath("cost"), "Kosten ohne 'resource'");
        }
        Optional<String> school = json.optString("school");
        if (school.isPresent() && !SCHOOLS.contains(school.get())) {
            throw new ContentParseException(json.childPath("school"), "Unbekannte Schule '" + school.get() + "', erlaubt: " + SCHOOLS);
        }
        return new PowerDefinition(id, json.id("source"), json.bool("adapted", false), kind, resource, cost, cooldown,
                school, json.stringList("tags"));
    }

    public String translationKey() {
        return "power." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        return List.of(translationKey(), translationKey() + ".desc");
    }
}
