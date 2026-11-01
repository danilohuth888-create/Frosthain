package net.frosthain.ui.talent;

import net.frosthain.core.ContentId;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.character.TalentRef;
import net.frosthain.core.content.TalentTreeDefinition;
import net.frosthain.core.content.TalentTreeDefinition.NodeKind;
import net.frosthain.core.content.TalentTreeDefinition.TalentNode;
import net.frosthain.core.progression.Blocker;
import net.frosthain.core.progression.TalentAllocator;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.ui.Theme;
import net.frosthain.ui.Translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Everything the talent screen shows, computed from the real rules. Renderers (Minecraft screen, desktop
 * prototype) only draw this; they never decide what is allowed.
 */
public final class TalentTreeView {

    public record PathEntry(ContentId tree, String name, TalentTreeDefinition.Category category, boolean accessible) {
    }

    public record Tab(String branch, String name, int color) {
    }

    public record NodeView(String key, TalentRef ref, NodeKind kind, String branch, int color, double x, double y,
                           NodeState state, int rank, int maxRank, String icon, String name) {
    }

    public record EdgeView(String from, String to, boolean anyOf, boolean active, int color) {
    }

    public record Requirement(String text, boolean met) {
    }

    /** Content of the detail panel on the right (reference image D). */
    public record Detail(String key, String name, String kindLabel, String description, String icon, int color,
                         NodeState state, String rankText, String costText, List<Requirement> requirements,
                         String statusText, int statusColor, List<String> reasons, boolean canUnlock, boolean canRefund) {
    }

    private final ContentRegistry registry;
    private final TalentAllocator allocator;
    private final Translator translator;
    private final CharacterSheet sheet;
    private final TalentTreeDefinition tree;
    private final boolean accessible;

    public TalentTreeView(ContentRegistry registry, TalentAllocator allocator, Translator translator,
                          CharacterSheet sheet, ContentId treeId) {
        this.registry = registry;
        this.allocator = allocator;
        this.translator = translator;
        this.sheet = sheet;
        this.tree = registry.get(ContentType.TALENT_TREES, treeId)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Talentbaum " + treeId));
        this.accessible = allocator.isAccessible(sheet, tree);
    }

    public TalentTreeDefinition tree() {
        return tree;
    }

    public boolean accessible() {
        return accessible;
    }

    public String title() {
        return translator.t(tree.translationKey());
    }

    public int availablePoints() {
        return allocator.availablePoints(sheet);
    }

    public int characterLevel() {
        return sheet.characterLevel();
    }

    /** All trees: general and rune paths first, then accessible class trees, then the others as preview. */
    public static List<PathEntry> paths(ContentRegistry registry, TalentAllocator allocator, Translator translator,
                                        CharacterSheet sheet) {
        List<PathEntry> entries = new ArrayList<>();
        for (TalentTreeDefinition candidate : registry.all(ContentType.TALENT_TREES).values()) {
            entries.add(new PathEntry(candidate.id(), translator.t(candidate.translationKey()), candidate.category(),
                    allocator.isAccessible(sheet, candidate)));
        }
        entries.sort(Comparator
                .comparingInt((PathEntry e) -> switch (e.category()) {
                    case GENERAL -> 0;
                    case RUNE -> 1;
                    case CLASS -> 2;
                })
                .thenComparing(e -> !e.accessible())
                .thenComparing(PathEntry::name));
        return List.copyOf(entries);
    }

    public List<Tab> tabs() {
        List<Tab> tabs = new ArrayList<>();
        for (int i = 0; i < tree.branches().size(); i++) {
            String branch = tree.branches().get(i);
            tabs.add(new Tab(branch, translator.t(tree.translationKey() + ".branch." + branch),
                    Theme.branchColor(tree.category(), i)));
        }
        return tabs;
    }

    public int branchColor(String branch) {
        return Theme.branchColor(tree.category(), tree.branches().indexOf(branch));
    }

    public List<NodeView> nodes() {
        List<NodeView> views = new ArrayList<>();
        for (TalentNode node : tree.nodes().values()) {
            views.add(nodeView(node));
        }
        return views;
    }

    public Optional<NodeView> node(String key) {
        return tree.node(key).map(this::nodeView);
    }

    private NodeView nodeView(TalentNode node) {
        TalentRef ref = new TalentRef(tree.id(), node.key());
        int rank = allocator.rank(sheet, ref);
        int color = node.kind() == NodeKind.ROOT ? Theme.FRAME_GOLD_BRIGHT : branchColor(node.branch());
        String icon = node.icon().map(ContentId::path).map(p -> p.substring(p.lastIndexOf('/') + 1)).orElse("star");
        return new NodeView(node.key(), ref, node.kind(), node.branch(), color, node.position().x(), node.position().y(),
                state(node, ref, rank), rank, node.maxRank(), icon, translator.t(node.translationKey(tree)));
    }

    private NodeState state(TalentNode node, TalentRef ref, int rank) {
        if (node.kind() == NodeKind.ROOT) {
            return accessible ? NodeState.ORIGIN : NodeState.LOCKED;
        }
        if (rank >= node.maxRank()) {
            return NodeState.MAXED;
        }
        if (rank > 0) {
            return NodeState.UNLOCKED;
        }
        List<Blocker> blockers = allocator.checkAllocate(sheet, ref);
        if (blockers.isEmpty()) {
            return NodeState.AVAILABLE;
        }
        boolean onlyPoints = blockers.stream().allMatch(b -> b.reason() == Blocker.Reason.NOT_ENOUGH_POINTS);
        return onlyPoints ? NodeState.REACHABLE : NodeState.LOCKED;
    }

    public List<EdgeView> edges() {
        List<EdgeView> edges = new ArrayList<>();
        for (TalentNode node : tree.nodes().values()) {
            int target = allocator.rank(sheet, new TalentRef(tree.id(), node.key()));
            for (String from : node.requiresAll()) {
                edges.add(edge(from, node, false, target));
            }
            for (String from : node.requiresAny()) {
                edges.add(edge(from, node, true, target));
            }
        }
        return edges;
    }

    private EdgeView edge(String from, TalentNode to, boolean anyOf, int targetRank) {
        int sourceRank = allocator.rank(sheet, new TalentRef(tree.id(), from));
        return new EdgeView(from, to.key(), anyOf, sourceRank > 0 && targetRank > 0, branchColor(to.branch()));
    }

    /** Bounding box of all node positions (grid units), used to fit the camera. */
    public double[] bounds() {
        double minX = 0, minY = 0, maxX = 0, maxY = 0;
        for (TalentNode node : tree.nodes().values()) {
            minX = Math.min(minX, node.position().x());
            minY = Math.min(minY, node.position().y());
            maxX = Math.max(maxX, node.position().x());
            maxY = Math.max(maxY, node.position().y());
        }
        return new double[]{minX, minY, maxX, maxY};
    }

    /** Average position of a branch's nodes, used when a tab is selected. */
    public double[] branchCenter(String branch) {
        double sx = 0, sy = 0;
        int n = 0;
        for (TalentNode node : tree.nodes().values()) {
            if (node.branch().equals(branch)) {
                sx += node.position().x();
                sy += node.position().y();
                n++;
            }
        }
        return n == 0 ? new double[]{0, 0} : new double[]{sx / n, sy / n};
    }

    public Detail detail(String key) {
        TalentNode node = tree.node(key).orElseThrow(() -> new IllegalArgumentException("Unbekannter Knoten " + key));
        NodeView view = nodeView(node);
        TalentRef ref = view.ref();
        String kindLabel = translator.t("ui.frosthain.talents.kind." + node.kind().name().toLowerCase(Locale.ROOT));

        List<Requirement> requirements = new ArrayList<>();
        for (String required : node.requiresAll()) {
            requirements.add(new Requirement(nodeName(required), allocator.rank(sheet, new TalentRef(tree.id(), required)) > 0));
        }
        if (!node.requiresAny().isEmpty()) {
            List<String> names = node.requiresAny().stream().map(this::nodeName).toList();
            boolean met = node.requiresAny().stream().anyMatch(r -> allocator.rank(sheet, new TalentRef(tree.id(), r)) > 0);
            requirements.add(new Requirement(translator.t("ui.frosthain.talents.requires_any") + " " + String.join(", ", names), met));
        }
        if (node.requiredLevel() > 1) {
            requirements.add(new Requirement(translator.t("ui.frosthain.talents.requires_level", node.requiredLevel()),
                    sheet.characterLevel() >= node.requiredLevel()));
        }
        if (node.requiredClassLevel() > 0) {
            boolean met = allocator.checkAllocate(sheet, ref).stream().noneMatch(b -> b.reason() == Blocker.Reason.CLASS_LEVEL_TOO_LOW);
            requirements.add(new Requirement(translator.t("ui.frosthain.talents.requires_class_level", node.requiredClassLevel()),
                    met || view.rank() > 0));
        }

        String costText = node.costPerRank() == 1
                ? translator.t("ui.frosthain.talents.cost.one")
                : translator.t("ui.frosthain.talents.cost.many", node.costPerRank());
        String rankText = translator.t("ui.frosthain.talents.rank", view.rank(), node.maxRank());

        List<Blocker> blockers = node.kind() == NodeKind.ROOT || view.state() == NodeState.MAXED
                ? List.of() : allocator.checkAllocate(sheet, ref);
        List<String> reasons = new ArrayList<>();
        for (Blocker blocker : blockers) {
            reasons.add(translator.t("blocker.frosthain." + blocker.reason().name().toLowerCase(Locale.ROOT), blockerDetail(blocker)));
        }
        boolean canUnlock = node.kind() != NodeKind.ROOT && view.state() != NodeState.MAXED && blockers.isEmpty();
        boolean canRefund = node.kind() != NodeKind.ROOT && view.rank() > 0 && allocator.checkRefund(sheet, ref).isEmpty();

        String statusText;
        int statusColor;
        switch (view.state()) {
            case ORIGIN -> {
                statusText = translator.t("ui.frosthain.talents.status.origin");
                statusColor = Theme.FRAME_GOLD_BRIGHT;
            }
            case MAXED -> {
                statusText = translator.t("ui.frosthain.talents.status.maxed");
                statusColor = Theme.FRAME_GOLD_BRIGHT;
            }
            case UNLOCKED -> {
                statusText = translator.t("ui.frosthain.talents.status.unlocked");
                statusColor = Theme.FRAME_GOLD_BRIGHT;
            }
            case AVAILABLE -> {
                statusText = translator.t("ui.frosthain.talents.status.available");
                statusColor = Theme.STATE_AVAILABLE;
            }
            default -> {
                statusText = translator.t("ui.frosthain.talents.status.locked");
                statusColor = Theme.STATE_BLOCKED;
            }
        }
        return new Detail(key, view.name(), kindLabel, translator.t(node.translationKey(tree) + ".desc"), view.icon(),
                view.color(), view.state(), rankText, costText, List.copyOf(requirements), statusText, statusColor,
                List.copyOf(reasons), canUnlock, canRefund);
    }

    private String blockerDetail(Blocker blocker) {
        return switch (blocker.reason()) {
            case MISSING_PREREQUISITE, EXCLUSIVE_CONFLICT -> nodeName(blocker.detail());
            case MISSING_ANY_PREREQUISITE -> String.join(", ",
                    Arrays.stream(blocker.detail().split(",\\s*")).map(this::nodeName).toList());
            case TREE_NOT_ACCESSIBLE -> translator.t(tree.translationKey());
            default -> blocker.detail();
        };
    }

    private String nodeName(String key) {
        return tree.node(key).map(n -> translator.t(n.translationKey(tree))).orElse(key);
    }
}
