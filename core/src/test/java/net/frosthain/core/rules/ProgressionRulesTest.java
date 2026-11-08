package net.frosthain.core.rules;

import net.frosthain.core.TestContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressionRulesTest {

    private final ProgressionRules rules = TestContent.baseRegistry().progression().orElseThrow();

    @Test
    void hostileCreaturesGiveExperienceByHealth() {
        assertEquals(50, rules.experienceForDefeat(20, true), "Zombie: 20 Leben × 2,5");
        assertEquals(500, rules.experienceForDefeat(200, true), "Obergrenze 500 (z. B. Enderdrache)");
    }

    @Test
    void passiveAnimalsGiveNothing() {
        assertEquals(0, rules.experienceForDefeat(10, false));
    }

    @Test
    void sixZombiesReachLevelTwo() {
        assertEquals(2, rules.levelForExperience(6L * rules.experienceForDefeat(20, true)));
    }
}
