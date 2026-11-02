package net.frosthain.core.power;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.Characters;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.progression.LevelingRules;
import net.frosthain.core.progression.TalentAllocator;
import net.frosthain.core.registry.ContentRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerRulesTest {

    private static final ContentId RUNE_STRIKE = ContentId.of("rune_strike");
    private static final ContentId SECOND_WIND = ContentId.of("second_wind");

    private final ContentRegistry registry = TestContent.baseRegistry();
    private final PowerRules rules = new PowerRules(registry);

    private static CharacterSheet runeMage() {
        return Characters.create(Characters.request("human", Optional.empty(), "rune_mage"));
    }

    @Test
    void classesStartWithTheirPowersAndResource() {
        CharacterSheet mage = runeMage();
        assertEquals(List.of(RUNE_STRIKE), rules.knownPowers(mage));
        assertEquals(ContentId.of("spell_power"), rules.resource(mage).orElseThrow().id());
        assertEquals(100, rules.maximum(mage));
        assertEquals(List.of(SECOND_WIND), rules.knownPowers(Characters.humanFighter()));
    }

    @Test
    void usingAPowerCostsResourceAndStartsTheCooldown() {
        CharacterSheet mage = runeMage();
        PowerRules.State state = rules.fresh(mage);
        assertEquals(List.of(), rules.check(state, mage, RUNE_STRIKE));
        state = rules.use(state, mage, RUNE_STRIKE);
        assertEquals(60, state.resource(), 1e-9);
        assertEquals(160, state.cooldown(RUNE_STRIKE));
        assertEquals(List.of(PowerRules.Problem.ON_COOLDOWN), rules.check(state, mage, RUNE_STRIKE));
    }

    @Test
    void cooldownRunsDownAndResourceRegeneratesOutOfCombatOnly() {
        CharacterSheet mage = runeMage();
        PowerRules.State used = rules.use(rules.fresh(mage), mage, RUNE_STRIKE);
        PowerRules.State inCombat = rules.tick(used, mage, 160, true);
        assertEquals(0, inCombat.cooldown(RUNE_STRIKE));
        assertEquals(60, inCombat.resource(), 1e-9, "Zauberkraft regeneriert nicht im Kampf");
        PowerRules.State calm = rules.tick(used, mage, 200, false);
        assertEquals(70, calm.resource(), 1e-9, "1 Zauberkraft pro Sekunde");
    }

    @Test
    void notEnoughResourceAndUnknownPowersAreReported() {
        CharacterSheet mage = runeMage();
        PowerRules.State empty = new PowerRules.State(10, java.util.Map.of());
        assertEquals(List.of(PowerRules.Problem.NOT_ENOUGH_RESOURCE), rules.check(empty, mage, RUNE_STRIKE));
        assertTrue(rules.check(rules.fresh(mage), mage, SECOND_WIND).contains(PowerRules.Problem.NOT_KNOWN));
        assertEquals(List.of(PowerRules.Problem.UNKNOWN_POWER), rules.check(rules.fresh(mage), mage, ContentId.of("fireball")));
    }

    @Test
    void talentsGrantPowersAndRaiseTheResourceMaximum() {
        CharacterSheet mage = runeMage().withExperience(3_000);
        LevelingRules leveling = new LevelingRules(registry);
        while (leveling.pendingLevelUps(mage) > 0) {
            mage = leveling.levelUp(mage, ContentId.of("rune_mage"));
        }
        TalentAllocator allocator = new TalentAllocator(registry);
        ContentId general = ContentId.of("general");
        for (String node : List.of("attunement", "arcane_bolt", "spell_focus")) {
            TalentRef ref = new TalentRef(general, node);
            assertEquals(List.of(), allocator.checkAllocate(mage, ref), node);
            mage = allocator.allocate(mage, ref);
        }
        assertEquals(List.of(RUNE_STRIKE, ContentId.of("arcane_bolt")), rules.knownPowers(mage));
        assertTrue(rules.maximum(mage) > 100, "Zauberfokus erhöht die maximale Zauberkraft");
    }
}
