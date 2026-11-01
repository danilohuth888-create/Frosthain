package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.JsonObjectReader;

import java.util.List;
import java.util.Optional;

/**
 * Entry of the license catalog. Every piece of content must reference an approved source,
 * otherwise validation fails. This is the technical enforcement of the rule
 * "only implement content whose use is permitted".
 */
public record SourceDefinition(
        ContentId id,
        String title,
        String publisher,
        String license,
        Optional<String> licenseUrl,
        Optional<String> url,
        Status status,
        Optional<String> attribution,
        Optional<String> notes
) implements ContentDefinition {

    public enum Status {
        /** Use is permitted; content may be shipped. */
        APPROVED,
        /** License not yet checked; content referencing it is blocked. */
        PENDING_REVIEW,
        /** Use is not permitted. */
        REJECTED
    }

    public static SourceDefinition parse(ContentId id, JsonObjectReader json) {
        return new SourceDefinition(
                id,
                json.string("title"),
                json.string("publisher"),
                json.string("license"),
                json.optString("license_url"),
                json.optString("url"),
                json.enumValue("status", Status.class),
                json.optString("attribution"),
                json.optString("notes"));
    }

    @Override
    public ContentId source() {
        return id;
    }

    @Override
    public boolean adapted() {
        return false;
    }

    @Override
    public List<String> requiredTranslationKeys() {
        // Legal metadata is kept verbatim (not translated).
        return List.of();
    }
}
