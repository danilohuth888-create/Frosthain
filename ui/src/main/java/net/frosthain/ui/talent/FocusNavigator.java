package net.frosthain.ui.talent;

import java.util.List;
import java.util.Optional;

/** Keyboard/controller navigation: arrow keys move the focus to the best node in that direction. */
public final class FocusNavigator {

    public enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        final double dx;
        final double dy;

        Direction(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    /** Maximum angle between the arrow direction and the candidate, in degrees. */
    private static final double MAX_ANGLE = 67.5;

    private FocusNavigator() {
    }

    public static Optional<TalentTreeView.NodeView> next(List<TalentTreeView.NodeView> nodes, TalentTreeView.NodeView from,
                                                         Direction direction) {
        double cosLimit = Math.cos(Math.toRadians(MAX_ANGLE));
        TalentTreeView.NodeView best = null;
        double bestScore = Double.MAX_VALUE;
        for (TalentTreeView.NodeView candidate : nodes) {
            if (candidate.key().equals(from.key())) {
                continue;
            }
            double vx = candidate.x() - from.x();
            double vy = candidate.y() - from.y();
            double distance = Math.hypot(vx, vy);
            double cos = (vx * direction.dx + vy * direction.dy) / distance;
            if (cos < cosLimit) {
                continue;
            }
            // Prefer close nodes that lie straight in the arrow direction.
            double score = distance * (1 + 2 * (1 - cos));
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }
}
