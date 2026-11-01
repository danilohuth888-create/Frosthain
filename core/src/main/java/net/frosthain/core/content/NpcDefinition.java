package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.JsonObjectReader;

import java.util.List;
import java.util.Optional;

/**
 * A named non-player character. Phase 4 adds the entity, appearance and schedule; for now the definition
 * carries what quests and dialogues need: a display name, a title and the dialogue to start.
 */
public record NpcDefinition(ContentId id, ContentId source, boolean adapted, Optional<ContentId> dialogue)
        implements ContentDefinition {

    public static NpcDefinition parse(ContentId id, JsonObjectReader json) {
        return new NpcDefinition(id, json.id("source"), json.bool("adapted", false), json.optId("dialogue"));
    }

    public String translationKey() {
        return "npc." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        return List.of(translationKey(), translationKey() + ".title");
    }
}
