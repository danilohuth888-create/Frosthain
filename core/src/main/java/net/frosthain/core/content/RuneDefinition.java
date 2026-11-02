package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A rune inscribed at a rune altar (Runenmagie, M4.2 first part): the materials are consumed and the player
 * receives a rune seal; using the seal releases the {@link Effect}, sized by {@code values} like a power.
 * Runes become known when {@code unlock_quest} is completed (no quest = known from the start).
 */
public record RuneDefinition(
        ContentId id,
        ContentId source,
        boolean adapted,
        Effect effect,
        List<Material> materials,
        Optional<ContentId> unlockQuest,
        Map<String, Double> values
) implements ContentDefinition {

    /** What a seal does when used; the mod implements each one. */
    public enum Effect { FIRE_BURST, FROST_BURST, WARD, HEAL, SWIFTNESS }

    /** @param item Minecraft item id, e.g. {@code minecraft:amethyst_shard} */
    public record Material(String item, int count) {
    }

    public static RuneDefinition parse(ContentId id, JsonObjectReader json) {
        Effect effect = json.enumValue("effect", Effect.class);
        List<Material> materials = json.objectList("materials", material -> {
            String item = material.string("item");
            if (!Modifier.MINECRAFT_ID.matcher(item).matches()) {
                throw new ContentParseException(material.childPath("item"), "ID 'namespace:pfad' erwartet");
            }
            return new Material(item, material.integerInRange("count", 1, 1, 64));
        });
        if (materials.isEmpty()) {
            throw new ContentParseException(json.childPath("materials"), "mindestens ein Material erwartet");
        }
        Set<String> items = new HashSet<>();
        for (Material material : materials) {
            if (!items.add(material.item())) {
                throw new ContentParseException(json.childPath("materials"), "Material '" + material.item() + "' doppelt");
            }
        }
        return new RuneDefinition(id, json.id("source"), json.bool("adapted", false), effect, materials,
                json.optId("unlock_quest"), PowerDefinition.parseValues(json));
    }

    public double value(String key, double fallback) {
        return values.getOrDefault(key, fallback);
    }

    public String translationKey() {
        return "rune." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        return List.of(translationKey(), translationKey() + ".desc");
    }
}
