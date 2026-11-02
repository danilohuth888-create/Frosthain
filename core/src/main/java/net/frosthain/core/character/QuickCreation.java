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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds a complete creation request from the four choices of the in-game editor (species, class, background, name).
 *
 * <p>Ability scores use the standard array, highest values on the class's primary abilities; the background bonus
 * is +2/+1 on the allowed abilities that fit the class best. Appearance starts at the species default. The result
 * still goes through {@link CharacterFactory}, so the server validates it like any other request.
 */
public final class QuickCreation {

    /** Order for abilities that are not primary for the class: survivability first, then common secondaries. */
    private static final List<AbilityScore> FALLBACK_ORDER = List.of(AbilityScore.CONSTITUTION, AbilityScore.DEXTERITY,
            AbilityScore.WISDOM, AbilityScore.STRENGTH, AbilityScore.CHARISMA, AbilityScore.INTELLIGENCE);

    private QuickCreation() {
    }

    /** @throws IllegalArgumentException if species, class or background are unknown */
    public static CharacterFactory.CreationRequest request(ContentRegistry registry, String name, ContentId species,
                                                           ContentId characterClass, ContentId background) {
        return request(registry, name, species, characterClass, background, Optional.empty());
    }

    /** Same, with the appearance chosen in the editor (empty = species default). */
    public static CharacterFactory.CreationRequest request(ContentRegistry registry, String name, ContentId species,
                                                           ContentId characterClass, ContentId background,
                                                           Optional<Appearance> appearance) {
        SpeciesDefinition s = registry.get(ContentType.SPECIES, species)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannte Spezies " + species));
        ClassDefinition c = registry.get(ContentType.CLASSES, characterClass)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannte Klasse " + characterClass));
        BackgroundDefinition b = registry.get(ContentType.BACKGROUNDS, background)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Hintergrund " + background));
        AbilityScoreRules rules = registry.abilityScoreRules()
                .orElseThrow(() -> new IllegalStateException("Attributsregeln fehlen"));
        Optional<String> lineage = s.lineageRequired() && !s.lineages().isEmpty()
                ? Optional.of(s.lineages().getFirst().key())
                : Optional.empty();
        return new CharacterFactory.CreationRequest(name, species, lineage, characterClass, background,
                AbilityScoreRules.Method.STANDARD_ARRAY, baseScores(rules.standardArray(), c.primaryAbilities()),
                backgroundBonus(b.abilityOptions(), c.primaryAbilities()), appearance.orElseGet(() -> Appearance.defaultFor(s)),
                CharacterSheet.Biography.EMPTY);
    }

    /** Standard array, highest values first on the primary abilities, then in {@link #FALLBACK_ORDER}. */
    public static Map<AbilityScore, Integer> baseScores(List<Integer> standardArray, List<AbilityScore> primary) {
        List<Integer> values = new ArrayList<>(standardArray);
        values.sort((a, b) -> Integer.compare(b, a));
        List<AbilityScore> order = priority(primary);
        Map<AbilityScore, Integer> scores = new EnumMap<>(AbilityScore.class);
        for (int i = 0; i < order.size(); i++) {
            scores.put(order.get(i), values.get(i));
        }
        return scores;
    }

    /** +2 on the best-fitting allowed ability, +1 on the next one. */
    public static Map<AbilityScore, Integer> backgroundBonus(List<AbilityScore> allowed, List<AbilityScore> primary) {
        List<AbilityScore> ranked = priority(primary).stream().filter(allowed::contains).toList();
        Map<AbilityScore, Integer> bonus = new EnumMap<>(AbilityScore.class);
        if (!ranked.isEmpty()) {
            bonus.put(ranked.get(0), 2);
        }
        if (ranked.size() > 1) {
            bonus.put(ranked.get(1), 1);
        }
        return bonus;
    }

    private static List<AbilityScore> priority(List<AbilityScore> primary) {
        List<AbilityScore> order = new ArrayList<>(primary);
        for (AbilityScore ability : FALLBACK_ORDER) {
            if (!order.contains(ability)) {
                order.add(ability);
            }
        }
        return order;
    }
}
