package net.frosthain.core.character;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.AbilityScore;
import net.frosthain.core.content.BackgroundDefinition;
import net.frosthain.core.content.ClassDefinition;
import net.frosthain.core.content.SpeciesDefinition;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.core.rules.AbilityScoreRules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Validates the choices of the character editor and creates the initial {@link CharacterSheet}.
 * The same validation runs on the (integrated) server, so a modified client cannot create invalid characters.
 */
public final class CharacterFactory {

    /** Everything the character editor collects before entering the world. */
    public record CreationRequest(
            String name,
            ContentId species,
            Optional<String> lineage,
            ContentId characterClass,
            ContentId background,
            AbilityScoreRules.Method method,
            Map<AbilityScore, Integer> baseScores,
            Map<AbilityScore, Integer> backgroundBonus,
            Appearance appearance,
            CharacterSheet.Biography biography
    ) {
    }

    public sealed interface Result {
        record Created(CharacterSheet sheet) implements Result {
        }

        record Rejected(List<String> problems) implements Result {
        }
    }

    public static final int MAX_NAME_LENGTH = 32;
    /** Letters (incl. umlauts/accents), spaces, apostrophes and hyphens. */
    private static final Pattern NAME = Pattern.compile("[\\p{L}][\\p{L} '\\-]*");

    private final ContentRegistry registry;

    public CharacterFactory(ContentRegistry registry) {
        this.registry = registry;
    }

    public Result create(CreationRequest request, UUID characterId) {
        List<String> problems = new ArrayList<>();
        String name = request.name() == null ? "" : request.name().strip();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH || !NAME.matcher(name).matches()) {
            problems.add("Name muss 1–" + MAX_NAME_LENGTH + " Zeichen lang sein und darf nur Buchstaben, Leerzeichen, ' und - enthalten.");
        }

        Optional<SpeciesDefinition> species = registry.get(ContentType.SPECIES, request.species());
        if (species.isEmpty()) {
            problems.add("Unbekannte Spezies " + request.species());
        } else {
            SpeciesDefinition s = species.get();
            if (request.lineage().isPresent() && s.lineage(request.lineage().get()).isEmpty()) {
                problems.add("Abstammungslinie '" + request.lineage().get() + "' gibt es für diese Spezies nicht.");
            }
            if (s.lineageRequired() && request.lineage().isEmpty()) {
                problems.add("Für diese Spezies muss eine Abstammungslinie gewählt werden.");
            }
            problems.addAll(request.appearance().validate(s));
        }

        Optional<ClassDefinition> clazz = registry.get(ContentType.CLASSES, request.characterClass());
        if (clazz.isEmpty()) {
            problems.add("Unbekannte Klasse " + request.characterClass());
        }

        AbilityScoreRules rules = registry.abilityScoreRules().orElseThrow(() -> new IllegalStateException("Attributsregeln fehlen"));
        problems.addAll(rules.validateBaseScores(request.method(), request.baseScores()));
        Optional<BackgroundDefinition> background = registry.get(ContentType.BACKGROUNDS, request.background());
        if (background.isEmpty()) {
            problems.add("Unbekannter Hintergrund " + request.background());
        } else {
            problems.addAll(rules.validateBackgroundBonus(background.get().abilityOptions(), request.backgroundBonus()));
        }

        if (request.biography().text().length() > CharacterSheet.Biography.MAX_TEXT_LENGTH) {
            problems.add("Biografie ist länger als " + CharacterSheet.Biography.MAX_TEXT_LENGTH + " Zeichen.");
        }

        if (!problems.isEmpty()) {
            return new Result.Rejected(List.copyOf(problems));
        }
        Map<ContentId, Integer> classLevels = new LinkedHashMap<>();
        classLevels.put(request.characterClass(), 1);
        CharacterSheet sheet = new CharacterSheet(characterId, name, request.species(), request.lineage(), request.background(),
                rules.applyBonus(request.baseScores(), request.backgroundBonus()), classLevels, Map.of(), 0L, Map.of(),
                request.appearance(), request.biography());
        return new Result.Created(sheet);
    }
}
