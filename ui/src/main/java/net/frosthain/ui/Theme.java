package net.frosthain.ui;

import net.frosthain.core.content.TalentTreeDefinition;

import java.util.List;

/**
 * Design tokens derived from the reference images (docs/10-ui-implementierungsplan.md §10.3).
 * Colors are ARGB integers so that both Java2D and Minecraft's GUI rendering can use them directly.
 */
public final class Theme {

    private Theme() {
    }

    public static final int PANEL_BACKGROUND = 0xEB0E1013;
    public static final int HUD_PANEL_BACKGROUND = 0xC70E1013;
    public static final int PANEL_INNER = 0xFF16191E;
    public static final int FRAME_GOLD = 0xFFB8955A;
    public static final int FRAME_GOLD_BRIGHT = 0xFFE6C27A;
    public static final int FRAME_GOLD_DIM = 0xFF6B5836;
    public static final int TEXT_PRIMARY = 0xFFEDE3CF;
    public static final int TEXT_SECONDARY = 0xFFB9AE98;
    public static final int TEXT_DISABLED = 0xFF6F6A60;
    public static final int STATE_AVAILABLE = 0xFF6FD15A;
    public static final int STATE_BLOCKED = 0xFFD9594C;
    public static final int NODE_LOCKED = 0xFF3A3D42;

    public static final int BRANCH_COMBAT = 0xFFE0452E;
    public static final int BRANCH_MAGIC = 0xFF3BA4F0;
    public static final int BRANCH_CRAFTING = 0xFFB25FE0;
    public static final int BRANCH_SURVIVAL = 0xFF6DC54A;
    public static final int BRANCH_RUNE = 0xFF7FD3E6;

    /** Branch palette by branch index; matches reference image D (red, blue, violet, green). */
    private static final List<Integer> GENERAL_PALETTE = List.of(BRANCH_COMBAT, BRANCH_MAGIC, BRANCH_CRAFTING, BRANCH_SURVIVAL);
    private static final List<Integer> RUNE_PALETTE = List.of(0xFFE6C27A, BRANCH_RUNE, 0xFF9B7BFF);

    public enum FontRole {
        /** Cinzel – titles, names, tabs. */
        HEADING,
        /** EB Garamond – descriptions, dialogue. */
        BODY,
        /** EB Garamond Italic – flavour text and hints. */
        BODY_ITALIC
    }

    /** Color of a branch by its index in the tree's branch list. Unknown branches (or none) are gold. */
    public static int branchColor(TalentTreeDefinition.Category category, int branchIndex) {
        List<Integer> palette = category == TalentTreeDefinition.Category.RUNE ? RUNE_PALETTE : GENERAL_PALETTE;
        if (branchIndex < 0) {
            return FRAME_GOLD_BRIGHT;
        }
        return palette.get(branchIndex % palette.size());
    }

    /** Multiplies the alpha channel, e.g. for dimmed (non-focused) branches. */
    public static int withAlpha(int argb, double factor) {
        int alpha = (int) Math.round(((argb >>> 24) & 0xFF) * Math.clamp(factor, 0.0, 1.0));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /** Linear blend of two colors (alpha included); t = 0 gives a, t = 1 gives b. */
    public static int mix(int a, int b, double t) {
        double f = Math.clamp(t, 0.0, 1.0);
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            result |= ((int) Math.round(ca + (cb - ca) * f) & 0xFF) << shift;
        }
        return result;
    }
}
