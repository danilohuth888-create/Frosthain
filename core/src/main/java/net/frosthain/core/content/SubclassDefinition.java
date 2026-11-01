package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.JsonObjectReader;

import java.util.List;
import java.util.Optional;

/** A subclass / specialisation, chosen at the parent class' {@code subclass_level}. */
public record SubclassDefinition(
        ContentId id,
        ContentId source,
        boolean adapted,
        ContentId parentClass,
        Optional<ContentId> talentTree
) implements ContentDefinition {

    public static SubclassDefinition parse(ContentId id, JsonObjectReader json) {
        return new SubclassDefinition(id, json.id("source"), json.bool("adapted", false),
                json.id("class"), json.optId("talent_tree"));
    }

    public String translationKey() {
        return "subclass." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        return List.of(translationKey(), translationKey() + ".desc");
    }
}
