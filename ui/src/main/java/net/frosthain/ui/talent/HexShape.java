package net.frosthain.ui.talent;

/** Flat-topped hexagon used for talent nodes (reference image D). */
public final class HexShape {

    private static final double SQRT3 = Math.sqrt(3);

    private HexShape() {
    }

    /** Corner points (x0, y0, x1, y1, …) of a flat-topped hexagon around (cx, cy) with circumradius r. */
    public static double[] corners(double cx, double cy, double r) {
        double[] points = new double[12];
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i);
            points[2 * i] = cx + r * Math.cos(angle);
            points[2 * i + 1] = cy + r * Math.sin(angle);
        }
        return points;
    }

    public static boolean contains(double cx, double cy, double r, double px, double py) {
        double dx = Math.abs(px - cx);
        double dy = Math.abs(py - cy);
        return dx <= r && dy <= r * SQRT3 / 2 && SQRT3 * dx + dy <= SQRT3 * r;
    }
}
