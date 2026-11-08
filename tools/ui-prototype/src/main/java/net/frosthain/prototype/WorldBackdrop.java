package net.frosthain.prototype;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Procedural blocky landscape that stands in for the Minecraft world behind HUD and dialogue.
 * Clearly a placeholder: in the game the real world is visible.
 */
final class WorldBackdrop {

    private final BufferedImage image;
    private final BufferedImage minimap;

    WorldBackdrop(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        minimap = new BufferedImage(48, 48, BufferedImage.TYPE_INT_RGB);
        paintLandscape();
        paintMinimap();
    }

    BufferedImage image() {
        return image;
    }

    BufferedImage minimap() {
        return minimap;
    }

    private void paintLandscape() {
        Graphics2D g = image.createGraphics();
        int w = image.getWidth();
        int h = image.getHeight();
        g.setPaint(new GradientPaint(0, 0, new Color(0x24324F), 0, h * 0.55f, new Color(0xD08A58)));
        g.fillRect(0, 0, w, h);
        Random random = new Random(26_3);
        int block = 16;
        // Three mountain layers, back to front.
        int[][] layers = {{0x3C4A6B, 0x6A7896}, {0x2E3A52, 0x55617A}, {0x223040, 0x3E4A5C}};
        for (int layer = 0; layer < layers.length; layer++) {
            double base = h * (0.38 + layer * 0.07);
            double phase = random.nextDouble() * 10;
            for (int x = 0; x < w; x += block) {
                double t = x / (double) w * (4 + layer * 2) + phase;
                double peak = Math.abs(Math.sin(t)) * (0.22 - layer * 0.05) * h + Math.sin(t * 3.1) * 0.03 * h;
                int top = (int) (base - peak) / block * block;
                g.setColor(new Color(layers[layer][0]));
                g.fillRect(x, top, block, h - top);
                if (layer < 2 && peak > 0.12 * h) {
                    g.setColor(new Color(0xE8EEF5));
                    g.fillRect(x, top, block, block * (2 - layer));
                }
            }
        }
        // Lake and shore.
        int lakeTop = (int) (h * 0.66) / block * block;
        g.setColor(new Color(0x1D3B4A));
        g.fillRect(0, lakeTop, w, h - lakeTop);
        g.setColor(new Color(0x2E5866));
        for (int i = 0; i < 40; i++) {
            g.fillRect(random.nextInt(w / block) * block, lakeTop + random.nextInt((h - lakeTop) / block) * block, block * 3, 2);
        }
        // Forest on both sides with a few warm windows (village).
        for (int x = 0; x < w; x += block) {
            boolean forest = x < w * 0.3 || x > w * 0.72;
            if (!forest) {
                continue;
            }
            int treeHeight = (3 + random.nextInt(6)) * block;
            g.setColor(new Color(random.nextBoolean() ? 0x1E3324 : 0x274430));
            g.fillRect(x, lakeTop - treeHeight, block, treeHeight + block * 4);
        }
        for (int i = 0; i < 6; i++) {
            int hx = (int) (w * 0.3 + i * 50);
            g.setColor(new Color(0x3B2A1E));
            g.fillRect(hx, lakeTop - 48, 40, 48);
            g.setColor(new Color(0x5A3A26));
            g.fillRect(hx - 6, lakeTop - 60, 52, 14);
            g.setColor(new Color(0xF2B661));
            g.fillRect(hx + 14, lakeTop - 36, 10, 10);
        }
        g.dispose();
    }

    private void paintMinimap() {
        Random random = new Random(1284);
        for (int y = 0; y < minimap.getHeight(); y++) {
            for (int x = 0; x < minimap.getWidth(); x++) {
                double n = Math.sin(x * 0.21 + random.nextDouble() * 0.4) + Math.cos(y * 0.17) + Math.sin((x + y) * 0.09);
                int rgb;
                if (n < -0.9) {
                    rgb = 0x2F5F86;
                } else if (n < -0.6) {
                    rgb = 0xC9B98A;
                } else if (n < 1.2) {
                    rgb = random.nextInt(5) == 0 ? 0x3E6B35 : 0x4F7F3D;
                } else {
                    rgb = 0x8A8F96;
                }
                minimap.setRGB(x, y, rgb);
            }
        }
    }
}
