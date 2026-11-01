package net.frosthain.core.registry;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.validation.Diagnostic;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentLoaderTest {

    private static boolean hasError(ContentLoader.Result result, String fragment) {
        return result.diagnostics().stream()
                .anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR && d.message().contains(fragment));
    }

    @Test
    void reportsTyposInOptionalFieldsAsUnknownFields() {
        ContentLoader.Result result = new TestContent.Pack()
                .file("skills/climbing", """
                        {"source": "frosthain:original", "ability": "strength", "adaptd": true, "_comment": "ok"}""")
                .load();
        assertTrue(hasError(result, "Unbekannte Felder [adaptd]"), result.diagnostics()::toString);
        assertFalse(result.registry().contains(ContentType.SKILLS, ContentId.of("climbing")));
    }

    @Test
    void reportsMissingRequiredFieldsWithJsonPath() {
        ContentLoader.Result result = new TestContent.Pack()
                .file("skills/climbing", """
                        {"source": "frosthain:original", "abilty": "strength"}""")
                .load();
        assertTrue(hasError(result, "$.ability: Pflichtfeld fehlt"), result.diagnostics()::toString);
    }

    @Test
    void reportsNestedJsonPaths() {
        ContentLoader.Result result = new TestContent.Pack()
                .file("species/test", """
                        {"source": "frosthain:original", "size": "medium",
                         "traits": [{"type": "attribute", "attribute": "minecraft:max_health", "operation": "add_value", "amount": 1},
                                    {"type": "teleport"}]}""")
                .load();
        assertTrue(hasError(result, "$.traits[1].type: Unbekannter Modifikator-Typ 'teleport'"), result.diagnostics()::toString);
    }

    @Test
    void rejectsMalformedJsonWithoutAbortingOtherFiles() {
        ContentLoader.Result result = new TestContent.Pack()
                .file("skills/broken", "{\"source\": ")
                .file("skills/fine", """
                        {"source": "frosthain:original", "ability": "wisdom"}""")
                .load();
        assertTrue(hasError(result, "Ungültiges JSON"), result.diagnostics()::toString);
        assertTrue(result.registry().contains(ContentType.SKILLS, ContentId.of("fine")));
    }

    @Test
    void rejectsUnknownEnumValuesWithAllowedValues() {
        ContentLoader.Result result = new TestContent.Pack()
                .file("skills/odd", """
                        {"source": "frosthain:original", "ability": "luck"}""")
                .load();
        assertTrue(hasError(result, "Unbekannter Wert 'luck'"), result.diagnostics()::toString);
    }

    @Test
    void laterPacksOverrideEarlierOnesAndReportIt() {
        String first = "{\"source\": \"frosthain:original\", \"ability\": \"strength\"}";
        String second = "{\"source\": \"frosthain:original\", \"ability\": \"dexterity\"}";
        List<ContentFile> files = List.of(
                new ContentFile("base", "frosthain", "skills", "athletics", "base/athletics.json", () -> new StringReader(first)),
                new ContentFile("addon", "frosthain", "skills", "athletics", "addon/athletics.json", () -> new StringReader(second)));
        ContentLoader.Result result = ContentLoader.load(files, List.of());
        assertEquals("DEXTERITY", result.registry().get(ContentType.SKILLS, ContentId.of("athletics")).orElseThrow().ability().name());
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.severity() == Diagnostic.Severity.INFO
                && d.message().contains("überschreibt")), result.diagnostics()::toString);
    }

    @Test
    void validatesRulesFiles() {
        ContentLoader.Result result = new TestContent.Pack()
                .file("rules/progression", """
                        {"max_level": 3, "experience_thresholds": [0, 500, 400], "multiclass": {}}""")
                .load();
        assertTrue(hasError(result, "Schwellen müssen streng steigen"), result.diagnostics()::toString);
    }
}
