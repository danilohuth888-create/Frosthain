package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Metadata of an active or passive power (Fähigkeit/Zauber), plus the numbers of its in-game effect
 * ({@code values}: range, damage, knockback, …) so balancing happens in data, not in code.
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
        List<String> tags,
        Map<String, Double> values
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
                school, json.stringList("tags"), parseValues(json));
    }

    /** The optional {@code values} object of powers and runes: only {@link #VALUE_KEYS}, each 0–10000. */
    static Map<String, Double> parseValues(JsonObjectReader json) {
        Map<String, Double> values = new TreeMap<>();
        if (json.has("values")) {
            JsonObjectReader valueJson = json.object("values");
            for (String key : valueJson.keys()) {
                if (!VALUE_KEYS.contains(key)) {
                    throw new ContentParseException(valueJson.childPath(key), "Unbekannter Wert '" + key + "', erlaubt: " + VALUE_KEYS);
                }
                double value = valueJson.number(key);
                if (value < 0 || value > 10_000) {
                    throw new ContentParseException(valueJson.childPath(key), "Wert zwischen 0 und 10000 erwartet");
                }
                values.put(key, value);
            }
            valueJson.finish();
        }
        if (values.getOrDefault("damage_min", 0.0) > values.getOrDefault("damage_max", Double.MAX_VALUE)) {
            throw new ContentParseException(json.childPath("values"), "damage_min ist größer als damage_max");
        }
        return Map.copyOf(values);
    }

    /** Numbers of the in-game effect, shown in the ability details (translation key {@code ui.frosthain.power.value.<key>}). */
    public static final List<String> VALUE_KEYS = List.of("range", "damage_min", "damage_max", "knockback", "heal_fraction",
            "duration_ticks", "amplifier", "radius", "dash");

    public Optional<Double> value(String key) {
        return Optional.ofNullable(values.get(key));
    }

    public double value(String key, double fallback) {
        return values.getOrDefault(key, fallback);
    }

    public String translationKey() {
        return "power." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        return List.of(translationKey(), translationKey() + ".desc");
    }
}
