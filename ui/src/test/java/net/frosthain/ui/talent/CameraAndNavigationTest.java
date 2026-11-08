package net.frosthain.ui.talent;

import net.frosthain.core.ContentId;
import net.frosthain.core.progression.TalentAllocator;
import net.frosthain.ui.UiTestContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraAndNavigationTest {

    @Test
    void zoomKeepsThePointUnderTheCursorFixed() {
        Camera camera = new Camera(20, 200, 80);
        camera.setViewport(800, 600);
        double worldX = camera.toWorldX(600);
        double worldY = camera.toWorldY(150);
        camera.zoomAt(1.5, 600, 150);
        assertEquals(120, camera.zoom(), 1e-9);
        assertEquals(600, camera.toScreenX(worldX), 1e-9);
        assertEquals(150, camera.toScreenY(worldY), 1e-9);
    }

    @Test
    void zoomIsClampedAndFitCentersTheTree() {
        Camera camera = new Camera(20, 200, 80);
        camera.setViewport(800, 600);
        camera.zoomAt(100, 0, 0);
        assertEquals(200, camera.zoom(), 1e-9);
        camera.fit(-3, -3, 3, 3, 1);
        assertEquals(600.0 / 8, camera.zoom(), 1e-9);
        assertEquals(400, camera.toScreenX(0), 1e-9);
        assertEquals(300, camera.toScreenY(0), 1e-9);
    }

    @Test
    void panMovesTheView() {
        Camera camera = new Camera(20, 200, 100);
        camera.setViewport(800, 600);
        camera.panPixels(100, -50);
        assertEquals(500, camera.toScreenX(0), 1e-9);
        assertEquals(250, camera.toScreenY(0), 1e-9);
    }

    @Test
    void arrowKeysMoveToTheNodeInThatDirection() {
        TalentTreeView view = new TalentTreeView(UiTestContent.registry(), new TalentAllocator(UiTestContent.registry()),
                UiTestContent.german(), UiTestContent.character("fighter"), ContentId.of("general"));
        List<TalentTreeView.NodeView> nodes = view.nodes();
        TalentTreeView.NodeView stance = view.node("combat_stance").orElseThrow();
        assertEquals("toughness", FocusNavigator.next(nodes, stance, FocusNavigator.Direction.LEFT).orElseThrow().key());
        assertEquals("dodge", FocusNavigator.next(nodes, stance, FocusNavigator.Direction.UP).orElseThrow().key());
        TalentTreeView.NodeView bloodlust = view.node("bloodlust").orElseThrow();
        assertTrue(FocusNavigator.next(nodes, bloodlust, FocusNavigator.Direction.UP).isEmpty(), "am Rand bleibt der Fokus stehen");
    }

    @Test
    void hexHitTest() {
        assertTrue(HexShape.contains(0, 0, 10, 9.5, 0));
        assertTrue(HexShape.contains(0, 0, 10, 0, 8.6));
        assertFalse(HexShape.contains(0, 0, 10, 0, 8.8));
        assertFalse(HexShape.contains(0, 0, 10, 9, 5));
    }
}
