package net.frosthain.prototype;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Simple vector icons for talent nodes in the prototype. In the game these become textures
 * ({@code assets/frosthain/textures/gui/icon/<name>.png}); the icon ids in the content stay the same.
 */
final class Glyphs {

    private Glyphs() {
    }

    /** Draws icon {@code name} centred at (cx, cy) inside a box of half-size {@code r}. */
    static void draw(Graphics2D g, String name, double cx, double cy, double r, Color color) {
        AffineTransform saved = g.getTransform();
        g.translate(cx, cy);
        g.scale(r, r);
        g.setColor(color);
        g.setStroke(new BasicStroke(0.14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (name) {
            case "swords" -> {
                g.draw(line(-0.75, 0.75, 0.75, -0.75));
                g.draw(line(0.75, 0.75, -0.75, -0.75));
                g.draw(line(-0.75, 0.35, -0.35, 0.75));
                g.draw(line(0.75, 0.35, 0.35, 0.75));
            }
            case "sword" -> {
                g.draw(line(-0.6, 0.6, 0.7, -0.7));
                g.draw(line(-0.75, 0.25, -0.25, 0.75));
                g.draw(line(-0.75, 0.75, -0.55, 0.55));
            }
            case "shield" -> g.draw(path(new double[]{0, -0.85, 0.7, -0.6, 0.6, 0.2, 0, 0.85, -0.6, 0.2, -0.7, -0.6}, true));
            case "heart" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(0, 0.8);
                p.curveTo(-1.0, 0.0, -0.7, -0.85, 0, -0.35);
                p.curveTo(0.7, -0.85, 1.0, 0.0, 0, 0.8);
                g.draw(p);
            }
            case "boot" -> g.draw(path(new double[]{-0.4, -0.8, 0.1, -0.8, 0.1, 0.2, 0.75, 0.4, 0.75, 0.75, -0.4, 0.75}, true));
            case "flame" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(0, 0.85);
                p.curveTo(-0.8, 0.6, -0.6, -0.1, -0.1, -0.85);
                p.curveTo(0.0, -0.3, 0.6, -0.2, 0.5, 0.3);
                p.curveTo(0.5, 0.65, 0.3, 0.8, 0, 0.85);
                g.draw(p);
            }
            case "eye" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(-0.85, 0);
                p.quadTo(0, -0.75, 0.85, 0);
                p.quadTo(0, 0.75, -0.85, 0);
                g.draw(p);
                g.fill(new Ellipse2D.Double(-0.22, -0.22, 0.44, 0.44));
            }
            case "bolt" -> g.draw(path(new double[]{0.2, -0.85, -0.45, 0.1, 0.05, 0.1, -0.2, 0.85, 0.5, -0.15, 0.0, -0.15}, true));
            case "rune" -> {
                g.draw(line(0, -0.85, 0, 0.85));
                g.draw(line(0, -0.45, 0.55, -0.85));
                g.draw(line(0, -0.05, 0.55, -0.45));
                g.draw(line(0, 0.25, -0.5, -0.2));
            }
            case "star" -> g.draw(star(5, 0.85, 0.38));
            case "drop" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(0, -0.85);
                p.curveTo(0.5, -0.2, 0.65, 0.15, 0.6, 0.35);
                p.curveTo(0.5, 0.85, -0.5, 0.85, -0.6, 0.35);
                p.curveTo(-0.65, 0.15, -0.5, -0.2, 0, -0.85);
                g.draw(p);
            }
            case "crystal", "gem" -> {
                g.draw(path(new double[]{-0.5, -0.4, -0.25, -0.75, 0.25, -0.75, 0.5, -0.4, 0, 0.85}, true));
                g.draw(line(-0.5, -0.4, 0.5, -0.4));
                g.draw(line(-0.1, -0.4, 0, 0.85));
            }
            case "pickaxe" -> {
                g.draw(line(-0.55, 0.75, 0.35, -0.15));
                Path2D head = new Path2D.Double();
                head.moveTo(-0.3, -0.75);
                head.quadTo(0.55, -0.6, 0.75, 0.25);
                g.draw(head);
            }
            case "hammer" -> {
                g.draw(line(-0.6, 0.75, 0.15, 0.0));
                g.draw(path(new double[]{-0.15, -0.55, 0.3, -0.85, 0.8, -0.35, 0.5, -0.05}, true));
            }
            case "hourglass" -> {
                g.draw(line(-0.55, -0.8, 0.55, -0.8));
                g.draw(line(-0.55, 0.8, 0.55, 0.8));
                g.draw(path(new double[]{-0.45, -0.8, 0.45, 0.8, -0.45, 0.8, 0.45, -0.8}, true));
            }
            case "crown" -> g.draw(path(new double[]{-0.75, 0.55, -0.75, -0.45, -0.35, 0.0, 0, -0.65, 0.35, 0.0, 0.75, -0.45, 0.75, 0.55}, true));
            case "leaf" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(-0.7, 0.7);
                p.curveTo(-0.7, -0.4, 0.2, -0.8, 0.75, -0.75);
                p.curveTo(0.8, -0.2, 0.4, 0.7, -0.7, 0.7);
                g.draw(p);
                g.draw(line(-0.7, 0.7, 0.3, -0.3));
            }
            case "wave" -> {
                for (double y : new double[]{-0.35, 0.25}) {
                    Path2D p = new Path2D.Double();
                    p.moveTo(-0.85, y);
                    p.quadTo(-0.42, y - 0.35, 0, y);
                    p.quadTo(0.42, y + 0.35, 0.85, y);
                    g.draw(p);
                }
            }
            case "paw" -> {
                g.fill(new Ellipse2D.Double(-0.4, 0.0, 0.8, 0.65));
                for (double[] toe : new double[][]{{-0.7, -0.35}, {-0.28, -0.75}, {0.28, -0.75}, {0.7, -0.35}}) {
                    g.fill(new Ellipse2D.Double(toe[0] - 0.17, toe[1] - 0.17, 0.34, 0.34));
                }
            }
            case "mountain" -> {
                g.draw(path(new double[]{-0.9, 0.7, -0.2, -0.6, 0.15, 0.0, 0.4, -0.3, 0.9, 0.7}, true));
                g.draw(line(-0.45, -0.15, -0.2, -0.3));
            }
            case "book" -> {
                g.draw(path(new double[]{0, -0.6, -0.8, -0.75, -0.8, 0.6, 0, 0.75}, false));
                g.draw(path(new double[]{0, -0.6, 0.8, -0.75, 0.8, 0.6, 0, 0.75}, false));
                g.draw(line(0, -0.6, 0, 0.75));
            }
            case "circle" -> {
                g.draw(new Ellipse2D.Double(-0.75, -0.75, 1.5, 1.5));
                g.draw(star(6, 0.5, 0.5));
            }
            case "snowflake" -> {
                for (int i = 0; i < 3; i++) {
                    double a = Math.toRadians(60 * i + 90);
                    g.draw(line(-0.85 * Math.cos(a), -0.85 * Math.sin(a), 0.85 * Math.cos(a), 0.85 * Math.sin(a)));
                }
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(60 * i + 90);
                    double bx = 0.55 * Math.cos(a);
                    double by = 0.55 * Math.sin(a);
                    g.draw(line(bx, by, bx + 0.22 * Math.cos(a + 0.8), by + 0.22 * Math.sin(a + 0.8)));
                    g.draw(line(bx, by, bx + 0.22 * Math.cos(a - 0.8), by + 0.22 * Math.sin(a - 0.8)));
                }
            }
            case "horn" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(-0.8, -0.3);
                p.curveTo(-0.2, -0.3, 0.4, 0.0, 0.8, 0.6);
                p.lineTo(0.5, 0.75);
                p.curveTo(0.1, 0.25, -0.3, 0.2, -0.8, 0.25);
                p.closePath();
                g.draw(p);
            }
            case "tree" -> {
                g.draw(line(0, 0.85, 0, -0.1));
                g.draw(line(0, 0.3, -0.45, -0.15));
                g.draw(line(0, 0.3, 0.45, -0.15));
                g.draw(new Ellipse2D.Double(-0.78, -0.95, 1.56, 1.05));
                g.draw(line(0, 0.85, -0.45, 0.85));
                g.draw(line(0, 0.85, 0.45, 0.85));
            }
            case "lock" -> {
                g.draw(new Arc2D.Double(-0.38, -0.8, 0.76, 0.8, 0, 180, Arc2D.OPEN));
                g.draw(line(-0.38, -0.4, -0.38, -0.1));
                g.draw(line(0.38, -0.4, 0.38, -0.1));
                g.fill(new Rectangle2D.Double(-0.55, -0.1, 1.1, 0.85));
            }
            default -> g.draw(new Ellipse2D.Double(-0.6, -0.6, 1.2, 1.2));
        }
        g.setTransform(saved);
    }

    private static Shape line(double x1, double y1, double x2, double y2) {
        Path2D p = new Path2D.Double();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        return p;
    }

    private static Shape path(double[] points, boolean close) {
        Path2D p = new Path2D.Double();
        p.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) {
            p.lineTo(points[i], points[i + 1]);
        }
        if (close) {
            p.closePath();
        }
        return p;
    }

    static Shape star(int spikes, double outer, double inner) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < spikes * 2; i++) {
            double r = i % 2 == 0 ? outer : inner;
            double a = Math.PI * i / spikes - Math.PI / 2;
            if (i == 0) {
                p.moveTo(r * Math.cos(a), r * Math.sin(a));
            } else {
                p.lineTo(r * Math.cos(a), r * Math.sin(a));
            }
        }
        p.closePath();
        return p;
    }
}
