package net.frosthain.core.persistence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.character.Appearance;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.Characters;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.progression.LevelingRules;
import net.frosthain.core.progression.TalentAllocator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterSheetCodecTest {

    private static CharacterSheet richCharacter() {
        LevelingRules leveling = new LevelingRules(TestContent.baseRegistry());
        TalentAllocator talents = new TalentAllocator(TestContent.baseRegistry());
        CharacterSheet sheet = Characters.create(Characters.request("tiefling", Optional.of("infernal"), "fighter"))
                .withExperience(1_800);
        sheet = leveling.levelUp(sheet, ContentId.of("fighter"));
        sheet = leveling.levelUp(sheet, ContentId.of("fighter"));
        sheet = leveling.chooseSubclass(sheet, ContentId.of("fighter/rune_warden"));
        sheet = talents.allocate(sheet, new TalentRef(ContentId.of("general"), "combat_stance"));
        sheet = talents.allocate(sheet, new TalentRef(ContentId.of("fighter"), "weapon_focus"));
        Appearance base = sheet.appearance();
        Appearance appearance = new Appearance(Appearance.BodyType.SLIM, 1.03, "#a0405a", Optional.of(ContentId.of("skin/freckles")),
                base.eyeStyle(), "#ffd166", ContentId.of("hair/braid"), "#20121a", Optional.empty(), base.features(),
                List.of(new Appearance.Marking(Appearance.Marking.Kind.MAGIC, ContentId.of("marking/rune_circle"), "#66ccff",
                        Appearance.Marking.Region.LEFT_ARM)));
        return new CharacterSheet(sheet.characterId(), sheet.name(), sheet.species(), sheet.lineage(), sheet.background(),
                sheet.abilityScores(), sheet.classLevels(), sheet.subclasses(), sheet.experience(), sheet.talents(), appearance,
                new CharacterSheet.Biography("Geboren im Schatten des Frostgipfels.", List.of("neugierig", "loyal")));
    }

    @Test
    void roundTripPreservesEverything() {
        CharacterSheet original = richCharacter();
        JsonObject json = CharacterSheetCodec.encode(original);
        assertEquals(CharacterSheetCodec.CURRENT_VERSION, json.get("schema_version").getAsInt());
        CharacterSheetCodec.Decoded decoded = CharacterSheetCodec.decode(JsonParser.parseString(json.toString()).getAsJsonObject());
        assertEquals(original, decoded.sheet());
        assertFalse(decoded.migration().migrated());
    }

    @Test
    void refusesDataFromNewerVersions() {
        JsonObject json = CharacterSheetCodec.encode(richCharacter());
        json.addProperty("schema_version", CharacterSheetCodec.CURRENT_VERSION + 1);
        assertThrows(SchemaMigrator.FutureVersionException.class, () -> CharacterSheetCodec.decode(json));
    }

    @Test
    void rejectsUnknownFieldsInsteadOfDroppingData() {
        JsonObject json = CharacterSheetCodec.encode(richCharacter());
        json.addProperty("guild", "Frostwache");
        ContentParseException error = assertThrows(ContentParseException.class, () -> CharacterSheetCodec.decode(json));
        assertTrue(error.getMessage().contains("guild"));
    }

    @Test
    void migratorAppliesStepsInOrderAndKeepsOriginalUntouched() {
        SchemaMigrator migrator = new SchemaMigrator(3, List.of(
                new SchemaMigrator.Migration(2, "xp → experience", data -> {
                    data.add("experience", data.remove("xp"));
                    return data;
                }),
                new SchemaMigrator.Migration(1, "name ergänzen", data -> {
                    data.addProperty("name", "Unbenannt");
                    return data;
                })));
        JsonObject stored = JsonParser.parseString("{\"schema_version\": 1, \"xp\": 42}").getAsJsonObject();
        SchemaMigrator.Outcome outcome = migrator.migrate(stored);
        assertEquals(3, outcome.data().get("schema_version").getAsInt());
        assertEquals(42, outcome.data().get("experience").getAsInt());
        assertEquals("Unbenannt", outcome.data().get("name").getAsString());
        assertEquals(2, outcome.appliedSteps().size());
        assertEquals(1, outcome.originalVersion());
        assertTrue(stored.has("xp"), "Original darf nicht verändert werden (für das Backup)");
    }

    @Test
    void migratorRequiresACompleteChain() {
        assertThrows(IllegalArgumentException.class, () -> new SchemaMigrator(3, List.of(
                new SchemaMigrator.Migration(1, "nur ein Schritt", data -> data))));
    }

    @Test
    void talentReferencesUseStableTextFormat() {
        JsonObject json = CharacterSheetCodec.encode(richCharacter());
        assertEquals(Map.of("frosthain:fighter#weapon_focus", 1, "frosthain:general#combat_stance", 1),
                Map.of("frosthain:fighter#weapon_focus", json.getAsJsonObject("talents").get("frosthain:fighter#weapon_focus").getAsInt(),
                        "frosthain:general#combat_stance", json.getAsJsonObject("talents").get("frosthain:general#combat_stance").getAsInt()));
        assertEquals(2, json.getAsJsonObject("talents").size());
    }
}
