package net.frosthain.core.registry;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.content.ClassDefinition;
import net.frosthain.core.content.SourceDefinition;
import net.frosthain.core.content.SpeciesDefinition;
import net.frosthain.core.validation.ContentValidator;
import net.frosthain.core.validation.Diagnostic;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the shipped base content pack. If this fails, the content in /content is broken. */
class BaseContentPackTest {

    private final ContentRegistry registry = TestContent.baseRegistry();

    @Test
    void baseContentHasNoValidationErrors() {
        List<Diagnostic> errors = ContentValidator.validate(registry).stream()
                .filter(d -> d.severity() == Diagnostic.Severity.ERROR)
                .toList();
        assertEquals(List.of(), errors);
    }

    @Test
    void containsAllTwelveSrdClassesTheRuneMageAndNineSpecies() {
        Set<String> classes = Set.of("barbarian", "bard", "cleric", "druid", "fighter", "monk", "paladin", "ranger", "rogue",
                "sorcerer", "warlock", "wizard");
        for (String clazz : classes) {
            assertTrue(registry.contains(ContentType.CLASSES, ContentId.of(clazz)), clazz);
        }
        assertTrue(registry.contains(ContentType.CLASSES, ContentId.of("rune_mage")), "Runenmagier (FROSTHAIN-Klasse)");
        assertEquals(13, registry.all(ContentType.CLASSES).size());
        assertEquals(9, registry.all(ContentType.SPECIES).size());
    }

    @Test
    void everyClassHasAtLeastOneSubclass() {
        for (ClassDefinition clazz : registry.all(ContentType.CLASSES).values()) {
            boolean hasSubclass = registry.all(ContentType.SUBCLASSES).values().stream()
                    .anyMatch(sub -> sub.parentClass().equals(clazz.id()));
            assertTrue(hasSubclass, "keine Unterklasse für " + clazz.id());
        }
    }

    @Test
    void srdContentIsMarkedAsAdaptedAndAttributed() {
        SourceDefinition srd = registry.get(ContentType.SOURCES, ContentId.of("srd_5_2_1")).orElseThrow();
        assertEquals("CC-BY-4.0", srd.license());
        assertTrue(srd.attribution().orElseThrow().contains("System Reference Document 5.2.1"));
        for (ClassDefinition clazz : registry.all(ContentType.CLASSES).values()) {
            if (clazz.source().equals(srd.id())) {
                assertTrue(clazz.adapted(), clazz.id() + " stammt aus dem SRD und wurde angepasst – adapted muss true sein");
            }
        }
    }

    @Test
    void speciesWithRequiredFeaturesOfferAtLeastOnePart() {
        SpeciesDefinition tiefling = registry.get(ContentType.SPECIES, ContentId.of("tiefling")).orElseThrow();
        assertEquals(List.of("horns", "tail"), tiefling.appearance().requiredSlots());
        assertTrue(tiefling.lineageRequired());
        assertEquals(3, tiefling.lineages().size());
    }

    @Test
    void germanAndEnglishTranslationsAreComplete() {
        List<Diagnostic> translationProblems = ContentValidator.validate(registry).stream()
                .filter(d -> d.location().startsWith("lang/"))
                .toList();
        assertEquals(List.of(), translationProblems);
    }
}
