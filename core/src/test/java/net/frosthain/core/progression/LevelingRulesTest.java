package net.frosthain.core.progression;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.Characters;
import net.frosthain.core.rules.ProgressionRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelingRulesTest {

    private final LevelingRules leveling = new LevelingRules(TestContent.baseRegistry());
    private final ProgressionRules rules = TestContent.baseRegistry().progression().orElseThrow();

    private static boolean has(List<Blocker> blockers, Blocker.Reason reason) {
        return blockers.stream().anyMatch(b -> b.reason() == reason);
    }

    /** Levels the sheet up to {@code level} in its starting class. */
    private CharacterSheet levelTo(CharacterSheet sheet, int level) {
        CharacterSheet current = sheet.withExperience(rules.experienceThresholds().get(level - 1));
        while (current.characterLevel() < level) {
            current = leveling.levelUp(current, current.startingClass());
        }
        return current;
    }

    @Test
    void experienceCurveMatchesFormula() {
        assertEquals(1, rules.levelForExperience(0));
        assertEquals(1, rules.levelForExperience(299));
        assertEquals(2, rules.levelForExperience(300));
        assertEquals(3, rules.levelForExperience(900));
        assertEquals(20, rules.levelForExperience(57_000));
        assertEquals(20, rules.levelForExperience(10_000_000));
        assertEquals(600, rules.experienceToNextLevel(300));
        assertEquals(0, rules.experienceToNextLevel(57_000));
        assertThrows(IllegalArgumentException.class, () -> rules.levelForExperience(-1));
    }

    @Test
    void talentPointsIncludeBonusLevels() {
        assertEquals(1, rules.talentPointsAtLevel(1));
        assertEquals(5, rules.talentPointsAtLevel(4));
        assertEquals(25, rules.talentPointsAtLevel(20));
    }

    @Test
    void experienceOnlyMakesLevelUpsAvailable() {
        CharacterSheet sheet = Characters.humanFighter().withExperience(900);
        assertEquals(2, leveling.pendingLevelUps(sheet));
        CharacterSheet levelled = leveling.levelUp(sheet, ContentId.of("fighter"));
        assertEquals(2, levelled.characterLevel());
        assertEquals(1, leveling.pendingLevelUps(levelled));
        assertTrue(has(leveling.checkLevelUp(Characters.humanFighter(), ContentId.of("fighter")), Blocker.Reason.NO_LEVEL_UP_AVAILABLE));
    }

    @Test
    void multiclassNeedsMinimumLevelAndAbilityScores() {
        CharacterSheet levelOne = Characters.humanFighter().withExperience(300);
        assertTrue(has(leveling.checkLevelUp(levelOne, ContentId.of("rogue")), Blocker.Reason.LEVEL_TOO_LOW_FOR_MULTICLASS));

        CharacterSheet levelThree = levelTo(Characters.humanFighter(), 3).withExperience(rules.experienceThresholds().get(3));
        // DEX 13 meets the rogue requirement, INT 8 does not meet the wizard requirement.
        assertEquals(List.of(), leveling.checkLevelUp(levelThree, ContentId.of("rogue")));
        List<Blocker> wizard = leveling.checkLevelUp(levelThree, ContentId.of("wizard"));
        assertTrue(wizard.contains(Blocker.of(Blocker.Reason.ABILITY_REQUIREMENT, ContentId.of("wizard"))), wizard::toString);
        // Paladin needs STR 13 AND CHA 13; CHA is 10.
        assertTrue(has(leveling.checkLevelUp(levelThree, ContentId.of("paladin")), Blocker.Reason.ABILITY_REQUIREMENT));
    }

    @Test
    void numberOfClassesIsLimited() {
        CharacterSheet sheet = levelTo(Characters.humanFighter(), 3).withExperience(rules.experienceThresholds().get(5));
        sheet = leveling.levelUp(sheet, ContentId.of("rogue"));
        sheet = leveling.levelUp(sheet, ContentId.of("barbarian"));
        assertEquals(3, sheet.classLevels().size());
        CharacterSheet finalSheet = sheet.withExperience(rules.experienceThresholds().get(6));
        assertTrue(has(leveling.checkLevelUp(finalSheet, ContentId.of("monk")), Blocker.Reason.MAX_CLASSES_REACHED));
        assertEquals(List.of(), leveling.checkLevelUp(finalSheet, ContentId.of("fighter")));
    }

    @Test
    void combinedCasterLevelPreventsStackingCasterResources() {
        CharacterSheet sheet = Characters.humanFighter().withClassLevels(new java.util.LinkedHashMap<>(java.util.Map.of(
                ContentId.of("wizard"), 5)));
        assertEquals(5, leveling.combinedCasterLevel(sheet));
        java.util.Map<ContentId, Integer> mixed = new java.util.LinkedHashMap<>();
        mixed.put(ContentId.of("wizard"), 4);
        mixed.put(ContentId.of("paladin"), 5);
        mixed.put(ContentId.of("fighter"), 3);
        // 4 (full) + 5/2 = 2 (half) + 0 (fighter) = 6
        assertEquals(6, leveling.combinedCasterLevel(sheet.withClassLevels(mixed)));
    }

    @Test
    void subclassIsChosenAtClassLevelThree() {
        CharacterSheet levelTwo = levelTo(Characters.humanFighter(), 2);
        assertFalse(leveling.needsSubclassChoice(levelTwo, ContentId.of("fighter")));
        assertThrows(IllegalStateException.class, () -> leveling.chooseSubclass(levelTwo, ContentId.of("fighter/champion")));

        CharacterSheet levelThree = levelTo(Characters.humanFighter(), 3);
        assertTrue(leveling.needsSubclassChoice(levelThree, ContentId.of("fighter")));
        CharacterSheet champion = leveling.chooseSubclass(levelThree, ContentId.of("fighter/champion"));
        assertEquals(Optional.of(ContentId.of("fighter/champion")), Optional.ofNullable(champion.subclasses().get(ContentId.of("fighter"))));
        assertFalse(leveling.needsSubclassChoice(champion, ContentId.of("fighter")));
        // A wizard subclass cannot be chosen by a fighter.
        assertThrows(IllegalStateException.class, () -> leveling.chooseSubclass(levelThree, ContentId.of("wizard/evoker")));
    }
}
