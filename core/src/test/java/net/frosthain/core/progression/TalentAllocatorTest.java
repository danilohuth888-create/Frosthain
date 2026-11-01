package net.frosthain.core.progression;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.Characters;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.rules.ProgressionRules;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentAllocatorTest {

    private static final ContentId GENERAL = ContentId.of("general");
    private static final ContentId FIGHTER_TREE = ContentId.of("fighter");

    private final TalentAllocator talents = new TalentAllocator(TestContent.baseRegistry());
    private final LevelingRules leveling = new LevelingRules(TestContent.baseRegistry());
    private final ProgressionRules rules = TestContent.baseRegistry().progression().orElseThrow();

    private static TalentRef general(String node) {
        return new TalentRef(GENERAL, node);
    }

    private static TalentRef fighter(String node) {
        return new TalentRef(FIGHTER_TREE, node);
    }

    private static boolean has(List<Blocker> blockers, Blocker.Reason reason) {
        return blockers.stream().anyMatch(b -> b.reason() == reason);
    }

    private CharacterSheet fighterAtLevel(int level) {
        CharacterSheet sheet = Characters.humanFighter().withExperience(rules.experienceThresholds().get(level - 1));
        while (sheet.characterLevel() < level) {
            sheet = leveling.levelUp(sheet, ContentId.of("fighter"));
        }
        return sheet;
    }

    private CharacterSheet allocateAll(CharacterSheet sheet, TalentRef... refs) {
        CharacterSheet current = sheet;
        for (TalentRef ref : refs) {
            current = talents.allocate(current, ref);
        }
        return current;
    }

    @Test
    void rootIsFreeAndAlwaysUnlocked() {
        CharacterSheet sheet = Characters.humanFighter();
        assertEquals(1, talents.rank(sheet, general("awakening")));
        assertEquals(1, talents.rank(sheet, fighter("weapon_master")));
        assertTrue(has(talents.checkAllocate(sheet, general("awakening")), Blocker.Reason.ROOT_NODE));
    }

    @Test
    void spendsPointsAndExplainsMissingPoints() {
        CharacterSheet sheet = Characters.humanFighter();
        assertEquals(1, talents.availablePoints(sheet));
        CharacterSheet after = talents.allocate(sheet, general("combat_stance"));
        assertEquals(0, talents.availablePoints(after));
        List<Blocker> blockers = talents.checkAllocate(after, general("toughness"));
        assertEquals(List.of(Blocker.of(Blocker.Reason.NOT_ENOUGH_POINTS, 1)), blockers);
        assertThrows(IllegalStateException.class, () -> talents.allocate(after, general("toughness")));
    }

    @Test
    void explainsAllReasonsForALockedNode() {
        List<Blocker> blockers = talents.checkAllocate(Characters.humanFighter(), general("stalwart"));
        assertTrue(blockers.contains(Blocker.of(Blocker.Reason.CHARACTER_LEVEL_TOO_LOW, 5)), blockers::toString);
        assertTrue(blockers.contains(Blocker.of(Blocker.Reason.MISSING_PREREQUISITE, "weapon_training")), blockers::toString);
    }

    @Test
    void respectsMaxRank() {
        CharacterSheet sheet = allocateAll(fighterAtLevel(4), general("combat_stance"),
                general("toughness"), general("toughness"), general("toughness"));
        assertTrue(has(talents.checkAllocate(sheet, general("toughness")), Blocker.Reason.MAX_RANK_REACHED));
    }

    @Test
    void requiresAnyNeedsOneOfSeveralPaths() {
        CharacterSheet sheet = allocateAll(fighterAtLevel(5), general("attunement"));
        assertTrue(has(talents.checkAllocate(sheet, general("spell_focus")), Blocker.Reason.MISSING_ANY_PREREQUISITE));
        CharacterSheet withPath = talents.allocate(sheet, general("arcane_bolt"));
        assertEquals(List.of(), talents.checkAllocate(withPath, general("spell_focus")));
    }

    @Test
    void keystonesAreMutuallyExclusive() {
        CharacterSheet sheet = allocateAll(fighterAtLevel(10), general("combat_stance"),
                general("toughness"), general("weapon_training"), general("stalwart"), general("bloodlust"),
                general("attunement"), general("arcane_bolt"), general("spell_focus"));
        List<Blocker> overload = talents.checkAllocate(sheet, general("arcane_overload"));
        assertEquals(List.of(Blocker.of(Blocker.Reason.EXCLUSIVE_CONFLICT, "bloodlust")), overload);
    }

    @Test
    void refundIsBlockedWhileOtherTalentsDependOnIt() {
        CharacterSheet sheet = allocateAll(fighterAtLevel(4), general("combat_stance"), general("toughness"), general("weapon_training"));
        List<Blocker> blockers = talents.checkRefund(sheet, general("toughness"));
        assertTrue(has(blockers, Blocker.Reason.REQUIRED_BY_OTHER_TALENT), blockers::toString);

        CharacterSheet twoRanks = talents.allocate(sheet, general("toughness"));
        CharacterSheet refunded = talents.refund(twoRanks, general("toughness"));
        assertEquals(1, refunded.talents().get(general("toughness")));

        CharacterSheet leafRefunded = talents.refund(sheet, general("weapon_training"));
        assertEquals(Map.of(general("combat_stance"), 1, general("toughness"), 1), leafRefunded.talents());
        assertTrue(has(talents.checkRefund(leafRefunded, general("weapon_training")), Blocker.Reason.NOT_ALLOCATED));
    }

    @Test
    void fullResetReturnsAllPoints() {
        CharacterSheet sheet = allocateAll(fighterAtLevel(4), general("combat_stance"), general("toughness"), fighter("weapon_focus"));
        assertEquals(2, talents.availablePoints(sheet));
        CharacterSheet reset = talents.resetAll(sheet);
        assertEquals(5, talents.availablePoints(reset));
        CharacterSheet treeReset = talents.resetTree(sheet, FIGHTER_TREE);
        assertEquals(3, talents.availablePoints(treeReset));
    }

    @Test
    void classTreesNeedTheClassAndClassLevels() {
        CharacterSheet sheet = fighterAtLevel(3);
        assertEquals(List.of(), talents.checkAllocate(talents.allocate(sheet, fighter("weapon_focus")), fighter("swift_strikes")));

        // Fighter 1 / Rogue 2 (character level 3): fighter tree accessible, but class level 3 nodes are not.
        Map<ContentId, Integer> multiclass = new LinkedHashMap<>();
        multiclass.put(ContentId.of("fighter"), 1);
        multiclass.put(ContentId.of("rogue"), 2);
        CharacterSheet mixed = talents.allocate(sheet.withClassLevels(multiclass), fighter("weapon_focus"));
        assertTrue(talents.checkAllocate(mixed, fighter("swift_strikes")).contains(Blocker.of(Blocker.Reason.CLASS_LEVEL_TOO_LOW, 3)));

        // A wizard has no access to the fighter tree at all.
        CharacterSheet wizard = Characters.create(Characters.request("human", Optional.empty(), "wizard"));
        assertTrue(has(talents.checkAllocate(wizard, fighter("weapon_focus")), Blocker.Reason.TREE_NOT_ACCESSIBLE));
    }

    @Test
    void sanitizeRemovesAllocationsThatBecameInvalid() {
        CharacterSheet valid = allocateAll(fighterAtLevel(4), general("combat_stance"), general("toughness"), general("weapon_training"));
        Map<TalentRef, Integer> broken = new LinkedHashMap<>(valid.talents());
        // A content update removed one node and the save lost the prerequisite of another.
        broken.put(general("removed_in_update"), 1);
        broken.remove(general("combat_stance"));
        CharacterSheet damaged = valid.withTalents(broken);

        TalentAllocator.Sanitized result = talents.sanitize(damaged);
        assertEquals(List.of(), talents.findViolations(result.sheet()));
        assertTrue(result.removed().contains(general("removed_in_update")));
        // toughness lost its prerequisite, weapon_training then lost toughness: both are removed.
        assertTrue(result.removed().contains(general("toughness")));
        assertTrue(result.removed().contains(general("weapon_training")));
        assertEquals(Map.of(), result.sheet().talents());
    }

    @Test
    void sanitizeHandlesOverspentBudget() {
        CharacterSheet sheet = allocateAll(fighterAtLevel(4), general("combat_stance"), general("toughness"), general("weapon_training"));
        // Simulate a lower level (e.g. rule change): level 2 only grants 2 points but 3 are spent.
        Map<ContentId, Integer> lower = Map.of(ContentId.of("fighter"), 2);
        TalentAllocator.Sanitized result = talents.sanitize(sheet.withClassLevels(lower));
        assertEquals(List.of(), talents.findViolations(result.sheet()));
        assertTrue(talents.availablePoints(result.sheet()) >= 0);
        assertEquals(List.of(general("weapon_training")), result.removed());
    }
}
