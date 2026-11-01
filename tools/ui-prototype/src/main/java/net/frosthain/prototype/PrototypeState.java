package net.frosthain.prototype;

import net.frosthain.core.ContentId;
import net.frosthain.core.character.Appearance;
import net.frosthain.core.character.CharacterFactory;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.content.AbilityScore;
import net.frosthain.core.content.DialogueDefinition;
import net.frosthain.core.content.SpeciesDefinition;
import net.frosthain.core.dialogue.DialogueSession;
import net.frosthain.core.progression.LevelingRules;
import net.frosthain.core.progression.TalentAllocator;
import net.frosthain.core.quest.QuestLog;
import net.frosthain.core.quest.QuestRules;
import net.frosthain.core.registry.ContentLoader;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.core.registry.DirectoryContentSource;
import net.frosthain.core.rules.AbilityScoreRules;
import net.frosthain.core.rules.ProgressionRules;
import net.frosthain.ui.Translator;
import net.frosthain.ui.dialogue.DialogueView;
import net.frosthain.ui.hud.HudPolicy;
import net.frosthain.ui.talent.Camera;
import net.frosthain.ui.talent.TalentTreeView;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Game state of the prototype. It uses only the real rules from {@code core} – the same calls the mod's
 * server makes – so behaviour in the prototype is behaviour in the game.
 */
final class PrototypeState {

    enum Scene { HUD, TALENTS, DIALOGUE }

    final ContentRegistry registry;
    final Translator translator;
    final TalentAllocator allocator;
    final LevelingRules leveling;
    final QuestRules questRules;

    CharacterSheet sheet;
    QuestLog questLog;
    Scene scene = Scene.HUD;
    boolean trackerCollapsed;
    String notification = "";

    ContentId selectedTree = ContentId.of("general");
    String selectedNode = "combat_stance";
    String activeTab = "";
    boolean confirmReset;
    final Camera camera = new Camera(38, 150, 82);

    DialogueView dialogue;

    PrototypeState(Path contentRoot) {
        DirectoryContentSource.Scan scan = DirectoryContentSource.scanAllPacks(contentRoot);
        ContentLoader.Result result = ContentLoader.load(scan.contentFiles(), scan.langFiles());
        if (result.hasErrors()) {
            throw new IllegalStateException("Inhalte fehlerhaft: " + result.diagnostics());
        }
        registry = result.registry();
        translator = new Translator(registry, "de_de");
        allocator = new TalentAllocator(registry);
        leveling = new LevelingRules(registry);
        questRules = new QuestRules(registry);
        sheet = referenceCharacter();
        questLog = questRules.start(QuestLog.EMPTY, ContentId.of("awakening")).log();
        questLog = questRules.start(questLog, ContentId.of("lost_supplies")).log();
    }

    /** Human fighter at level 12 with 3 free talent points – the situation shown in reference image D. */
    private CharacterSheet referenceCharacter() {
        SpeciesDefinition human = registry.get(ContentType.SPECIES, ContentId.of("human")).orElseThrow();
        Map<AbilityScore, Integer> scores = new EnumMap<>(AbilityScore.class);
        int[] values = {15, 13, 14, 12, 10, 8};
        for (AbilityScore ability : AbilityScore.values()) {
            scores.put(ability, values[ability.ordinal()]);
        }
        CharacterFactory.CreationRequest request = new CharacterFactory.CreationRequest("Arwen", human.id(), Optional.empty(),
                ContentId.of("fighter"), ContentId.of("soldier"), AbilityScoreRules.Method.STANDARD_ARRAY, scores,
                Map.of(AbilityScore.STRENGTH, 2, AbilityScore.CONSTITUTION, 1), Appearance.defaultFor(human),
                CharacterSheet.Biography.EMPTY);
        CharacterSheet created = switch (new CharacterFactory(registry).create(request, UUID.nameUUIDFromBytes(new byte[]{1}))) {
            case CharacterFactory.Result.Created c -> c.sheet();
            case CharacterFactory.Result.Rejected r -> throw new IllegalStateException(r.problems().toString());
        };
        ProgressionRules progression = registry.progression().orElseThrow();
        CharacterSheet sheet = created.withExperience(progression.experienceThresholds().get(11));
        while (sheet.characterLevel() < 12) {
            sheet = leveling.levelUp(sheet, ContentId.of("fighter"));
        }
        sheet = leveling.chooseSubclass(sheet, ContentId.of("fighter/champion"));
        ContentId general = ContentId.of("general");
        for (String node : List.of("attunement", "arcane_bolt", "rune_ward", "spell_focus", "spell_focus", "mana_flow",
                "forager", "forager", "pathfinder", "deep_breath", "climber", "miners_luck")) {
            sheet = allocator.allocate(sheet, new TalentRef(general, node));
        }
        return sheet;
    }

    TalentTreeView talentView() {
        return new TalentTreeView(registry, allocator, translator, sheet, selectedTree);
    }

    List<TalentTreeView.PathEntry> paths() {
        return TalentTreeView.paths(registry, allocator, translator, sheet);
    }

    void selectTree(ContentId tree) {
        selectedTree = tree;
        TalentTreeView view = talentView();
        selectedNode = view.tree().root();
        activeTab = "";
        confirmReset = false;
        double[] b = view.bounds();
        camera.fit(b[0], b[1], b[2], b[3], 0.9);
    }

    void unlockSelected() {
        TalentRef ref = new TalentRef(selectedTree, selectedNode);
        if (allocator.checkAllocate(sheet, ref).isEmpty()) {
            sheet = allocator.allocate(sheet, ref);
            notification = "";
        }
    }

    void refundSelected() {
        TalentRef ref = new TalentRef(selectedTree, selectedNode);
        if (sheet.talents().getOrDefault(ref, 0) > 0 && allocator.checkRefund(sheet, ref).isEmpty()) {
            sheet = allocator.refund(sheet, ref);
        }
    }

    void resetAll() {
        sheet = allocator.resetAll(sheet);
        confirmReset = false;
    }

    /** Test helper of the prototype: one more character level. */
    void gainLevel() {
        ProgressionRules progression = registry.progression().orElseThrow();
        if (sheet.characterLevel() >= progression.maxLevel()) {
            return;
        }
        sheet = sheet.withExperience(Math.max(sheet.experience(),
                progression.experienceThresholds().get(sheet.characterLevel())));
        sheet = leveling.levelUp(sheet, sheet.startingClass());
    }

    void openDialogue() {
        DialogueDefinition eldrin = registry.get(ContentType.DIALOGUES, ContentId.parse("eldrin/awakening")).orElseThrow();
        DialogueSession session = new DialogueSession(eldrin, new DialogueSession.Context() {
            @Override
            public DialogueDefinition.QuestState questState(ContentId quest) {
                if (questLog.isCompleted(quest)) {
                    return DialogueDefinition.QuestState.COMPLETED;
                }
                return questLog.isActive(quest) ? DialogueDefinition.QuestState.ACTIVE : DialogueDefinition.QuestState.NOT_STARTED;
            }

            @Override
            public void apply(DialogueDefinition.Effect effect) {
                applyEffect(effect);
            }
        });
        dialogue = new DialogueView(registry, session, translator);
        scene = Scene.DIALOGUE;
    }

    private void applyEffect(DialogueDefinition.Effect effect) {
        QuestRules.Outcome outcome = switch (effect) {
            case DialogueDefinition.Effect.StartQuest(ContentId quest) -> questRules.start(questLog, quest);
            case DialogueDefinition.Effect.AdvanceObjective(ContentId quest, String objective, int amount) ->
                    questRules.advance(questLog, quest, objective, amount);
            case DialogueDefinition.Effect.GrantExperience(int amount) -> {
                sheet = sheet.withExperience(sheet.experience() + amount);
                yield new QuestRules.Outcome(questLog, List.of());
            }
        };
        questLog = outcome.log();
        if (outcome.experienceGained() > 0) {
            sheet = sheet.withExperience(sheet.experience() + outcome.experienceGained());
        }
    }

    /** Test helper: a supply crate was found (side quest progress). */
    void findCrate() {
        questLog = questRules.advance(questLog, ContentId.of("lost_supplies"), "find_crates", 1).log();
    }

    void closeDialogue() {
        dialogue = null;
        scene = Scene.HUD;
    }

    HudPolicy.Mode hudMode() {
        return switch (scene) {
            case HUD -> HudPolicy.Mode.GAMEPLAY;
            case TALENTS -> HudPolicy.Mode.MENU;
            case DIALOGUE -> HudPolicy.Mode.DIALOGUE;
        };
    }
}
