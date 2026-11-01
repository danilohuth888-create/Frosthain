package net.frosthain.core.character;

import net.frosthain.core.ContentId;

/** Reference to a node in a talent tree, serialised as {@code <tree id>#<node key>}. */
public record TalentRef(ContentId tree, String node) implements Comparable<TalentRef> {

    public static TalentRef parse(String text) {
        int hash = text.indexOf('#');
        if (hash <= 0 || hash == text.length() - 1) {
            throw new IllegalArgumentException("Talent-Referenz '<baum>#<knoten>' erwartet: " + text);
        }
        return new TalentRef(ContentId.parse(text.substring(0, hash)), text.substring(hash + 1));
    }

    @Override
    public int compareTo(TalentRef other) {
        int byTree = tree.compareTo(other.tree);
        return byTree != 0 ? byTree : node.compareTo(other.node);
    }

    @Override
    public String toString() {
        return tree + "#" + node;
    }
}
