package net.frosthain.core.character;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.content.AbilityScore;
import net.frosthain.core.content.SpeciesDefinition;
import net.frosthain.core.registry.ContentType;
import net.frosthain.core.rules.AbilityScoreRules;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.fail;

/** Factory for realistic test characters built through the real {@link CharacterFactory}. */
public final class Characters {

    private Characters() {
    }

    public static Map<AbilityScore, Integer> scores(int str, int dex, int con, int intel, int wis, int cha) {
        Map<AbilityScore, Integer> scores = new EnumMap<>(AbilityScore.class);
        scores.put(AbilityScore.STRENGTH, str);
        scores.put(AbilityScore.DEXTERITY, dex);
        scores.put(AbilityScore.CONSTITUTION, con);
        scores.put(AbilityScore.INTELLIGENCE, intel);
        scores.put(AbilityScore.WISDOM, wis);
        scores.put(AbilityScore.CHARISMA, cha);
        return scores;
    }

    public static CharacterFactory.CreationRequest request(String species, Optional<String> lineage, String clazz) {
        SpeciesDefinition speciesDefinition = TestContent.baseRegistry().get(ContentType.SPECIES, ContentId.of(species)).orElseThrow();
        return new CharacterFactory.CreationRequest("Arwen Eisherz", ContentId.of(species), lineage, ContentId.of(clazz),
                ContentId.of("soldier"), AbilityScoreRules.Method.STANDARD_ARRAY,
                scores(15, 13, 14, 8, 12, 10),
                Map.of(AbilityScore.STRENGTH, 2, AbilityScore.CONSTITUTION, 1),
                Appearance.defaultFor(speciesDefinition), CharacterSheet.Biography.EMPTY);
    }

    /** Human soldier fighter: STR 17, DEX 13, CON 15, INT 8, WIS 12, CHA 10. */
    public static CharacterSheet humanFighter() {
        return create(request("human", Optional.empty(), "fighter"));
    }

    public static CharacterSheet create(CharacterFactory.CreationRequest request) {
        CharacterFactory.Result result = new CharacterFactory(TestContent.baseRegistry()).create(request, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return switch (result) {
            case CharacterFactory.Result.Created created -> created.sheet();
            case CharacterFactory.Result.Rejected rejected -> {
                fail("Testcharakter abgelehnt: " + rejected.problems());
                yield null;
            }
        };
    }
}
