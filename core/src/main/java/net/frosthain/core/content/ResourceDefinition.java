package net.frosthain.core.content;

import net.frosthain.core.ContentId;
import net.frosthain.core.json.ContentParseException;
import net.frosthain.core.json.JsonObjectReader;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A class resource such as arcane energy, stamina or fury. Powers spend resources; classes declare
 * which resource they use. Multiclass characters share pools where the rules say so (see MulticlassRules).
 */
public record ResourceDefinition(
        ContentId id,
        ContentId source,
        boolean adapted,
        String color,
        int baseMaximum,
        double regenerationPerSecond,
        boolean regeneratesInCombat
) implements ContentDefinition {

    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    public static ResourceDefinition parse(ContentId id, JsonObjectReader json) {
        String color = json.string("color");
        if (!HEX_COLOR.matcher(color).matches()) {
            throw new ContentParseException(json.childPath("color"), "Farbe im Format #RRGGBB erwartet");
        }
        double regeneration = json.number("regeneration_per_second", 0.0);
        if (regeneration < 0) {
            throw new ContentParseException(json.childPath("regeneration_per_second"), "darf nicht negativ sein");
        }
        return new ResourceDefinition(id, json.id("source"), json.bool("adapted", false), color,
                json.integerInRange("base_maximum", 0, 0, 10_000), regeneration,
                json.bool("regenerates_in_combat", true));
    }

    public String translationKey() {
        return "resource." + id.toTranslationFragment();
    }

    @Override
    public List<String> requiredTranslationKeys() {
        return List.of(translationKey());
    }
}
