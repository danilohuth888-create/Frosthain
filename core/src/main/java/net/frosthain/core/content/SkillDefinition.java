package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.JsonObjectReader;

import java.util.List;

/** A skill (Athletik, Heimlichkeit, ...) used for proficiencies and dialogue checks. */
public record SkillDefinition(ContentId id, ContentId source, boolean adapted, AbilityScore ability)
        implements ContentDefinition {

    public static SkillDefinition parse(ContentId id, JsonObjectReader json) {
        return new SkillDefinition(id, json.id("source"), json.bool("adapted", false),
                json.enumValue("ability", AbilityScore.class));
    }

    public String translationKey() {
        return "skill." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        return List.of(translationKey());
    }
}
