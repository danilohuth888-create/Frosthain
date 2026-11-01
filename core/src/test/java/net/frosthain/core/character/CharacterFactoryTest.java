package net.frosthain.core.character;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.content.AbilityScore;
import net.frosthain.core.registry.ContentType;
import net.frosthain.core.rules.AbilityScoreRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterFactoryTest {

    private final CharacterFactory factory = new CharacterFactory(TestContent.baseRegistry());

    private List<String> problems(CharacterFactory.CreationRequest request) {
        CharacterFactory.Result result = factory.create(request, UUID.randomUUID());
        return assertInstanceOf(CharacterFactory.Result.Rejected.class, result).problems();
    }

    private static boolean contains(List<String> problems, String fragment) {
        return problems.stream().anyMatch(p -> p.contains(fragment));
    }

    @Test
    void createsLevelOneCharacterWithBackgroundBonus() {
        CharacterSheet sheet = Characters.humanFighter();
        assertEquals(1, sheet.characterLevel());
        assertEquals(ContentId.of("fighter"), sheet.startingClass());
        assertEquals(17, sheet.abilityScore(AbilityScore.STRENGTH));
        assertEquals(15, sheet.abilityScore(AbilityScore.CONSTITUTION));
        assertEquals(0, sheet.experience());
        assertTrue(sheet.talents().isEmpty());
    }

    @Test
    void pointBuyWithinBudgetIsAccepted() {
        var base = Characters.request("human", Optional.empty(), "wizard");
        var request = new CharacterFactory.CreationRequest(base.name(), base.species(), base.lineage(), base.characterClass(),
                ContentId.of("sage"), AbilityScoreRules.Method.POINT_BUY, Characters.scores(8, 14, 14, 15, 12, 8),
                Map.of(AbilityScore.INTELLIGENCE, 2, AbilityScore.CONSTITUTION, 1), base.appearance(), base.biography());
        // cost: 0 + 7 + 7 + 9 + 4 + 0 = 27
        assertInstanceOf(CharacterFactory.Result.Created.class, factory.create(request, UUID.randomUUID()));
    }

    @Test
    void pointBuyOverBudgetIsRejected() {
        var base = Characters.request("human", Optional.empty(), "wizard");
        var request = new CharacterFactory.CreationRequest(base.name(), base.species(), base.lineage(), base.characterClass(),
                base.background(), AbilityScoreRules.Method.POINT_BUY, Characters.scores(15, 15, 15, 10, 8, 8),
                base.backgroundBonus(), base.appearance(), base.biography());
        // cost: 9 + 9 + 9 + 2 + 0 + 0 = 29 > 27
        assertTrue(contains(problems(request), "Punktekauf überschritten: 29 von 27"));
    }

    @Test
    void backgroundBonusMustUseBackgroundAbilities() {
        var base = Characters.request("human", Optional.empty(), "fighter");
        var request = new CharacterFactory.CreationRequest(base.name(), base.species(), base.lineage(), base.characterClass(),
                base.background(), base.method(), base.baseScores(), Map.of(AbilityScore.CHARISMA, 2, AbilityScore.STRENGTH, 1),
                base.appearance(), base.biography());
        assertTrue(contains(problems(request), "charisma gehört nicht zu den Attributen"));
    }

    @Test
    void speciesWithLineagesRequireAChoice() {
        assertTrue(contains(problems(Characters.request("elf", Optional.empty(), "ranger")), "Abstammungslinie gewählt"));
        assertTrue(contains(problems(Characters.request("elf", Optional.of("ice_elf"), "ranger")), "'ice_elf'"));
        assertInstanceOf(CharacterFactory.Result.Created.class,
                factory.create(Characters.request("elf", Optional.of("wood_elf"), "ranger"), UUID.randomUUID()));
    }

    @Test
    void appearanceIsValidatedAgainstSpecies() {
        var base = Characters.request("tiefling", Optional.of("infernal"), "warlock");
        Appearance withoutHorns = new Appearance(base.appearance().bodyType(), 1.0, "#aa3344", Optional.empty(),
                base.appearance().eyeStyle(), "#ffcc00", base.appearance().hairStyle(), "#111111", Optional.empty(),
                Map.of("tail", ContentId.of("tail/fiend"), "ears", ContentId.of("ears/pointed_long")), List.of());
        var request = new CharacterFactory.CreationRequest(base.name(), base.species(), base.lineage(), base.characterClass(),
                base.background(), base.method(), base.baseScores(), base.backgroundBonus(), withoutHorns, base.biography());
        List<String> problems = problems(request);
        assertTrue(contains(problems, "'horns' ist für diese Spezies Pflicht"), problems::toString);
        assertTrue(contains(problems, "'ears' ist für diese Spezies nicht verfügbar"), problems::toString);
    }

    @Test
    void defaultAppearanceIsValidForEverySpecies() {
        TestContent.baseRegistry().all(ContentType.SPECIES).values().forEach(species ->
                assertEquals(List.of(), Appearance.defaultFor(species).validate(species), species.id().toString()));
    }

    @Test
    void namesAreRestricted() {
        var base = Characters.request("human", Optional.empty(), "fighter");
        for (String name : List.of("", "   ", "x".repeat(33), "<script>", "1337")) {
            var request = new CharacterFactory.CreationRequest(name, base.species(), base.lineage(), base.characterClass(),
                    base.background(), base.method(), base.baseScores(), base.backgroundBonus(), base.appearance(), base.biography());
            assertTrue(contains(problems(request), "Name muss"), name);
        }
        var umlaut = new CharacterFactory.CreationRequest("Jörg Öfenbrück-D'Arcy", base.species(), base.lineage(), base.characterClass(),
                base.background(), base.method(), base.baseScores(), base.backgroundBonus(), base.appearance(), base.biography());
        assertInstanceOf(CharacterFactory.Result.Created.class, factory.create(umlaut, UUID.randomUUID()));
    }
}
