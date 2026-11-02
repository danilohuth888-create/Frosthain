package net.frosthain.ui;

import net.frosthain.core.ContentId;
import net.frosthain.core.character.Appearance;
import net.frosthain.core.character.CharacterFactory;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.content.AbilityScore;
import net.frosthain.core.content.SpeciesDefinition;
import net.frosthain.core.registry.ContentLoader;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.core.registry.DirectoryContentSource;
import net.frosthain.core.rules.AbilityScoreRules;
import net.frosthain.ui.text.TextMeasurer;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Shared fixtures for UI tests: the real base content and simple characters. */
public final class UiTestContent {

    private static ContentRegistry registry;

    private UiTestContent() {
    }

    public static synchronized ContentRegistry registry() {
        if (registry == null) {
            Path root = Path.of(System.getProperty("frosthain.contentRoot", "../content"));
            DirectoryContentSource.Scan scan = DirectoryContentSource.scanAllPacks(root);
            ContentLoader.Result result = ContentLoader.load(scan.contentFiles(), scan.langFiles());
            assertFalse(result.hasErrors(), result.diagnostics()::toString);
            registry = result.registry();
        }
        return registry;
    }

    public static Translator german() {
        return new Translator(registry(), "de_de");
    }

    /** Every character is 8 px wide – deterministic layout tests without fonts. */
    public static final TextMeasurer MONO = (text, role, size) -> text.length() * 8;

    public static CharacterSheet character(String clazz) {
        SpeciesDefinition human = registry().get(ContentType.SPECIES, ContentId.of("human")).orElseThrow();
        Map<AbilityScore, Integer> scores = new EnumMap<>(AbilityScore.class);
        int[] values = {15, 13, 14, 8, 12, 10};
        for (AbilityScore ability : AbilityScore.values()) {
            scores.put(ability, values[ability.ordinal()]);
        }
        CharacterFactory.CreationRequest request = new CharacterFactory.CreationRequest("Arwen", human.id(), Optional.empty(),
                ContentId.of(clazz), ContentId.of("soldier"), AbilityScoreRules.Method.STANDARD_ARRAY, scores,
                Map.of(AbilityScore.STRENGTH, 2, AbilityScore.CONSTITUTION, 1), Appearance.defaultFor(human),
                CharacterSheet.Biography.EMPTY);
        return ((CharacterFactory.Result.Created) new CharacterFactory(registry()).create(request, UUID.randomUUID())).sheet();
    }
}
