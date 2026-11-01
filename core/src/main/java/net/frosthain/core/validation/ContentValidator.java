package net.frosthain.core.validation;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.AbilityScore;
import net.frosthain.core.content.BackgroundDefinition;
import net.frosthain.core.content.ClassDefinition;
import net.frosthain.core.content.ContentDefinition;
import net.frosthain.core.content.DialogueDefinition;
import net.frosthain.core.content.NpcDefinition;
import net.frosthain.core.content.QuestDefinition;
import net.frosthain.core.content.EngineFlags;
import net.frosthain.core.content.Modifier;
import net.frosthain.core.content.PowerDefinition;
import net.frosthain.core.content.SourceDefinition;
import net.frosthain.core.content.SpeciesDefinition;
import net.frosthain.core.content.SubclassDefinition;
import net.frosthain.core.content.TalentTreeDefinition;
import net.frosthain.core.content.TalentTreeDefinition.TalentNode;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Cross-file validation of a loaded {@link ContentRegistry}: license gate, references, talent tree
 * structure and translations. Runs in CI ({@code ./gradlew check}) and in the mod after every reload.
 */
public final class ContentValidator {

    public static final String PRIMARY_LOCALE = "de_de";
    public static final List<String> SECONDARY_LOCALES = List.of("en_us");
    private static final int MAX_GRID = 50;

    private final ContentRegistry registry;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final Map<ContentId, Set<String>> plannedFlagUsers = new TreeMap<>();

    private ContentValidator(ContentRegistry registry) {
        this.registry = registry;
    }

    public static List<Diagnostic> validate(ContentRegistry registry) {
        ContentValidator validator = new ContentValidator(registry);
        validator.run();
        List<Diagnostic> result = new ArrayList<>(validator.diagnostics);
        result.sort(Diagnostic.ORDER);
        return List.copyOf(result);
    }

    private void run() {
        checkRules();
        for (ContentType<?> type : ContentType.ALL) {
            for (ContentDefinition definition : registry.all(type).values()) {
                checkSource(type, definition);
            }
        }
        checkSourcesAttribution();
        registry.all(ContentType.SPECIES).values().forEach(this::checkSpecies);
        registry.all(ContentType.CLASSES).values().forEach(this::checkClass);
        registry.all(ContentType.SUBCLASSES).values().forEach(this::checkSubclass);
        registry.all(ContentType.BACKGROUNDS).values().forEach(this::checkBackground);
        registry.all(ContentType.POWERS).values().forEach(this::checkPower);
        registry.all(ContentType.TALENT_TREES).values().forEach(this::checkTalentTree);
        registry.all(ContentType.NPCS).values().forEach(this::checkNpc);
        registry.all(ContentType.QUESTS).values().forEach(this::checkQuest);
        registry.all(ContentType.DIALOGUES).values().forEach(this::checkDialogue);
        checkTranslations();
        plannedFlagUsers.forEach((flag, users) -> diagnostics.add(Diagnostic.info("flag " + flag,
                "Engine-Funktion ist spezifiziert, aber in der Mod noch nicht umgesetzt; genutzt von " + users)));
    }

    // ---- rules & licensing ----------------------------------------------------------------------------

    private void checkRules() {
        if (registry.progression().isEmpty()) {
            diagnostics.add(Diagnostic.error("rules/progression", "Fortschrittsregeln fehlen"));
        }
        if (registry.abilityScoreRules().isEmpty()) {
            diagnostics.add(Diagnostic.error("rules/ability_scores", "Attributsregeln fehlen"));
        }
    }

    private void checkSource(ContentType<?> type, ContentDefinition definition) {
        if (type == ContentType.SOURCES) {
            return;
        }
        String location = location(type, definition.id());
        var source = registry.get(ContentType.SOURCES, definition.source());
        if (source.isEmpty()) {
            diagnostics.add(Diagnostic.error(location, "Unbekannte Quelle " + definition.source() + " (Lizenzkatalog)"));
            return;
        }
        switch (source.get().status()) {
            case APPROVED -> {
                // permitted
            }
            case PENDING_REVIEW -> diagnostics.add(Diagnostic.error(location,
                    "Quelle " + definition.source() + " ist noch nicht lizenzrechtlich geprüft – Inhalt ist gesperrt"));
            case REJECTED -> diagnostics.add(Diagnostic.error(location,
                    "Quelle " + definition.source() + " ist abgelehnt – Inhalt darf nicht verwendet werden"));
        }
    }

    private void checkSourcesAttribution() {
        for (SourceDefinition source : registry.all(ContentType.SOURCES).values()) {
            boolean requiresAttribution = source.license().toUpperCase(Locale.ROOT).startsWith("CC-BY");
            if (requiresAttribution && source.attribution().isEmpty()) {
                diagnostics.add(Diagnostic.error(location(ContentType.SOURCES, source.id()),
                        "Lizenz " + source.license() + " verlangt einen Namensnennungstext (attribution)"));
            }
            if (source.status() == SourceDefinition.Status.APPROVED && source.licenseUrl().isEmpty()
                    && !source.license().startsWith("FROSTHAIN")) {
                diagnostics.add(Diagnostic.warning(location(ContentType.SOURCES, source.id()), "license_url fehlt"));
            }
        }
    }

    // ---- per type -------------------------------------------------------------------------------------

    private void checkSpecies(SpeciesDefinition species) {
        String location = location(ContentType.SPECIES, species.id());
        checkModifiers(location, species.traits());
        for (SpeciesDefinition.Lineage lineage : species.lineages()) {
            checkModifiers(location + " lineage " + lineage.key(), lineage.traits());
        }
    }

    private void checkClass(ClassDefinition clazz) {
        String location = location(ContentType.CLASSES, clazz.id());
        clazz.resource().ifPresent(resource -> requireRef(location, "resource", ContentType.RESOURCES, resource));
        clazz.startingPowers().forEach(power -> requireRef(location, "starting_powers", ContentType.POWERS, power));
        clazz.skillChoices().from().forEach(skill -> requireRef(location, "skill_choices", ContentType.SKILLS, skill));
        clazz.exclusiveWith().forEach(other -> {
            requireRef(location, "exclusive_with", ContentType.CLASSES, other);
            if (other.equals(clazz.id())) {
                diagnostics.add(Diagnostic.error(location, "exclusive_with enthält die Klasse selbst"));
            }
        });
        if (clazz.multiclassRequirements().isEmpty()) {
            diagnostics.add(Diagnostic.warning(location, "keine Multiclass-Voraussetzungen definiert"));
        }
        if (clazz.casterProgression() != ClassDefinition.CasterProgression.NONE && clazz.resource().isEmpty()) {
            diagnostics.add(Diagnostic.error(location, "Zauberwirkende Klasse ohne Ressource"));
        }
        boolean hasSubclass = registry.all(ContentType.SUBCLASSES).values().stream()
                .anyMatch(sub -> sub.parentClass().equals(clazz.id()));
        if (!hasSubclass) {
            diagnostics.add(Diagnostic.warning(location, "keine Unterklasse vorhanden"));
        }
        if (clazz.talentTree().isEmpty()) {
            diagnostics.add(Diagnostic.warning(location, "noch kein Talentbaum zugeordnet"));
        } else {
            ContentId treeId = clazz.talentTree().get();
            registry.get(ContentType.TALENT_TREES, treeId).ifPresentOrElse(tree -> {
                if (!(tree.owner() instanceof TalentTreeDefinition.Owner.ForClass(ContentId owner) && owner.equals(clazz.id()))) {
                    diagnostics.add(Diagnostic.error(location, "Talentbaum " + treeId + " gehört nicht zu dieser Klasse"));
                }
            }, () -> missing(location, "talent_tree", ContentType.TALENT_TREES, treeId));
        }
    }

    private void checkSubclass(SubclassDefinition subclass) {
        String location = location(ContentType.SUBCLASSES, subclass.id());
        requireRef(location, "class", ContentType.CLASSES, subclass.parentClass());
        subclass.talentTree().ifPresent(treeId -> registry.get(ContentType.TALENT_TREES, treeId).ifPresentOrElse(tree -> {
            if (!(tree.owner() instanceof TalentTreeDefinition.Owner.ForSubclass(ContentId owner) && owner.equals(subclass.id()))) {
                diagnostics.add(Diagnostic.error(location, "Talentbaum " + treeId + " gehört nicht zu dieser Unterklasse"));
            }
        }, () -> missing(location, "talent_tree", ContentType.TALENT_TREES, treeId)));
    }

    private void checkBackground(BackgroundDefinition background) {
        String location = location(ContentType.BACKGROUNDS, background.id());
        background.skillProficiencies().forEach(skill -> requireRef(location, "skill_proficiencies", ContentType.SKILLS, skill));
    }

    private void checkPower(PowerDefinition power) {
        String location = location(ContentType.POWERS, power.id());
        power.resource().ifPresent(resource -> requireRef(location, "resource", ContentType.RESOURCES, resource));
    }

    // ---- story: npcs, quests, dialogues ---------------------------------------------------------------

    private void checkNpc(NpcDefinition npc) {
        String location = location(ContentType.NPCS, npc.id());
        npc.dialogue().ifPresent(dialogueId -> registry.get(ContentType.DIALOGUES, dialogueId).ifPresentOrElse(dialogue -> {
            if (!dialogue.speaker().equals(npc.id())) {
                diagnostics.add(Diagnostic.error(location, "Dialog " + dialogueId + " hat einen anderen Sprecher (" + dialogue.speaker() + ")"));
            }
        }, () -> missing(location, "dialogue", ContentType.DIALOGUES, dialogueId)));
    }

    private void checkQuest(QuestDefinition quest) {
        String location = location(ContentType.QUESTS, quest.id());
        for (QuestDefinition.Objective objective : quest.objectives()) {
            if (objective.type() == QuestDefinition.ObjectiveType.TALK) {
                requireRef(location + " objective " + objective.key(), "target", ContentType.NPCS, ContentId.parse(objective.target()));
            }
        }
        for (ContentId followUp : quest.followUps()) {
            requireRef(location, "follow_ups", ContentType.QUESTS, followUp);
            if (followUp.equals(quest.id())) {
                diagnostics.add(Diagnostic.error(location, "Quest ist ihre eigene Folgequest"));
            }
        }
    }

    private void checkDialogue(DialogueDefinition dialogue) {
        String location = location(ContentType.DIALOGUES, dialogue.id());
        requireRef(location, "speaker", ContentType.NPCS, dialogue.speaker());
        Set<String> reachable = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>(List.of(dialogue.start()));
        while (!queue.isEmpty()) {
            String key = queue.poll();
            if (!reachable.add(key)) {
                continue;
            }
            for (DialogueDefinition.Choice choice : dialogue.nodes().get(key).choices()) {
                choice.next().ifPresent(queue::add);
            }
        }
        Set<String> unreachable = new TreeSet<>(dialogue.nodes().keySet());
        unreachable.removeAll(reachable);
        if (!unreachable.isEmpty()) {
            diagnostics.add(Diagnostic.warning(location, "Nicht erreichbare Dialogknoten: " + unreachable));
        }
        for (DialogueDefinition.Node node : dialogue.nodes().values()) {
            for (DialogueDefinition.Choice choice : node.choices()) {
                String choiceLocation = location + " " + node.key() + "." + choice.key();
                for (DialogueDefinition.Condition condition : choice.conditions()) {
                    switch (condition) {
                        case DialogueDefinition.Condition.QuestStateIs(ContentId quest, DialogueDefinition.QuestState state) ->
                                requireRef(choiceLocation, "conditions", ContentType.QUESTS, quest);
                    }
                }
                for (DialogueDefinition.Effect effect : choice.effects()) {
                    switch (effect) {
                        case DialogueDefinition.Effect.StartQuest(ContentId quest) ->
                                requireRef(choiceLocation, "effects", ContentType.QUESTS, quest);
                        case DialogueDefinition.Effect.AdvanceObjective(ContentId quest, String objective, int amount) ->
                                registry.get(ContentType.QUESTS, quest).ifPresentOrElse(q -> {
                                    if (q.objective(objective).isEmpty()) {
                                        diagnostics.add(Diagnostic.error(choiceLocation, "Quest " + quest + " hat kein Ziel '" + objective + "'"));
                                    }
                                }, () -> missing(choiceLocation, "effects", ContentType.QUESTS, quest));
                        case DialogueDefinition.Effect.GrantExperience(int amount) -> {
                            // range checked by the parser
                        }
                    }
                }
            }
        }
    }

    // ---- talent trees ---------------------------------------------------------------------------------

    private void checkTalentTree(TalentTreeDefinition tree) {
        String location = location(ContentType.TALENT_TREES, tree.id());
        boolean classBound = false;
        switch (tree.owner()) {
            case TalentTreeDefinition.Owner.General general -> {
                // accessible to everyone
            }
            case TalentTreeDefinition.Owner.ForClass(ContentId classId) -> {
                classBound = true;
                requireRef(location, "owner.class", ContentType.CLASSES, classId);
            }
            case TalentTreeDefinition.Owner.ForSubclass(ContentId subclassId) -> {
                classBound = true;
                requireRef(location, "owner.subclass", ContentType.SUBCLASSES, subclassId);
            }
        }
        int maxLevel = registry.progression().map(p -> p.maxLevel()).orElse(20);
        Map<TalentTreeDefinition.Position, String> positions = new HashMap<>();
        Map<String, List<String>> exclusiveGroups = new TreeMap<>();
        for (TalentNode node : tree.nodes().values()) {
            String nodeLocation = location + " node " + node.key();
            List<String> prerequisites = new ArrayList<>(node.requiresAll());
            prerequisites.addAll(node.requiresAny());
            for (String required : prerequisites) {
                if (!tree.nodes().containsKey(required)) {
                    diagnostics.add(Diagnostic.error(nodeLocation, "Voraussetzung '" + required + "' existiert nicht"));
                } else if (required.equals(node.key())) {
                    diagnostics.add(Diagnostic.error(nodeLocation, "Knoten setzt sich selbst voraus"));
                }
            }
            if (node.kind() == TalentTreeDefinition.NodeKind.ROOT) {
                if (!prerequisites.isEmpty()) {
                    diagnostics.add(Diagnostic.error(nodeLocation, "Wurzelknoten darf keine Voraussetzungen haben"));
                }
                if (!node.key().equals(tree.root())) {
                    diagnostics.add(Diagnostic.error(nodeLocation, "Nur ein Wurzelknoten pro Baum erlaubt"));
                }
            } else if (prerequisites.isEmpty()) {
                diagnostics.add(Diagnostic.error(nodeLocation, "Knoten ohne Voraussetzung ist vom Wurzelknoten aus nicht erreichbar"));
            }
            String previous = positions.putIfAbsent(node.position(), node.key());
            if (previous != null) {
                diagnostics.add(Diagnostic.error(nodeLocation, "Position " + node.position() + " bereits von '" + previous + "' belegt"));
            }
            if (Math.abs(node.position().x()) > MAX_GRID || Math.abs(node.position().y()) > MAX_GRID) {
                diagnostics.add(Diagnostic.error(nodeLocation, "Position außerhalb des Rasters ±" + MAX_GRID));
            }
            if (!tree.branches().isEmpty() && node.kind() != TalentTreeDefinition.NodeKind.ROOT
                    && !tree.branches().contains(node.branch())) {
                diagnostics.add(Diagnostic.error(nodeLocation, "Zweig '" + node.branch() + "' ist nicht in branches " + tree.branches()));
            }
            if (!classBound && node.requiredClassLevel() > 0) {
                diagnostics.add(Diagnostic.error(nodeLocation, "required_class_level ist nur in Klassen-/Unterklassenbäumen sinnvoll"));
            }
            if (node.requiredLevel() > maxLevel) {
                diagnostics.add(Diagnostic.error(nodeLocation, "required_level " + node.requiredLevel() + " ist höher als die Maximalstufe"));
            }
            node.exclusiveGroup().ifPresent(group -> exclusiveGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(node.key()));
            checkModifiers(nodeLocation, node.effects());
        }
        exclusiveGroups.forEach((group, members) -> {
            if (members.size() < 2) {
                diagnostics.add(Diagnostic.warning(location, "Exklusivgruppe '" + group + "' hat nur einen Knoten " + members));
            }
        });
        checkCycles(tree, location);
        checkReachability(tree, location);
    }

    private void checkCycles(TalentTreeDefinition tree, String location) {
        Map<String, Integer> state = new HashMap<>(); // 1 = visiting, 2 = done
        for (String start : tree.nodes().keySet()) {
            if (state.containsKey(start)) {
                continue;
            }
            Deque<Map.Entry<String, Integer>> stack = new ArrayDeque<>();
            stack.push(Map.entry(start, 0));
            state.put(start, 1);
            while (!stack.isEmpty()) {
                Map.Entry<String, Integer> top = stack.pop();
                List<String> edges = prerequisitesOf(tree, top.getKey());
                if (top.getValue() < edges.size()) {
                    stack.push(Map.entry(top.getKey(), top.getValue() + 1));
                    String next = edges.get(top.getValue());
                    Integer nextState = state.get(next);
                    if (nextState == null) {
                        state.put(next, 1);
                        stack.push(Map.entry(next, 0));
                    } else if (nextState == 1) {
                        diagnostics.add(Diagnostic.error(location, "Zyklische Voraussetzung über '" + top.getKey() + "' → '" + next + "'"));
                    }
                } else {
                    state.put(top.getKey(), 2);
                }
            }
        }
    }

    private static List<String> prerequisitesOf(TalentTreeDefinition tree, String key) {
        TalentNode node = tree.nodes().get(key);
        if (node == null) {
            return List.of();
        }
        List<String> edges = new ArrayList<>(node.requiresAll());
        edges.addAll(node.requiresAny());
        edges.removeIf(edge -> !tree.nodes().containsKey(edge));
        return edges;
    }

    /** A node is reachable if all of requires_all and (if given) one of requires_any are reachable. */
    private void checkReachability(TalentTreeDefinition tree, String location) {
        Set<String> reachable = new HashSet<>();
        reachable.add(tree.root());
        boolean changed = true;
        while (changed) {
            changed = false;
            for (TalentNode node : tree.nodes().values()) {
                if (reachable.contains(node.key())) {
                    continue;
                }
                boolean all = reachable.containsAll(node.requiresAll());
                boolean any = node.requiresAny().isEmpty() || node.requiresAny().stream().anyMatch(reachable::contains);
                boolean hasAnyPrerequisite = !node.requiresAll().isEmpty() || !node.requiresAny().isEmpty();
                if (all && any && hasAnyPrerequisite && !violatesExclusivity(tree, node)) {
                    reachable.add(node.key());
                    changed = true;
                }
            }
        }
        Set<String> unreachable = new TreeSet<>(tree.nodes().keySet());
        unreachable.removeAll(reachable);
        if (!unreachable.isEmpty()) {
            diagnostics.add(Diagnostic.error(location, "Vom Wurzelknoten nicht erreichbare Knoten: " + unreachable));
        }
    }

    /** A node that requires two members of its own exclusive group can never be unlocked. */
    private static boolean violatesExclusivity(TalentTreeDefinition tree, TalentNode node) {
        Map<String, Integer> groupCounts = new HashMap<>();
        for (String required : node.requiresAll()) {
            TalentNode prerequisite = tree.nodes().get(required);
            if (prerequisite != null && prerequisite.exclusiveGroup().isPresent()) {
                groupCounts.merge(prerequisite.exclusiveGroup().get(), 1, Integer::sum);
            }
        }
        return groupCounts.values().stream().anyMatch(count -> count > 1);
    }

    // ---- shared helpers -------------------------------------------------------------------------------

    private void checkModifiers(String location, List<Modifier> modifiers) {
        for (Modifier modifier : modifiers) {
            switch (modifier) {
                case Modifier.GrantPower grant -> requireRef(location, "grant_power", ContentType.POWERS, grant.power());
                case Modifier.SkillProficiency skill -> requireRef(location, "skill_proficiency", ContentType.SKILLS, skill.skill());
                case Modifier.Flag flag -> EngineFlags.lookup(flag.flag()).ifPresentOrElse(info -> {
                    if (info.implementation() == EngineFlags.Implementation.PLANNED) {
                        plannedFlagUsers.computeIfAbsent(flag.flag(), f -> new TreeSet<>()).add(location);
                    }
                }, () -> diagnostics.add(Diagnostic.error(location, "Unbekannte Engine-Funktion " + flag.flag())));
                case Modifier.AttributeModifier attribute -> {
                    // Validated against Minecraft's attribute registry by the mod.
                }
                case Modifier.AbilityScoreBonus bonus -> {
                    // Range already checked by the parser.
                }
                case Modifier.Resistance resistance -> {
                    // Damage types are validated by the mod.
                }
            }
        }
    }

    private <T extends ContentDefinition> void requireRef(String location, String field, ContentType<T> type, ContentId id) {
        if (!registry.contains(type, id)) {
            missing(location, field, type, id);
        }
    }

    private void missing(String location, String field, ContentType<?> type, ContentId id) {
        diagnostics.add(Diagnostic.error(location, field + ": " + type.folder() + " " + id + " existiert nicht"));
    }

    private void checkTranslations() {
        Set<String> required = new TreeSet<>();
        for (ContentType<?> type : ContentType.ALL) {
            for (ContentDefinition definition : registry.all(type).values()) {
                required.addAll(definition.requiredTranslationKeys());
            }
        }
        for (AbilityScore ability : AbilityScore.values()) {
            required.add(ability.translationKey());
        }
        Map<String, String> primary = registry.translations(PRIMARY_LOCALE);
        List<String> missingPrimary = required.stream().filter(key -> !primary.containsKey(key)).toList();
        if (!missingPrimary.isEmpty()) {
            diagnostics.add(Diagnostic.error("lang/" + PRIMARY_LOCALE,
                    missingPrimary.size() + " fehlende Übersetzungen: " + abbreviate(missingPrimary)));
        }
        for (String locale : SECONDARY_LOCALES) {
            Map<String, String> secondary = registry.translations(locale);
            List<String> missing = required.stream().filter(key -> !secondary.containsKey(key)).toList();
            if (!missing.isEmpty()) {
                diagnostics.add(Diagnostic.warning("lang/" + locale, missing.size() + " fehlende Übersetzungen: " + abbreviate(missing)));
            }
        }
    }

    private static String abbreviate(List<String> keys) {
        return keys.size() <= 8 ? keys.toString() : keys.subList(0, 8) + " …";
    }

    private static String location(ContentType<?> type, ContentId id) {
        return type.folder() + " " + id;
    }
}
