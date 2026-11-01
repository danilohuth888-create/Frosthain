package net.frosthain.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Namespaced identifier for content, e.g. {@code frosthain:fighter}.
 *
 * <p>The character rules intentionally mirror Minecraft resource locations
 * ({@code [a-z0-9_.-]} for the namespace, additionally {@code /} for the path), so every
 * {@code ContentId} can be converted 1:1 into a Minecraft identifier by the mod layer
 * without the core depending on Minecraft.
 */
public record ContentId(String namespace, String path) implements Comparable<ContentId> {

    public static final String DEFAULT_NAMESPACE = "frosthain";

    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_.-]+(/[a-z0-9_.-]+)*");

    public ContentId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Ungültiger Namespace '" + namespace + "' (erlaubt: a-z 0-9 _ . -)");
        }
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Ungültiger Pfad '" + path + "' (erlaubt: a-z 0-9 _ . - /)");
        }
    }

    /** Parses {@code namespace:path}; a missing namespace defaults to {@value #DEFAULT_NAMESPACE}. */
    public static ContentId parse(String text) {
        Objects.requireNonNull(text, "text");
        int colon = text.indexOf(':');
        if (colon < 0) {
            return new ContentId(DEFAULT_NAMESPACE, text);
        }
        if (text.indexOf(':', colon + 1) >= 0) {
            throw new IllegalArgumentException("Ungültige ID '" + text + "' (mehr als ein ':')");
        }
        return new ContentId(text.substring(0, colon), text.substring(colon + 1));
    }

    public static ContentId of(String path) {
        return new ContentId(DEFAULT_NAMESPACE, path);
    }

    /** Child id, used for nodes inside a tree or lineages inside a species: {@code ns:path/child}. */
    public ContentId child(String childPath) {
        return new ContentId(namespace, path + "/" + childPath);
    }

    /**
     * Translation key fragment: {@code frosthain:elf/high} becomes {@code frosthain.elf.high}.
     * Used to derive language keys such as {@code species.frosthain.elf}.
     */
    public String toTranslationFragment() {
        return namespace + "." + path.replace('/', '.');
    }

    @Override
    public int compareTo(ContentId other) {
        int byNamespace = namespace.compareTo(other.namespace);
        return byNamespace != 0 ? byNamespace : path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
