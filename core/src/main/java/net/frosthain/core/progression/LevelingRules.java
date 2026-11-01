package net.frosthain.core.progression;

import net.frosthain.core.ContentId;
import net.frosthain.core.character.CharacterSheet;
import net.frosthain.core.content.AbilityScore;
import net.frosthain.core.content.ClassDefinition;
import net.frosthain.core.content.SubclassDefinition;
import net.frosthain.core.registry.ContentRegistry;
import net.frosthain.core.registry.ContentType;
import net.frosthain.core.rules.ProgressionRules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Level-ups, multiclassing and subclass choice.
 *
 * <p>Experience only makes a level-up <em>available</em>; the player decides in the character menu which
 * class receives the new level (like in classic RPGs). Multiclassing is limited by:
 * <ol>
 *   <li>ability requirements of the current classes <em>and</em> the new class (SRD rule, default 13),</li>
 *   <li>a maximum number of classes and a minimum character level before the first multiclass,</li>
 *   <li>data-driven {@code exclusive_with} lists as a balancing lever,</li>
 *   <li>a shared spellcasting level instead of stacked casting resources ({@link #combinedCasterLevel}).</li>
 * </ol>
 */
public final class LevelingRules {

    private final ContentRegistry registry;
    private final ProgressionRules rules;

    public LevelingRules(ContentRegistry registry) {
        this.registry = registry;
        this.rules = registry.progression().orElseThrow(() -> new IllegalStateException("Fortschrittsregeln fehlen"));
    }

    /** Number of level-ups the character has earned but not yet assigned to a class. */
    public int pendingLevelUps(CharacterSheet sheet) {
        return Math.max(0, rules.levelForExperience(sheet.experience()) - sheet.characterLevel());
    }

    /** Reasons why the next level cannot go into {@code classId}; empty = allowed. */
    public List<Blocker> checkLevelUp(CharacterSheet sheet, ContentId classId) {
        Optional<ClassDefinition> target = registry.get(ContentType.CLASSES, classId);
        if (target.isEmpty()) {
            return List.of(Blocker.of(Blocker.Reason.UNKNOWN_CLASS, classId));
        }
        List<Blocker> blockers = new ArrayList<>();
        if (sheet.characterLevel() >= rules.maxLevel()) {
            blockers.add(Blocker.of(Blocker.Reason.MAX_LEVEL_REACHED, rules.maxLevel()));
        }
        if (pendingLevelUps(sheet) == 0) {
            blockers.add(Blocker.of(Blocker.Reason.NO_LEVEL_UP_AVAILABLE));
        }
        if (sheet.classLevel(classId) == 0) {
            blockers.addAll(checkNewClass(sheet, target.get()));
        }
        return List.copyOf(blockers);
    }

    /** Multiclass-specific checks for adding a class the character does not have yet. */
    public List<Blocker> checkNewClass(CharacterSheet sheet, ClassDefinition newClass) {
        List<Blocker> blockers = new ArrayList<>();
        ProgressionRules.Multiclass multiclass = rules.multiclass();
        if (sheet.classLevel(newClass.id()) > 0) {
            return List.of(Blocker.of(Blocker.Reason.ALREADY_HAS_CLASS, newClass.id()));
        }
        if (!multiclass.enabled()) {
            return List.of(Blocker.of(Blocker.Reason.MULTICLASS_DISABLED));
        }
        if (sheet.classLevels().size() >= multiclass.maxClasses()) {
            blockers.add(Blocker.of(Blocker.Reason.MAX_CLASSES_REACHED, multiclass.maxClasses()));
        }
        if (sheet.characterLevel() < multiclass.minimumLevelBeforeMulticlass()) {
            blockers.add(Blocker.of(Blocker.Reason.LEVEL_TOO_LOW_FOR_MULTICLASS, multiclass.minimumLevelBeforeMulticlass()));
        }
        if (!meetsRequirements(sheet, newClass)) {
            blockers.add(Blocker.of(Blocker.Reason.ABILITY_REQUIREMENT, newClass.id()));
        }
        for (ContentId existing : sheet.classLevels().keySet()) {
            registry.get(ContentType.CLASSES, existing).ifPresent(current -> {
                if (!meetsRequirements(sheet, current)) {
                    blockers.add(Blocker.of(Blocker.Reason.ABILITY_REQUIREMENT, current.id()));
                }
                if (current.exclusiveWith().contains(newClass.id()) || newClass.exclusiveWith().contains(current.id())) {
                    blockers.add(Blocker.of(Blocker.Reason.EXCLUSIVE_CLASS, current.id()));
                }
            });
        }
        return blockers;
    }

    /** True if any of the class' alternative requirement sets is fully met. No requirements = always met. */
    public static boolean meetsRequirements(CharacterSheet sheet, ClassDefinition clazz) {
        if (clazz.multiclassRequirements().isEmpty()) {
            return true;
        }
        for (Map<AbilityScore, Integer> alternative : clazz.multiclassRequirements()) {
            boolean met = alternative.entrySet().stream().allMatch(e -> sheet.abilityScore(e.getKey()) >= e.getValue());
            if (met) {
                return true;
            }
        }
        return false;
    }

    public CharacterSheet levelUp(CharacterSheet sheet, ContentId classId) {
        List<Blocker> blockers = checkLevelUp(sheet, classId);
        if (!blockers.isEmpty()) {
            throw new IllegalStateException("Stufenaufstieg in " + classId + " nicht möglich: " + blockers);
        }
        Map<ContentId, Integer> levels = new LinkedHashMap<>(sheet.classLevels());
        levels.merge(classId, 1, Integer::sum);
        return sheet.withClassLevels(levels);
    }

    /** Whether the class has reached its subclass level and no subclass was chosen yet. */
    public boolean needsSubclassChoice(CharacterSheet sheet, ContentId classId) {
        return registry.get(ContentType.CLASSES, classId)
                .map(clazz -> sheet.classLevel(classId) >= clazz.subclassLevel() && !sheet.subclasses().containsKey(classId))
                .orElse(false);
    }

    public CharacterSheet chooseSubclass(CharacterSheet sheet, ContentId subclassId) {
        SubclassDefinition subclass = registry.get(ContentType.SUBCLASSES, subclassId)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannte Unterklasse " + subclassId));
        if (!needsSubclassChoice(sheet, subclass.parentClass())) {
            throw new IllegalStateException("Für " + subclass.parentClass() + " ist gerade keine Unterklassenwahl möglich");
        }
        Map<ContentId, ContentId> subclasses = new TreeMap<>(sheet.subclasses());
        subclasses.put(subclass.parentClass(), subclassId);
        return sheet.withSubclasses(subclasses);
    }

    /**
     * Shared spellcasting level across all classes (full casters count fully, half casters half, third
     * casters a third, rounded down per class). Casting resources scale with this value instead of
     * stacking per class, which keeps caster multiclass builds in check.
     */
    public int combinedCasterLevel(CharacterSheet sheet) {
        int total = 0;
        for (Map.Entry<ContentId, Integer> entry : sheet.classLevels().entrySet()) {
            Optional<ClassDefinition> clazz = registry.get(ContentType.CLASSES, entry.getKey());
            if (clazz.isPresent()) {
                ClassDefinition.CasterProgression progression = clazz.get().casterProgression();
                total += entry.getValue() * progression.numerator() / progression.denominator();
            }
        }
        return total;
    }
}
