package net.frosthain.core.character;

import net.frosthain.core.ContentId;
import net.frosthain.core.content.AbilityScore;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Persistent RPG state of one character. Immutable; every change produces a new sheet, which makes
 * server-side validation and undo/confirmation dialogs straightforward.
 *
 * @param classLevels   levels per class in the order the classes were taken (first entry = starting class)
 * @param subclasses    chosen subclass per class
 * @param experience    total FROSTHAIN experience (independent from vanilla XP)
 * @param talents       allocated rank per talent node
 */
public record CharacterSheet(
        UUID characterId,
        String name,
        ContentId species,
        Optional<String> lineage,
        ContentId background,
        Map<AbilityScore, Integer> abilityScores,
        Map<ContentId, Integer> classLevels,
        Map<ContentId, ContentId> subclasses,
        long experience,
        Map<TalentRef, Integer> talents,
        Appearance appearance,
        Biography biography
) {

    /** Free-form role-play information. Optional and never required by any rule. */
    public record Biography(String text, List<String> personalityTraits) {
        public static final int MAX_TEXT_LENGTH = 4000;
        public static final Biography EMPTY = new Biography("", List.of());

        public Biography {
            personalityTraits = List.copyOf(personalityTraits);
        }
    }

    public CharacterSheet {
        abilityScores = Collections.unmodifiableMap(new EnumMap<>(abilityScores));
        classLevels = Collections.unmodifiableMap(new LinkedHashMap<>(classLevels));
        subclasses = Collections.unmodifiableMap(new TreeMap<>(subclasses));
        talents = Collections.unmodifiableMap(new TreeMap<>(talents));
        if (experience < 0) {
            throw new IllegalArgumentException("negative Erfahrung");
        }
    }

    public int characterLevel() {
        return classLevels.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int classLevel(ContentId classId) {
        return classLevels.getOrDefault(classId, 0);
    }

    public ContentId startingClass() {
        return classLevels.keySet().iterator().next();
    }

    public int abilityScore(AbilityScore ability) {
        return abilityScores.getOrDefault(ability, 10);
    }

    public CharacterSheet withTalents(Map<TalentRef, Integer> newTalents) {
        return new CharacterSheet(characterId, name, species, lineage, background, abilityScores, classLevels, subclasses,
                experience, newTalents, appearance, biography);
    }

    public CharacterSheet withClassLevels(Map<ContentId, Integer> newClassLevels) {
        return new CharacterSheet(characterId, name, species, lineage, background, abilityScores, newClassLevels, subclasses,
                experience, talents, appearance, biography);
    }

    public CharacterSheet withSubclasses(Map<ContentId, ContentId> newSubclasses) {
        return new CharacterSheet(characterId, name, species, lineage, background, abilityScores, classLevels, newSubclasses,
                experience, talents, appearance, biography);
    }

    public CharacterSheet withExperience(long newExperience) {
        return new CharacterSheet(characterId, name, species, lineage, background, abilityScores, classLevels, subclasses,
                newExperience, talents, appearance, biography);
    }

    public CharacterSheet withAppearance(Appearance newAppearance) {
        return new CharacterSheet(characterId, name, species, lineage, background, abilityScores, classLevels, subclasses,
                experience, talents, newAppearance, biography);
    }
}
