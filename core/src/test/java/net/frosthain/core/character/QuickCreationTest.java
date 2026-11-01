package net.frosthain.core.character;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.content.AbilityScore;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuickCreationTest {

    private final ContentRegistry registry = TestContent.baseRegistry();

    @Test
    void everyCombinationOfTheBaseContentIsAccepted() {
        CharacterFactory factory = new CharacterFactory(registry);
        List<String> rejected = new ArrayList<>();
        for (ContentId species : registry.all(ContentType.SPECIES).keySet()) {
            for (ContentId clazz : registry.all(ContentType.CLASSES).keySet()) {
                for (ContentId background : registry.all(ContentType.BACKGROUNDS).keySet()) {
                    CharacterFactory.Result result = factory.create(
                            QuickCreation.request(registry, "Arwen", species, clazz, background), UUID.randomUUID());
                    if (result instanceof CharacterFactory.Result.Rejected r) {
                        rejected.add(species + "/" + clazz + "/" + background + ": " + r.problems());
                    }
                }
            }
        }
        assertEquals(List.of(), rejected);
    }

    @Test
    void highestValuesGoToThePrimaryAbilities() {
        Map<AbilityScore, Integer> scores = QuickCreation.baseScores(List.of(15, 14, 13, 12, 10, 8),
                List.of(AbilityScore.INTELLIGENCE));
        assertEquals(15, scores.get(AbilityScore.INTELLIGENCE));
        assertEquals(14, scores.get(AbilityScore.CONSTITUTION));
        assertEquals(6, scores.size());
    }

    @Test
    void backgroundBonusPrefersPrimaryAbilities() {
        Map<AbilityScore, Integer> bonus = QuickCreation.backgroundBonus(
                List.of(AbilityScore.STRENGTH, AbilityScore.DEXTERITY, AbilityScore.CONSTITUTION), List.of(AbilityScore.DEXTERITY));
        assertEquals(Map.of(AbilityScore.DEXTERITY, 2, AbilityScore.CONSTITUTION, 1), bonus);
    }

    @Test
    void requiredLineageIsPreselectedAndValid() {
        CharacterFactory.Result result = new CharacterFactory(registry).create(QuickCreation.request(registry, "Zarek",
                ContentId.of("tiefling"), ContentId.of("warlock"), ContentId.of("criminal")), UUID.randomUUID());
        CharacterSheet sheet = assertInstanceOf(CharacterFactory.Result.Created.class, result).sheet();
        assertEquals(true, sheet.lineage().isPresent());
    }

    @Test
    void unknownChoicesAreRejectedBeforeTheFactory() {
        assertThrows(IllegalArgumentException.class, () -> QuickCreation.request(registry, "X",
                ContentId.of("unicorn"), ContentId.of("fighter"), ContentId.of("soldier")));
    }
}
