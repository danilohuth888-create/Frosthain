package net.frosthain.core.content;

import net.frosthain.core.ContentId;

import java.util.List;

/** Common contract of every data-driven definition (class, species, talent tree, ...). */
public interface ContentDefinition {

    ContentId id();

    /** Provenance: id of a {@link SourceDefinition} (license catalog entry). Mandatory for every definition. */
    ContentId source();

    /**
     * {@code true} if the content was changed relative to its source (e.g. D&amp;D rules adapted to
     * real-time Minecraft gameplay). CC-BY-4.0 requires indicating such changes.
     */
    boolean adapted();

    /** Language keys this definition needs. The validator checks them against the primary language. */
    List<String> requiredTranslationKeys();
}
