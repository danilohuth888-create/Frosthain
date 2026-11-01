package net.frosthain.core.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.frosthain.core.ContentId;
import net.frosthain.core.character.Appearance;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.content.AbilityScore;
import net.frosthain.core.content.SpeciesDefinition;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * JSON representation of {@link CharacterSheet} including schema versioning.
 *
 * <p>The mod stores this JSON in a player data attachment; the launcher reads the same format for its
 * character overview. Field names are snake_case and stable; any change requires a new schema version
 * plus a {@link SchemaMigrator.Migration}.
 */
public final class CharacterSheetCodec {

    /** Version 1 is the first persisted format. */
    public static final int CURRENT_VERSION = 1;

    private static final SchemaMigrator MIGRATOR = new SchemaMigrator(CURRENT_VERSION, List.of());

    private CharacterSheetCodec() {
    }

    public static JsonObject encode(CharacterSheet sheet) {
        JsonObject json = new JsonObject();
        json.addProperty(SchemaMigrator.VERSION_KEY, CURRENT_VERSION);
        json.addProperty("character_id", sheet.characterId().toString());
        json.addProperty("name", sheet.name());
        json.addProperty("species", sheet.species().toString());
        sheet.lineage().ifPresent(lineage -> json.addProperty("lineage", lineage));
        json.addProperty("background", sheet.background().toString());
        JsonObject abilities = new JsonObject();
        sheet.abilityScores().forEach((ability, value) -> abilities.addProperty(ability.key(), value));
        json.add("ability_scores", abilities);
        JsonArray classes = new JsonArray();
        sheet.classLevels().forEach((classId, level) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("class", classId.toString());
            entry.addProperty("level", level);
            Optional.ofNullable(sheet.subclasses().get(classId)).ifPresent(sub -> entry.addProperty("subclass", sub.toString()));
            classes.add(entry);
        });
        json.add("classes", classes);
        json.addProperty("experience", sheet.experience());
        JsonObject talents = new JsonObject();
        sheet.talents().forEach((ref, rank) -> talents.addProperty(ref.toString(), rank));
        json.add("talents", talents);
        json.add("appearance", encodeAppearance(sheet.appearance()));
        JsonObject biography = new JsonObject();
        biography.addProperty("text", sheet.biography().text());
        JsonArray traits = new JsonArray();
        sheet.biography().personalityTraits().forEach(traits::add);
        biography.add("personality_traits", traits);
        json.add("biography", biography);
        return json;
    }

    /** Decodes stored JSON, migrating older schema versions first. */
    public static Decoded decode(JsonObject stored) {
        SchemaMigrator.Outcome outcome = MIGRATOR.migrate(stored);
        JsonObjectReader json = new JsonObjectReader(outcome.data(), "$");
        json.integer(SchemaMigrator.VERSION_KEY);
        UUID id;
        try {
            id = UUID.fromString(json.string("character_id"));
        } catch (IllegalArgumentException e) {
            throw new ContentParseException("$.character_id", "ungültige UUID", e);
        }
        Map<AbilityScore, Integer> abilities = new EnumMap<>(AbilityScore.class);
        JsonObjectReader abilityJson = json.object("ability_scores");
        for (AbilityScore ability : AbilityScore.values()) {
            abilities.put(ability, abilityJson.integerInRange(ability.key(), 10, 1, 30));
        }
        abilityJson.finish();
        Map<ContentId, Integer> classLevels = new LinkedHashMap<>();
        Map<ContentId, ContentId> subclasses = new TreeMap<>();
        json.objectList("classes", entry -> {
            ContentId classId = entry.id("class");
            classLevels.put(classId, entry.integerInRange("level", 1, 1, 20));
            entry.optId("subclass").ifPresent(sub -> subclasses.put(classId, sub));
            return classId;
        });
        if (classLevels.isEmpty()) {
            throw new ContentParseException("$.classes", "mindestens eine Klasse erwartet");
        }
        Map<TalentRef, Integer> talents = new TreeMap<>();
        JsonObjectReader talentJson = json.object("talents");
        for (String key : talentJson.keys()) {
            try {
                talents.put(TalentRef.parse(key), talentJson.integerInRange(key, 1, 1, 5));
            } catch (IllegalArgumentException e) {
                throw new ContentParseException(talentJson.childPath(key), e.getMessage(), e);
            }
        }
        talentJson.finish();
        double experience = json.number("experience", 0);
        if (experience < 0 || experience > 1e12) {
            throw new ContentParseException("$.experience", "ungültige Erfahrung " + experience);
        }
        JsonObjectReader bio = json.object("biography");
        CharacterSheet.Biography biography = new CharacterSheet.Biography(bio.string("text"), bio.stringList("personality_traits"));
        bio.finish();
        CharacterSheet sheet = new CharacterSheet(id, json.string("name"), json.id("species"), json.optString("lineage"),
                json.id("background"), abilities, classLevels, subclasses, (long) experience, talents,
                decodeAppearance(json.object("appearance")), biography);
        json.finish();
        return new Decoded(sheet, outcome);
    }

    /** Result of decoding; {@code migration.migrated()} tells the caller to back up the original first. */
    public record Decoded(CharacterSheet sheet, SchemaMigrator.Outcome migration) {
    }

    private static JsonObject encodeAppearance(Appearance appearance) {
        JsonObject json = new JsonObject();
        json.addProperty("body_type", appearance.bodyType().name().toLowerCase(Locale.ROOT));
        json.addProperty("height_scale", appearance.heightScale());
        json.addProperty("skin_color", appearance.skinColor());
        appearance.skinPattern().ifPresent(p -> json.addProperty("skin_pattern", p.toString()));
        json.addProperty("eye_style", appearance.eyeStyle().toString());
        json.addProperty("eye_color", appearance.eyeColor());
        json.addProperty("hair_style", appearance.hairStyle().toString());
        json.addProperty("hair_color", appearance.hairColor());
        appearance.beardStyle().ifPresent(b -> json.addProperty("beard_style", b.toString()));
        JsonObject features = new JsonObject();
        new TreeMap<>(appearance.features()).forEach((slot, part) -> features.addProperty(slot, part.toString()));
        json.add("features", features);
        JsonArray markings = new JsonArray();
        for (Appearance.Marking marking : appearance.markings()) {
            JsonObject m = new JsonObject();
            m.addProperty("kind", marking.kind().name().toLowerCase(Locale.ROOT));
            m.addProperty("pattern", marking.pattern().toString());
            m.addProperty("color", marking.color());
            m.addProperty("region", marking.region().name().toLowerCase(Locale.ROOT));
            markings.add(m);
        }
        json.add("markings", markings);
        return json;
    }

    private static Appearance decodeAppearance(JsonObjectReader json) {
        Map<String, ContentId> features = new TreeMap<>();
        json.optObject("features").ifPresent(reader -> {
            for (String slot : SpeciesDefinition.AppearanceRules.KNOWN_SLOTS) {
                reader.optId(slot).ifPresent(part -> features.put(slot, part));
            }
            reader.finish();
        });
        Appearance appearance = new Appearance(
                json.enumValue("body_type", Appearance.BodyType.class),
                json.number("height_scale"),
                json.string("skin_color"),
                json.optId("skin_pattern"),
                json.id("eye_style"),
                json.string("eye_color"),
                json.id("hair_style"),
                json.string("hair_color"),
                json.optId("beard_style"),
                features,
                json.objectList("markings", m -> new Appearance.Marking(
                        m.enumValue("kind", Appearance.Marking.Kind.class),
                        m.id("pattern"),
                        m.string("color"),
                        m.enumValue("region", Appearance.Marking.Region.class))));
        json.finish();
        return appearance;
    }
}
