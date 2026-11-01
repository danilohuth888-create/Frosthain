package net.frosthain.prototype;

import net.frosthain.core.ContentId;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serial;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * FROSTHAIN UI prototype: reference images B (HUD), D (talent tree) and F (NPC dialogue) as a desktop window,
 * driven by the real rules from {@code core} and the presentation logic from {@code ui}.
 *
 * <p>{@code --screenshots <dir>} renders fixed scenes to PNG without a display (used for reviews and CI).
 */
public final class PrototypeApp {

    static final int WIDTH = 1600;
    static final int HEIGHT = 900;

    private final PrototypeState state;
    private final Painter painter;
    private final HudScene hud;
    private final TalentScene talents;
    private final DialogueScene dialogue;

    PrototypeApp(Path contentRoot) {
        state = new PrototypeState(contentRoot);
        painter = new Painter(contentRoot);
        WorldBackdrop backdrop = new WorldBackdrop(WIDTH, HEIGHT);
        hud = new HudScene(painter, backdrop);
        talents = new TalentScene(painter);
        dialogue = new DialogueScene(painter);
    }

    public static void main(String[] args) {
        Path contentRoot = Path.of(System.getProperty("frosthain.contentRoot", "content"));
        PrototypeApp app = new PrototypeApp(contentRoot);
        if (args.length == 2 && args[0].equals("--screenshots")) {
            app.screenshots(Path.of(args[1]));
            return;
        }
        SwingUtilities.invokeLater(app::openWindow);
    }

    void render(Graphics2D g) {
        Painter.quality(g);
        switch (state.scene) {
            case HUD -> hud.render(g, state, WIDTH, HEIGHT);
            case TALENTS -> talents.render(g, state, WIDTH, HEIGHT);
            case DIALOGUE -> {
                hud.render(g, state, WIDTH, HEIGHT);
                dialogue.render(g, state, WIDTH, HEIGHT);
            }
        }
        if (state.scene == PrototypeState.Scene.HUD) {
            java.awt.Font hint = painter.font(net.frosthain.ui.Theme.FontRole.BODY_ITALIC, 14f);
            String[] lines = {"Prototyp-Steuerung: K Talente · Rechtsklick/T Eldrin ansprechen",
                    "H Questtracker · C Kiste gefunden · L Stufe +1"};
            for (int i = 0; i < lines.length; i++) {
                painter.text(g, lines[i], hint, 0xCCFFFFFF, WIDTH - painter.width(lines[i], hint) - 14, HEIGHT - 44 + i * 18);
            }
        }
    }

    BufferedImage snapshot() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        render(g);
        g.dispose();
        return image;
    }

    // ---- screenshot mode ------------------------------------------------------------------------------

    private void screenshots(Path dir) {
        try {
            Files.createDirectories(dir);
            save(dir, "B_hud.png");

            state.scene = PrototypeState.Scene.TALENTS;
            snapshot(); // first frame initialises the camera viewport
            state.selectTree(ContentId.of("general"));
            state.selectedNode = "combat_stance";
            save(dir, "D_talente_kampfhaltung.png");
            state.selectedNode = "stalwart";
            talents.selectTab(state, "combat");
            save(dir, "D_talente_gesperrt_reiter_kampf.png");
            talents.selectTab(state, "combat");
            state.selectedNode = "combat_stance";
            state.unlockSelected();
            save(dir, "D_talente_nach_freischalten.png");
            state.selectTree(ContentId.of("runes"));
            state.selectedNode = "frost_bond";
            save(dir, "D_runenpfad.png");

            state.scene = PrototypeState.Scene.HUD;
            state.openDialogue();
            save(dir, "F_dialog.png");
            state.dialogue.select(3);
            save(dir, "F_dialog_auswahl.png");
            state.dialogue.confirm();
            save(dir, "F_dialog_antwort.png");
            state.dialogue.confirm();
            state.closeDialogue();
            save(dir, "B_hud_nach_dialog.png");
            System.out.println("Screenshots geschrieben nach " + dir.toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void save(Path dir, String name) throws IOException {
        ImageIO.write(snapshot(), "png", dir.resolve(name).toFile());
    }

    // ---- interactive window ---------------------------------------------------------------------------

    private void openWindow() {
        JFrame frame = new JFrame("FROSTHAIN – UI-Prototyp (B · D · F)");
        Canvas canvas = new Canvas();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        canvas.requestFocusInWindow();
    }

    private final class Canvas extends JPanel {
        @Serial
        private static final long serialVersionUID = 1L;

        private transient Point dragStart;

        Canvas() {
            setPreferredSize(new Dimension(WIDTH * 4 / 5, HEIGHT * 4 / 5));
            setFocusable(true);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    double[] p = logical(e.getX(), e.getY());
                    dragStart = e.getPoint();
                    switch (state.scene) {
                        case HUD -> {
                            if (SwingUtilities.isRightMouseButton(e)) {
                                state.openDialogue();
                            } else if (hud.trackerToggle.contains(p[0], p[1])) {
                                state.trackerCollapsed = !state.trackerCollapsed;
                            }
                        }
                        case TALENTS -> {
                            if (SwingUtilities.isLeftMouseButton(e)) {
                                talents.click(state, p[0], p[1]);
                            }
                        }
                        case DIALOGUE -> dialogue.click(state, p[0], p[1]);
                    }
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (state.scene == PrototypeState.Scene.TALENTS && dragStart != null) {
                        double scale = scale();
                        double[] p = logical(e.getX(), e.getY());
                        if (talents.inTree(p[0], p[1]) && (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)
                                || SwingUtilities.isLeftMouseButton(e))) {
                            state.camera.panPixels((e.getX() - dragStart.x) / scale, (e.getY() - dragStart.y) / scale);
                        }
                        dragStart = e.getPoint();
                        repaint();
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    if (state.scene == PrototypeState.Scene.DIALOGUE) {
                        double[] p = logical(e.getX(), e.getY());
                        dialogue.hover(state, p[0], p[1]);
                        repaint();
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (state.scene == PrototypeState.Scene.TALENTS) {
                        double[] p = logical(e.getX(), e.getY());
                        if (talents.inTree(p[0], p[1])) {
                            talents.zoom(state, Math.pow(1.12, -e.getPreciseWheelRotation()), p[0], p[1]);
                            repaint();
                        }
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    handleKey(e.getKeyCode());
                    repaint();
                }
            });
        }

        private double scale() {
            return Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        }

        private double[] logical(int x, int y) {
            double scale = scale();
            double ox = (getWidth() - WIDTH * scale) / 2;
            double oy = (getHeight() - HEIGHT * scale) / 2;
            return new double[]{(x - ox) / scale, (y - oy) / scale};
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double scale = scale();
            int w = (int) (WIDTH * scale);
            int h = (int) (HEIGHT * scale);
            g.drawImage(snapshot(), (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
        }
    }

    private void handleKey(int keyCode) {
        switch (state.scene) {
            case HUD -> {
                switch (keyCode) {
                    case KeyEvent.VK_K -> {
                        state.scene = PrototypeState.Scene.TALENTS;
                        state.selectTree(state.selectedTree);
                        state.selectedNode = "combat_stance".equals(state.selectedNode) || state.selectedTree.equals(ContentId.of("general"))
                                ? "combat_stance" : state.selectedNode;
                    }
                    case KeyEvent.VK_T -> state.openDialogue();
                    case KeyEvent.VK_H -> state.trackerCollapsed = !state.trackerCollapsed;
                    case KeyEvent.VK_C -> state.findCrate();
                    case KeyEvent.VK_L -> state.gainLevel();
                    default -> {
                        // not handled
                    }
                }
            }
            case TALENTS -> {
                if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_K) {
                    state.scene = PrototypeState.Scene.HUD;
                } else {
                    talents.key(state, keyCode);
                }
            }
            case DIALOGUE -> dialogue.key(state, keyCode);
        }
    }
}
