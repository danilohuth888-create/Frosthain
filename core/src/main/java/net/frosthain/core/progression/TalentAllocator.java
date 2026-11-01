package net.frosthain.core.progression;

import net.frosthain.core.ContentId;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.content.SubclassDefinition;
import net.frosthain.core.content.TalentTreeDefinition;
import net.frosthain.core.content.TalentTreeDefinition.NodeKind;
import net.frosthain.core.content.TalentTreeDefinition.TalentNode;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.core.rules.ProgressionRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Talent rules: point budget, prerequisites, exclusive choices, refunds and resets.
 *
 * <p>Design rules (docs/06-datenmodell.md):
 * <ul>
 *   <li>Every character level grants talent points (plus bonus points at configured levels).</li>
 *   <li>Points can be spent in the general tree and in the trees of every class/subclass the character has.</li>
 *   <li>Root nodes are free and unlocked as soon as the tree is accessible.</li>
 *   <li>A single rank can only be refunded if no other allocated talent depends on it.</li>
 *   <li>A full reset is always possible; its in-game price is decided by the mod, not here.</li>
 * </ul>
 * All methods are pure: they never mutate the sheet, they return a new one.
 */
public final class TalentAllocator {

    private final ContentRegistry registry;
    private final ProgressionRules rules;

    public TalentAllocator(ContentRegistry registry) {
        this.registry = registry;
        this.rules = registry.progression().orElseThrow(() -> new IllegalStateException("Fortschrittsregeln fehlen"));
    }

    public int earnedPoints(CharacterSheet sheet) {
        return rules.talentPointsAtLevel(sheet.characterLevel());
    }

    public int spentPoints(CharacterSheet sheet) {
        int spent = 0;
        for (Map.Entry<TalentRef, Integer> entry : sheet.talents().entrySet()) {
            spent += findNode(entry.getKey()).map(node -> node.costPerRank() * entry.getValue()).orElse(0);
        }
        return spent;
    }

    public int availablePoints(CharacterSheet sheet) {
        return earnedPoints(sheet) - spentPoints(sheet);
    }

    public int rank(CharacterSheet sheet, TalentRef ref) {
        Optional<TalentTreeDefinition> tree = registry.get(ContentType.TALENT_TREES, ref.tree());
        if (tree.isPresent() && tree.get().root().equals(ref.node()) && isAccessible(sheet, tree.get())) {
            return 1;
        }
        return sheet.talents().getOrDefault(ref, 0);
    }

    /** Whether the character may use this tree at all (general trees always, class trees with that class). */
    public boolean isAccessible(CharacterSheet sheet, TalentTreeDefinition tree) {
        return switch (tree.owner()) {
            case TalentTreeDefinition.Owner.General general -> true;
            case TalentTreeDefinition.Owner.ForClass(ContentId classId) -> sheet.classLevel(classId) > 0;
            case TalentTreeDefinition.Owner.ForSubclass(ContentId subclassId) -> sheet.subclasses().containsValue(subclassId);
        };
    }

    /** Level of the class that owns the tree (0 for general trees). */
    private int owningClassLevel(CharacterSheet sheet, TalentTreeDefinition tree) {
        return switch (tree.owner()) {
            case TalentTreeDefinition.Owner.General general -> 0;
            case TalentTreeDefinition.Owner.ForClass(ContentId classId) -> sheet.classLevel(classId);
            case TalentTreeDefinition.Owner.ForSubclass(ContentId subclassId) -> registry
                    .get(ContentType.SUBCLASSES, subclassId)
                    .map(SubclassDefinition::parentClass)
                    .map(sheet::classLevel)
                    .orElse(0);
        };
    }

    /** All reasons why one more rank cannot be allocated; empty list = allowed. */
    public List<Blocker> checkAllocate(CharacterSheet sheet, TalentRef ref) {
        Optional<TalentTreeDefinition> treeOpt = registry.get(ContentType.TALENT_TREES, ref.tree());
        if (treeOpt.isEmpty()) {
            return List.of(Blocker.of(Blocker.Reason.UNKNOWN_TREE, ref.tree()));
        }
        TalentTreeDefinition tree = treeOpt.get();
        Optional<TalentNode> nodeOpt = tree.node(ref.node());
        if (nodeOpt.isEmpty()) {
            return List.of(Blocker.of(Blocker.Reason.UNKNOWN_NODE, ref));
        }
        TalentNode node = nodeOpt.get();
        if (node.kind() == NodeKind.ROOT) {
            return List.of(Blocker.of(Blocker.Reason.ROOT_NODE));
        }
        List<Blocker> blockers = new ArrayList<>();
        if (!isAccessible(sheet, tree)) {
            blockers.add(Blocker.of(Blocker.Reason.TREE_NOT_ACCESSIBLE, tree.id()));
        }
        int current = sheet.talents().getOrDefault(ref, 0);
        if (current >= node.maxRank()) {
            blockers.add(Blocker.of(Blocker.Reason.MAX_RANK_REACHED, node.maxRank()));
        }
        if (availablePoints(sheet) < node.costPerRank()) {
            blockers.add(Blocker.of(Blocker.Reason.NOT_ENOUGH_POINTS, node.costPerRank()));
        }
        if (sheet.characterLevel() < node.requiredLevel()) {
            blockers.add(Blocker.of(Blocker.Reason.CHARACTER_LEVEL_TOO_LOW, node.requiredLevel()));
        }
        if (node.requiredClassLevel() > 0 && owningClassLevel(sheet, tree) < node.requiredClassLevel()) {
            blockers.add(Blocker.of(Blocker.Reason.CLASS_LEVEL_TOO_LOW, node.requiredClassLevel()));
        }
        blockers.addAll(prerequisiteBlockers(sheet, tree, node));
        node.exclusiveGroup().ifPresent(group -> {
            for (TalentNode other : tree.nodes().values()) {
                if (!other.key().equals(node.key()) && other.exclusiveGroup().equals(node.exclusiveGroup())
                        && rank(sheet, new TalentRef(tree.id(), other.key())) > 0) {
                    blockers.add(Blocker.of(Blocker.Reason.EXCLUSIVE_CONFLICT, other.key()));
                }
            }
        });
        return List.copyOf(blockers);
    }

    private List<Blocker> prerequisiteBlockers(CharacterSheet sheet, TalentTreeDefinition tree, TalentNode node) {
        List<Blocker> blockers = new ArrayList<>();
        for (String required : node.requiresAll()) {
            if (rank(sheet, new TalentRef(tree.id(), required)) == 0) {
                blockers.add(Blocker.of(Blocker.Reason.MISSING_PREREQUISITE, required));
            }
        }
        if (!node.requiresAny().isEmpty()
                && node.requiresAny().stream().noneMatch(required -> rank(sheet, new TalentRef(tree.id(), required)) > 0)) {
            blockers.add(Blocker.of(Blocker.Reason.MISSING_ANY_PREREQUISITE, String.join(", ", node.requiresAny())));
        }
        return blockers;
    }

    public CharacterSheet allocate(CharacterSheet sheet, TalentRef ref) {
        List<Blocker> blockers = checkAllocate(sheet, ref);
        if (!blockers.isEmpty()) {
            throw new IllegalStateException("Talent " + ref + " kann nicht vergeben werden: " + blockers);
        }
        Map<TalentRef, Integer> talents = new TreeMap<>(sheet.talents());
        talents.merge(ref, 1, Integer::sum);
        return sheet.withTalents(talents);
    }

    /** Reasons why one rank of {@code ref} cannot be refunded; empty list = allowed. */
    public List<Blocker> checkRefund(CharacterSheet sheet, TalentRef ref) {
        if (sheet.talents().getOrDefault(ref, 0) == 0) {
            return List.of(Blocker.of(Blocker.Reason.NOT_ALLOCATED, ref));
        }
        CharacterSheet after = removeOneRank(sheet, ref);
        Set<TalentRef> alreadyInvalid = new HashSet<>();
        findViolations(sheet).forEach(violation -> alreadyInvalid.add(violation.talent()));
        List<Blocker> blockers = new ArrayList<>();
        for (Violation violation : findViolations(after)) {
            if (!alreadyInvalid.contains(violation.talent())) {
                blockers.add(Blocker.of(Blocker.Reason.REQUIRED_BY_OTHER_TALENT, violation.talent()));
            }
        }
        return List.copyOf(blockers);
    }

    public CharacterSheet refund(CharacterSheet sheet, TalentRef ref) {
        List<Blocker> blockers = checkRefund(sheet, ref);
        if (!blockers.isEmpty()) {
            throw new IllegalStateException("Talent " + ref + " kann nicht zurückgenommen werden: " + blockers);
        }
        return removeOneRank(sheet, ref);
    }

    private static CharacterSheet removeOneRank(CharacterSheet sheet, TalentRef ref) {
        Map<TalentRef, Integer> talents = new TreeMap<>(sheet.talents());
        talents.computeIfPresent(ref, (key, rank) -> rank > 1 ? rank - 1 : null);
        return sheet.withTalents(talents);
    }

    /** Full reset of all trees. The in-game cost (if any) is applied by the caller. */
    public CharacterSheet resetAll(CharacterSheet sheet) {
        return sheet.withTalents(Map.of());
    }

    public CharacterSheet resetTree(CharacterSheet sheet, ContentId treeId) {
        Map<TalentRef, Integer> talents = new TreeMap<>(sheet.talents());
        talents.keySet().removeIf(ref -> ref.tree().equals(treeId));
        return sheet.withTalents(talents);
    }

    /** A talent allocation that is not (or no longer) valid. */
    public record Violation(TalentRef talent, List<Blocker> reasons) {
    }

    /**
     * Checks a complete sheet, e.g. after loading a save game with changed content. Every allocation is
     * re-checked as if it were the last one taken (rank - 1 plus the remaining points).
     */
    public List<Violation> findViolations(CharacterSheet sheet) {
        List<Violation> violations = new ArrayList<>();
        int available = availablePoints(sheet);
        for (Map.Entry<TalentRef, Integer> entry : sheet.talents().entrySet()) {
            TalentRef ref = entry.getKey();
            Optional<TalentTreeDefinition> tree = registry.get(ContentType.TALENT_TREES, ref.tree());
            Optional<TalentNode> node = tree.flatMap(t -> t.node(ref.node()));
            if (tree.isEmpty() || node.isEmpty()) {
                violations.add(new Violation(ref, List.of(Blocker.of(Blocker.Reason.UNKNOWN_NODE, ref))));
                continue;
            }
            List<Blocker> reasons = new ArrayList<>();
            if (node.get().kind() == NodeKind.ROOT) {
                reasons.add(Blocker.of(Blocker.Reason.ROOT_NODE));
            }
            if (!isAccessible(sheet, tree.get())) {
                reasons.add(Blocker.of(Blocker.Reason.TREE_NOT_ACCESSIBLE, tree.get().id()));
            }
            if (entry.getValue() > node.get().maxRank()) {
                reasons.add(Blocker.of(Blocker.Reason.MAX_RANK_REACHED, node.get().maxRank()));
            }
            if (sheet.characterLevel() < node.get().requiredLevel()) {
                reasons.add(Blocker.of(Blocker.Reason.CHARACTER_LEVEL_TOO_LOW, node.get().requiredLevel()));
            }
            if (node.get().requiredClassLevel() > 0 && owningClassLevel(sheet, tree.get()) < node.get().requiredClassLevel()) {
                reasons.add(Blocker.of(Blocker.Reason.CLASS_LEVEL_TOO_LOW, node.get().requiredClassLevel()));
            }
            reasons.addAll(prerequisiteBlockers(sheet, tree.get(), node.get()));
            node.get().exclusiveGroup().ifPresent(group -> tree.get().nodes().values().stream()
                    .filter(other -> !other.key().equals(ref.node()) && other.exclusiveGroup().equals(node.get().exclusiveGroup()))
                    .filter(other -> sheet.talents().getOrDefault(new TalentRef(ref.tree(), other.key()), 0) > 0)
                    .forEach(other -> reasons.add(Blocker.of(Blocker.Reason.EXCLUSIVE_CONFLICT, other.key()))));
            if (!reasons.isEmpty()) {
                violations.add(new Violation(ref, List.copyOf(reasons)));
            }
        }
        if (available < 0) {
            violations.add(new Violation(BUDGET,
                    List.of(Blocker.of(Blocker.Reason.NOT_ENOUGH_POINTS, -available))));
        }
        return List.copyOf(violations);
    }

    /**
     * Removes invalid allocations so that a save game stays loadable after content updates. Points are
     * refunded implicitly (they are derived from the level). Returns the cleaned sheet and what was removed,
     * so the mod can inform the player instead of silently changing their build.
     */
    public record Sanitized(CharacterSheet sheet, List<TalentRef> removed) {
    }

    public Sanitized sanitize(CharacterSheet sheet) {
        CharacterSheet current = sheet;
        List<TalentRef> removed = new ArrayList<>();
        // Removing one allocation can invalidate dependants, so iterate until stable.
        for (int guard = 0; guard < 1000; guard++) {
            List<Violation> violations = findViolations(current);
            if (violations.isEmpty()) {
                return new Sanitized(current, List.copyOf(removed));
            }
            Violation first = violations.getFirst();
            Map<TalentRef, Integer> talents = new TreeMap<>(current.talents());
            if (first.talent().tree().equals(BUDGET.tree())) {
                // Over budget (e.g. fewer points after a rule change): remove the most advanced talent
                // that nothing else depends on, one rank at a time.
                TalentRef leaf = mostAdvancedLeaf(current);
                talents.computeIfPresent(leaf, (key, rank) -> rank > 1 ? rank - 1 : null);
                removed.add(leaf);
            } else {
                talents.remove(first.talent());
                removed.add(first.talent());
            }
            current = current.withTalents(talents);
        }
        throw new IllegalStateException("Talentbereinigung konvergiert nicht");
    }

    /** Pseudo reference used for "more points spent than earned". */
    public static final TalentRef BUDGET = new TalentRef(ContentId.of("budget"), "points");

    private TalentRef mostAdvancedLeaf(CharacterSheet sheet) {
        Set<TalentRef> required = new HashSet<>();
        for (TalentRef ref : sheet.talents().keySet()) {
            findNode(ref).ifPresent(node -> {
                node.requiresAll().forEach(key -> required.add(new TalentRef(ref.tree(), key)));
                node.requiresAny().forEach(key -> required.add(new TalentRef(ref.tree(), key)));
            });
        }
        return sheet.talents().keySet().stream()
                .filter(ref -> !required.contains(ref))
                .max(Comparator
                        .comparingInt((TalentRef ref) -> findNode(ref).map(TalentNode::requiredLevel).orElse(0))
                        .thenComparing(Comparator.naturalOrder()))
                .orElseGet(() -> sheet.talents().keySet().iterator().next());
    }

    private Optional<TalentNode> findNode(TalentRef ref) {
        return registry.get(ContentType.TALENT_TREES, ref.tree()).flatMap(tree -> tree.node(ref.node()));
    }
}
