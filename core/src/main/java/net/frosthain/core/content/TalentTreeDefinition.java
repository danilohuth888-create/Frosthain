package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A visually branching talent tree. Nodes sit on a grid around a central root node; edges are given
 * by prerequisites. A tree either belongs to everyone ({@code general}), to a class or to a subclass.
 */
public record TalentTreeDefinition(
        ContentId id,
        ContentId source,
        boolean adapted,
        Owner owner,
        String root,
        List<String> branches,
        Map<String, TalentNode> nodes
) implements ContentDefinition {

    public sealed interface Owner {
        record General() implements Owner {
        }

        record ForClass(ContentId classId) implements Owner {
        }

        record ForSubclass(ContentId subclassId) implements Owner {
        }
    }

    public enum NodeKind {
        /** Entry point; free and automatically unlocked once the tree is accessible. */
        ROOT,
        /** Permanent bonus. */
        PASSIVE,
        /** Unlocks an active power. */
        ACTIVE,
        /** Build-defining node; usually in an exclusive group. */
        KEYSTONE
    }

    public record Position(int x, int y) {
    }

    /**
     * @param requiresAll        all of these nodes need at least rank 1
     * @param requiresAny        at least one of these nodes needs rank 1 (empty = no constraint)
     * @param requiredLevel      minimum character level
     * @param requiredClassLevel minimum level in the owning class (class/subclass trees only)
     * @param exclusiveGroup     at most one node of the same group can be allocated
     * @param effects            effects per rank; numeric amounts are multiplied by the rank
     */
    public record TalentNode(
            String key,
            NodeKind kind,
            String branch,
            Position position,
            int maxRank,
            int costPerRank,
            List<String> requiresAll,
            List<String> requiresAny,
            int requiredLevel,
            int requiredClassLevel,
            Optional<String> exclusiveGroup,
            List<Modifier> effects,
            Optional<ContentId> icon
    ) {
        public String translationKey(TalentTreeDefinition tree) {
            return "talent." + tree.id().toTranslationFragment() + "." + key;
        }
    }

    private static final Pattern NODE_KEY = Pattern.compile("[a-z0-9_]+");

    public static TalentTreeDefinition parse(ContentId id, JsonObjectReader json) {
        JsonObjectReader ownerJson = json.object("owner");
        String ownerType = ownerJson.string("type");
        Owner owner = switch (ownerType) {
            case "general" -> new Owner.General();
            case "class" -> new Owner.ForClass(ownerJson.id("class"));
            case "subclass" -> new Owner.ForSubclass(ownerJson.id("subclass"));
            default -> throw new ContentParseException(ownerJson.childPath("type"),
                    "erwartet: general, class oder subclass");
        };
        ownerJson.finish();

        String root = json.string("root");
        List<String> branches = json.stringList("branches");
        for (String branch : branches) {
            checkKey(json.childPath("branches"), branch);
        }
        Map<String, TalentNode> nodes = new LinkedHashMap<>();
        List<TalentNode> parsed = json.objectList("nodes", TalentTreeDefinition::parseNode);
        for (int i = 0; i < parsed.size(); i++) {
            TalentNode node = parsed.get(i);
            if (nodes.putIfAbsent(node.key(), node) != null) {
                throw new ContentParseException(json.childPath("nodes") + "[" + i + "]", "Knoten '" + node.key() + "' doppelt definiert");
            }
        }
        if (!nodes.containsKey(root)) {
            throw new ContentParseException(json.childPath("root"), "Wurzelknoten '" + root + "' existiert nicht");
        }
        if (nodes.get(root).kind() != NodeKind.ROOT) {
            throw new ContentParseException(json.childPath("root"), "Wurzelknoten muss kind=root haben");
        }
        return new TalentTreeDefinition(id, json.id("source"), json.bool("adapted", false), owner, root,
                branches, Collections.unmodifiableMap(nodes));
    }

    private static TalentNode parseNode(JsonObjectReader json) {
        String key = json.string("key");
        checkKey(json.childPath("key"), key);
        NodeKind kind = json.enumValue("kind", NodeKind.class);
        List<Integer> pos = json.intList("position");
        if (pos.size() != 2) {
            throw new ContentParseException(json.childPath("position"), "[x, y] erwartet");
        }
        int maxRank = json.integerInRange("max_rank", 1, 1, 5);
        int cost = json.integerInRange("cost", kind == NodeKind.ROOT ? 0 : 1, 0, 5);
        if (kind == NodeKind.ROOT && (cost != 0 || maxRank != 1)) {
            throw new ContentParseException(json.path(), "Wurzelknoten sind kostenlos und haben genau einen Rang");
        }
        if (kind != NodeKind.ROOT && cost == 0) {
            throw new ContentParseException(json.childPath("cost"), "nur Wurzelknoten dürfen kostenlos sein");
        }
        List<Modifier> effects = json.objectList("effects", Modifier::parse);
        if (kind == NodeKind.ACTIVE && effects.stream().noneMatch(m -> m instanceof Modifier.GrantPower)) {
            throw new ContentParseException(json.childPath("effects"), "aktive Knoten müssen eine Fähigkeit gewähren (grant_power)");
        }
        String branch = json.optString("branch").orElse("");
        return new TalentNode(key, kind, branch, new Position(pos.get(0), pos.get(1)), maxRank, cost,
                json.stringList("requires"), json.stringList("requires_any"),
                json.integerInRange("required_level", 1, 1, 20),
                json.integerInRange("required_class_level", 0, 0, 20),
                json.optString("exclusive_group"), effects, json.optId("icon"));
    }

    private static void checkKey(String path, String key) {
        if (!NODE_KEY.matcher(key).matches()) {
            throw new ContentParseException(path, "Schlüssel '" + key + "' darf nur a-z, 0-9 und _ enthalten");
        }
    }

    public Optional<TalentNode> node(String key) {
        return Optional.ofNullable(nodes.get(key));
    }

    public String translationKey() {
        return "talent_tree." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(translationKey());
        for (String branch : branches) {
            keys.add(translationKey() + ".branch." + branch);
        }
        for (TalentNode node : nodes.values()) {
            keys.add(node.translationKey(this));
            keys.add(node.translationKey(this) + ".desc");
        }
        return keys;
    }
}
