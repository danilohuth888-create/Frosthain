package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A playable species (Abstammung) with optional lineages (Unterart/Herkunft) and the
 * appearance options it unlocks in the character editor.
 */
public record SpeciesDefinition(
        ContentId id,
        ContentId source,
        boolean adapted,
        Size size,
        List<Modifier> traits,
        List<Lineage> lineages,
        boolean lineageRequired,
        AppearanceRules appearance
) implements ContentDefinition {

    public enum Size { SMALL, MEDIUM, LARGE }

    /** A lineage; its id is {@code <species id>/<key>}, e.g. {@code frosthain:elf/high_elf}. */
    public record Lineage(ContentId id, String key, List<Modifier> traits) {
        public String translationKey(SpeciesDefinition species) {
            return species.translationKey() + ".lineage." + key;
        }
    }

    /**
     * Species-specific limits of the visual editor.
     *
     * @param heightScaleMin  minimum visual height factor (1.0 = Minecraft standard)
     * @param heightScaleMax  maximum visual height factor
     * @param featureSlots    allowed part ids per slot (ears, horns, tail, ...). Slots not listed are unavailable.
     * @param requiredSlots   slots that must be filled (e.g. tiefling horns)
     */
    public record AppearanceRules(
            double heightScaleMin,
            double heightScaleMax,
            Map<String, List<ContentId>> featureSlots,
            List<String> requiredSlots
    ) {
        public static final List<String> KNOWN_SLOTS = List.of("ears", "horns", "tail", "scales", "tusks", "markings");
    }

    public static SpeciesDefinition parse(ContentId id, JsonObjectReader json) {
        Size size = json.enumValue("size", Size.class);
        List<Modifier> traits = json.objectList("traits", Modifier::parse);
        List<Lineage> lineages = json.objectList("lineages", lineage -> {
            String key = lineage.string("key");
            ContentId lineageId;
            try {
                lineageId = id.child(key);
            } catch (IllegalArgumentException e) {
                throw new ContentParseException(lineage.childPath("key"), e.getMessage(), e);
            }
            return new Lineage(lineageId, key, lineage.objectList("traits", Modifier::parse));
        });
        boolean lineageRequired = json.bool("lineage_required", !lineages.isEmpty());
        if (lineageRequired && lineages.isEmpty()) {
            throw new ContentParseException(json.childPath("lineage_required"), "true, aber keine Abstammungslinien definiert");
        }
        AppearanceRules appearance = json.optObject("appearance")
                .map(SpeciesDefinition::parseAppearance)
                .orElse(new AppearanceRules(0.95, 1.05, Map.of(), List.of()));
        return new SpeciesDefinition(id, json.id("source"), json.bool("adapted", false), size, traits,
                lineages, lineageRequired, appearance);
    }

    private static AppearanceRules parseAppearance(JsonObjectReader json) {
        List<Double> range = json.list("height_scale", JsonObjectReader::asDouble);
        double min = 0.95;
        double max = 1.05;
        if (!range.isEmpty()) {
            if (range.size() != 2 || range.get(0) > range.get(1) || range.get(0) < 0.5 || range.get(1) > 1.5) {
                throw new ContentParseException(json.childPath("height_scale"), "[min, max] mit 0.5 <= min <= max <= 1.5 erwartet");
            }
            min = range.get(0);
            max = range.get(1);
        }
        Map<String, List<ContentId>> slots = new LinkedHashMap<>();
        Optional<JsonObjectReader> features = json.optObject("features");
        if (features.isPresent()) {
            JsonObjectReader featureReader = features.get();
            for (String slot : AppearanceRules.KNOWN_SLOTS) {
                List<ContentId> parts = featureReader.idList(slot);
                if (!parts.isEmpty()) {
                    slots.put(slot, parts);
                }
            }
            featureReader.finish();
        }
        List<String> required = new ArrayList<>(json.stringList("required_slots"));
        for (String slot : required) {
            if (!slots.containsKey(slot)) {
                throw new ContentParseException(json.childPath("required_slots"), "Pflicht-Slot '" + slot + "' hat keine erlaubten Teile");
            }
        }
        json.finish();
        return new AppearanceRules(min, max, Map.copyOf(slots), List.copyOf(required));
    }

    public Optional<Lineage> lineage(String key) {
        return lineages.stream().filter(l -> l.key().equals(key)).findFirst();
    }

    public String translationKey() {
        return "species." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(translationKey());
        keys.add(translationKey() + ".desc");
        for (Lineage lineage : lineages) {
            keys.add(lineage.translationKey(this));
        }
        return keys;
    }
}
