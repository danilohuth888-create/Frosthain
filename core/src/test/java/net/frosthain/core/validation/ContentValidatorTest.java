package net.frosthain.core.validation;

import net.frosthain.core.TestContent;
import net.frosthain.core.registry.ContentLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentValidatorTest {

    private static List<Diagnostic> errors(TestContent.Pack pack) {
        ContentLoader.Result result = pack.load();
        assertFalse(result.hasErrors(), () -> "Ladefehler im Testpaket: " + result.diagnostics());
        return ContentValidator.validate(result.registry()).stream()
                .filter(d -> d.severity() == Diagnostic.Severity.ERROR)
                .toList();
    }

    private static boolean contains(List<Diagnostic> diagnostics, String fragment) {
        return diagnostics.stream().anyMatch(d -> d.message().contains(fragment));
    }

    /** A tree with a root and the given extra node JSON objects. */
    private static String tree(String... nodes) {
        StringBuilder json = new StringBuilder("""
                {"source": "frosthain:original", "owner": {"type": "general"}, "root": "start",
                 "nodes": [{"key": "start", "kind": "root", "position": [0, 0]}""");
        for (String node : nodes) {
            json.append(", ").append(node);
        }
        return json.append("]}").toString();
    }

    private static TestContent.Pack packWithTree(String treeJson, String... nodeKeys) {
        TestContent.Pack pack = new TestContent.Pack().file("talent_trees/test", treeJson)
                .translate("talent_tree.frosthain.test", "talent.frosthain.test.start", "talent.frosthain.test.start.desc");
        for (String key : nodeKeys) {
            pack.translate("talent.frosthain.test." + key, "talent.frosthain.test." + key + ".desc");
        }
        return pack;
    }

    @Test
    void minimalPackIsValid() {
        assertEquals(List.of(), errors(new TestContent.Pack()));
    }

    @Test
    void contentFromUnreviewedSourceIsBlocked() {
        TestContent.Pack pack = new TestContent.Pack()
                .file("sources/community_pack", """
                        {"title": "Some Homebrew", "publisher": "Unknown", "license": "unknown", "status": "pending_review"}""")
                .file("skills/sailing", """
                        {"source": "frosthain:community_pack", "ability": "dexterity"}""")
                .translate("skill.frosthain.sailing");
        assertTrue(contains(errors(pack), "noch nicht lizenzrechtlich geprüft"));
    }

    @Test
    void rejectedAndUnknownSourcesAreErrors() {
        TestContent.Pack pack = new TestContent.Pack()
                .file("sources/protected_book", """
                        {"title": "Protected", "publisher": "X", "license": "All rights reserved", "status": "rejected"}""")
                .file("skills/a", """
                        {"source": "frosthain:protected_book", "ability": "dexterity"}""")
                .file("skills/b", """
                        {"source": "frosthain:nowhere", "ability": "dexterity"}""")
                .translate("skill.frosthain.a", "skill.frosthain.b");
        List<Diagnostic> errors = errors(pack);
        assertTrue(contains(errors, "abgelehnt"));
        assertTrue(contains(errors, "Unbekannte Quelle frosthain:nowhere"));
    }

    @Test
    void ccByWithoutAttributionIsAnError() {
        TestContent.Pack pack = new TestContent.Pack().file("sources/cc", """
                {"title": "CC", "publisher": "X", "license": "CC-BY-4.0", "license_url": "https://example.org", "status": "approved"}""");
        assertTrue(contains(errors(pack), "Namensnennungstext"));
    }

    @Test
    void detectsUnknownReferences() {
        TestContent.Pack pack = new TestContent.Pack().file("backgrounds/sailor", """
                {"source": "frosthain:original", "ability_options": ["strength", "dexterity", "wisdom"],
                 "skill_proficiencies": ["frosthain:athletics", "frosthain:sailing"]}""")
                .translate("background.frosthain.sailor", "background.frosthain.sailor.desc");
        assertTrue(contains(errors(pack), "skills frosthain:athletics existiert nicht"));
    }

    @Test
    void detectsPrerequisiteCycles() {
        String json = tree(
                """
                {"key": "a", "kind": "passive", "position": [1, 0], "requires": ["start", "c"]}""",
                """
                {"key": "b", "kind": "passive", "position": [2, 0], "requires": ["a"]}""",
                """
                {"key": "c", "kind": "passive", "position": [3, 0], "requires": ["b"]}""");
        List<Diagnostic> errors = errors(packWithTree(json, "a", "b", "c"));
        assertTrue(contains(errors, "Zyklische Voraussetzung"), errors::toString);
        assertTrue(contains(errors, "nicht erreichbare Knoten: [a, b, c]"), errors::toString);
    }

    @Test
    void detectsOrphanNodesAndDuplicatePositions() {
        String json = tree(
                """
                {"key": "orphan", "kind": "passive", "position": [1, 0]}""",
                """
                {"key": "twin", "kind": "passive", "position": [1, 0], "requires": ["start"]}""");
        List<Diagnostic> errors = errors(packWithTree(json, "orphan", "twin"));
        assertTrue(contains(errors, "ohne Voraussetzung"), errors::toString);
        assertTrue(contains(errors, "bereits von"), errors::toString);
    }

    @Test
    void detectsNodesThatNeedTwoMutuallyExclusiveChoices() {
        String json = tree(
                """
                {"key": "left", "kind": "keystone", "position": [-1, 0], "requires": ["start"], "exclusive_group": "k"}""",
                """
                {"key": "right", "kind": "keystone", "position": [1, 0], "requires": ["start"], "exclusive_group": "k"}""",
                """
                {"key": "impossible", "kind": "passive", "position": [0, 1], "requires": ["left", "right"]}""");
        List<Diagnostic> errors = errors(packWithTree(json, "left", "right", "impossible"));
        assertTrue(contains(errors, "nicht erreichbare Knoten: [impossible]"), errors::toString);
    }

    @Test
    void reportsMissingGermanTranslations() {
        TestContent.Pack pack = new TestContent.Pack().file("skills/sailing", """
                {"source": "frosthain:original", "ability": "dexterity"}""");
        assertTrue(contains(errors(pack), "skill.frosthain.sailing"));
    }

    @Test
    void unknownEngineFlagsAreErrors() {
        TestContent.Pack pack = new TestContent.Pack().file("species/ghost", """
                {"source": "frosthain:original", "size": "medium", "traits": [{"type": "flag", "flag": "frosthain:fly"}]}""")
                .translate("species.frosthain.ghost", "species.frosthain.ghost.desc");
        assertTrue(contains(errors(pack), "Unbekannte Engine-Funktion frosthain:fly"));
    }

    @Test
    void storyReferencesAreChecked() {
        TestContent.Pack pack = new TestContent.Pack()
                .file("npcs/mira", """
                        {"source": "frosthain:original", "dialogue": "frosthain:mira/hello"}""")
                .file("quests/errand", """
                        {"source": "frosthain:original", "kind": "npc",
                         "objectives": [{"key": "talk", "type": "talk", "target": "frosthain:nobody"}]}""")
                .file("dialogues/mira/hello", """
                        {"source": "frosthain:original", "speaker": "frosthain:mira", "start": "hi",
                         "nodes": [{"key": "hi", "choices": [{"key": "ok", "effects": [
                            {"type": "advance_objective", "quest": "frosthain:errand", "objective": "missing"}]}]},
                                   {"key": "orphan"}]}""")
                .translate("npc.frosthain.mira", "npc.frosthain.mira.title", "quest.frosthain.errand",
                        "quest.frosthain.errand.desc", "quest.frosthain.errand.objective.talk",
                        "dialogue.frosthain.mira.hello.hi", "dialogue.frosthain.mira.hello.hi.choice.ok",
                        "dialogue.frosthain.mira.hello.orphan");
        ContentLoader.Result result = pack.load();
        assertFalse(result.hasErrors(), result.diagnostics()::toString);
        List<Diagnostic> all = ContentValidator.validate(result.registry());
        assertTrue(contains(all, "npcs frosthain:nobody existiert nicht"), all::toString);
        assertTrue(contains(all, "hat kein Ziel 'missing'"), all::toString);
        assertTrue(contains(all, "Nicht erreichbare Dialogknoten: [orphan]"), all::toString);
    }

    @Test
    void dialogueLinksToUnknownNodesAreRejectedWhileLoading() {
        ContentLoader.Result result = new TestContent.Pack()
                .file("dialogues/broken", """
                        {"source": "frosthain:original", "speaker": "frosthain:x", "start": "a",
                         "nodes": [{"key": "a", "choices": [{"key": "go", "next": "nowhere"}]}]}""")
                .load();
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.message().contains("unbekannten Knoten 'nowhere'")),
                result.diagnostics()::toString);
    }
}
