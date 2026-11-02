package net.frosthain.core.character;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.content.SpeciesDefinition;
import net.frosthain.core.persistence.CharacterSheetCodec;
import net.frosthain.core.registry.ContentType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppearanceOptionsTest {

    @Test
    void everyOfferedOptionIsValidForEverySpecies() {
        List<String> problems = new ArrayList<>();
        for (SpeciesDefinition species : TestContent.baseRegistry().all(ContentType.SPECIES).values()) {
            Appearance base = Appearance.defaultFor(species);
            for (int i = 0; i < AppearanceOptions.SKIN_COLORS.size(); i++) {
                Appearance a = AppearanceOptions.with(base, Appearance.BodyType.SLIM,
                        AppearanceOptions.stepHeight(species, base.heightScale(), i - 4),
                        AppearanceOptions.SKIN_COLORS.get(i),
                        AppearanceOptions.EYE_COLORS.get(i % AppearanceOptions.EYE_COLORS.size()),
                        AppearanceOptions.HAIR_STYLES.get(i % AppearanceOptions.HAIR_STYLES.size()),
                        AppearanceOptions.HAIR_COLORS.get(i % AppearanceOptions.HAIR_COLORS.size()));
                for (String slot : species.appearance().featureSlots().keySet()) {
                    List<Optional<ContentId>> options = AppearanceOptions.featureOptions(species, slot);
                    a = AppearanceOptions.withFeature(a, slot, options.get(i % options.size()));
                }
                a.validate(species).forEach(p -> problems.add(species.id() + ": " + p));
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void heightStaysInsideTheSpeciesRange() {
        SpeciesDefinition dwarf = TestContent.baseRegistry().get(ContentType.SPECIES, ContentId.of("dwarf")).orElseThrow();
        double height = Appearance.defaultFor(dwarf).heightScale();
        for (int i = 0; i < 50; i++) {
            height = AppearanceOptions.stepHeight(dwarf, height, -1);
        }
        assertEquals(dwarf.appearance().heightScaleMin(), height, 1e-9);
    }

    @Test
    void requiredFeaturesCannotBeRemoved() {
        SpeciesDefinition elf = TestContent.baseRegistry().get(ContentType.SPECIES, ContentId.of("elf")).orElseThrow();
        assertTrue(AppearanceOptions.featureOptions(elf, "ears").stream().allMatch(Optional::isPresent));
    }

    @Test
    void chosenAppearanceIsStoredWithTheCharacterAndSurvivesTheCodec() {
        var registry = TestContent.baseRegistry();
        SpeciesDefinition tiefling = registry.get(ContentType.SPECIES, ContentId.of("tiefling")).orElseThrow();
        Appearance chosen = AppearanceOptions.withFeature(
                AppearanceOptions.with(Appearance.defaultFor(tiefling), Appearance.BodyType.SLIM, 1.02, "#7d5ba6", "#c9a227",
                        ContentId.of("hair/braided"), "#9a2f2f"),
                "horns", Optional.of(ContentId.parse("frosthain:horns/crown")));
        CharacterFactory.Result result = new CharacterFactory(registry).create(QuickCreation.request(registry, "Zarek",
                tiefling.id(), ContentId.of("warlock"), ContentId.of("criminal"), Optional.of(chosen)), UUID.randomUUID());
        CharacterSheet sheet = assertInstanceOf(CharacterFactory.Result.Created.class, result).sheet();
        assertEquals(chosen, sheet.appearance());
        assertEquals(chosen, CharacterSheetCodec.decodeAppearance(CharacterSheetCodec.encodeAppearance(chosen)));
    }
}
