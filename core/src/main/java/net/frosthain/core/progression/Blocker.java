package net.frosthain.core.progression;

/**
 * Why an action (allocating a talent, taking a class) is not possible right now.
 * The UI translates {@link Reason} via {@code blocker.frosthain.<reason>} and inserts {@code detail}.
 * This is what makes locked talents explainable ("Benötigt Stufe 5", "Benötigt: Schildwall").
 */
public record Blocker(Reason reason, String detail) {

    public enum Reason {
        UNKNOWN_TREE,
        UNKNOWN_NODE,
        TREE_NOT_ACCESSIBLE,
        ROOT_NODE,
        MAX_RANK_REACHED,
        NOT_ENOUGH_POINTS,
        CHARACTER_LEVEL_TOO_LOW,
        CLASS_LEVEL_TOO_LOW,
        MISSING_PREREQUISITE,
        MISSING_ANY_PREREQUISITE,
        EXCLUSIVE_CONFLICT,
        NOT_ALLOCATED,
        REQUIRED_BY_OTHER_TALENT,
        MULTICLASS_DISABLED,
        UNKNOWN_CLASS,
        ALREADY_HAS_CLASS,
        MAX_CLASSES_REACHED,
        LEVEL_TOO_LOW_FOR_MULTICLASS,
        NO_LEVEL_UP_AVAILABLE,
        MAX_LEVEL_REACHED,
        ABILITY_REQUIREMENT,
        EXCLUSIVE_CLASS
    }

    public static Blocker of(Reason reason) {
        return new Blocker(reason, "");
    }

    public static Blocker of(Reason reason, Object detail) {
        return new Blocker(reason, String.valueOf(detail));
    }
}
