package net.frosthain.ui.talent;

/** Visual state of a talent node (UI spec D: locked, available, unlocked – plus a few finer distinctions). */
public enum NodeState {
    /** Root node of an accessible tree. */
    ORIGIN,
    /** Prerequisites, level, class or an exclusive choice prevent it. Shown grey with a lock. */
    LOCKED,
    /** Everything is met except free talent points. Shown in branch colour, without lock. */
    REACHABLE,
    /** Can be unlocked right now. */
    AVAILABLE,
    /** At least one rank allocated, more ranks possible. */
    UNLOCKED,
    /** Maximum rank reached. */
    MAXED;

    public boolean isAllocated() {
        return this == ORIGIN || this == UNLOCKED || this == MAXED;
    }
}
