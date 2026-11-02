package net.frosthain.core.rune;

import net.frosthain.core.ContentId;
import net.frosthain.core.TestContent;
import net.frosthain.core.content.RuneDefinition;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.registry.ContentLoader;
import net.frosthain.core.validation.ContentValidator;
import net.frosthain.core.validation.Diagnostic;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuneRulesTest {

    private static final ContentId FIRE = ContentId.of("fire");
    private static final ContentId HEALING = ContentId.of("healing");

    private final RuneRules rules = new RuneRules(TestContent.baseRegistry());

    private static QuestLog completed(String... quests) {
        Map<ContentId, QuestLog.Progress> map = new LinkedHashMap<>();
        for (String quest : quests) {
            map.put(ContentId.of(quest), new QuestLog.Progress(QuestLog.Status.COMPLETED, Map.of()));
        }
        return new QuestLog(map, List.of());
    }

    @Test
    void altarListsFiveRunesInEffectOrder() {
        assertEquals(List.of("fire", "frost", "ward", "healing", "swiftness"),
                rules.all().stream().map(rune -> rune.id().path()).toList());
    }

    @Test
    void questsUnlockRunes() {
        assertEquals(List.of(), rules.known(QuestLog.EMPTY));
        assertEquals(List.of("fire", "frost", "ward"),
                rules.known(completed("first_rune")).stream().map(rune -> rune.id().path()).toList());
        assertEquals(5, rules.known(completed("first_rune", "frozen_crypt")).size());
    }

    @Test
    void missingMaterialsAreCountedPerItem() {
        RuneDefinition fire = rules.all().getFirst();
        assertEquals(Map.of("minecraft:amethyst_shard", 1, "minecraft:coal", 3), rules.missing(fire, Map.of()));
        assertEquals(Map.of("minecraft:coal", 1), rules.missing(fire, Map.of("minecraft:amethyst_shard", 2, "minecraft:coal", 2)));
        assertEquals(Map.of(), rules.missing(fire, Map.of("minecraft:amethyst_shard", 1, "minecraft:coal", 3)));
    }

    @Test
    void serverCheckNamesTheProblem() {
        Map<String, Integer> plenty = Map.of("minecraft:amethyst_shard", 9, "minecraft:coal", 9, "minecraft:sweet_berries", 9);
        assertEquals(Optional.of(RuneRules.Problem.UNKNOWN_RUNE), rules.check(ContentId.of("nope"), completed("first_rune"), plenty));
        assertEquals(Optional.of(RuneRules.Problem.NOT_KNOWN), rules.check(HEALING, completed("first_rune"), plenty));
        assertEquals(Optional.of(RuneRules.Problem.MISSING_MATERIALS), rules.check(FIRE, completed("first_rune"), Map.of()));
        assertEquals(Optional.empty(), rules.check(FIRE, completed("first_rune"), plenty));
    }

    @Test
    void validatorChecksUnlockQuestAndMaterials() {
        ContentLoader.Result result = new TestContent.Pack()
                .file("runes/odd", """
                        {"source": "frosthain:original", "effect": "ward", "unlock_quest": "frosthain:missing",
                         "materials": [{"item": "minecraft:iron_ingot", "count": 2}]}""")
                .translate("rune.frosthain.odd", "rune.frosthain.odd.desc")
                .load();
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics()::toString);
        List<Diagnostic> errors = ContentValidator.validate(result.registry()).stream()
                .filter(d -> d.severity() == Diagnostic.Severity.ERROR).toList();
        assertEquals(1, errors.size(), errors::toString);
        assertTrue(errors.getFirst().toString().contains("unlock_quest"), errors::toString);

        ContentLoader.Result duplicate = new TestContent.Pack()
                .file("runes/twice", """
                        {"source": "frosthain:original", "effect": "heal",
                         "materials": [{"item": "minecraft:apple"}, {"item": "minecraft:apple", "count": 2}]}""")
                .load();
        assertTrue(duplicate.hasErrors());
    }
}
