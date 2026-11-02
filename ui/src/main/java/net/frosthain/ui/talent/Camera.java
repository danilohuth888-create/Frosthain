package net.frosthain.ui.talent;

/**
 * Pan and zoom for the talent tree. World coordinates are grid units of node positions; screen coordinates
 * are pixels of the tree area. Zoom is pixels per grid unit and is clamped so the tree stays readable.
 */
public final class Camera {

    private final double minZoom;
    private final double maxZoom;
    private double centerX;
    private double centerY;
    private double zoom;
    private double viewportWidth;
    private double viewportHeight;

    public Camera(double minZoom, double maxZoom, double zoom) {
        if (minZoom <= 0 || maxZoom < minZoom) {
            throw new IllegalArgumentException("ungültiger Zoombereich");
        }
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.zoom = Math.clamp(zoom, minZoom, maxZoom);
    }

    public void setViewport(double width, double height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    public double zoom() {
        return zoom;
    }

    public double centerX() {
        return centerX;
    }

    public double centerY() {
        return centerY;
    }

    public double toScreenX(double worldX) {
        return viewportWidth / 2 + (worldX - centerX) * zoom;
    }

    public double toScreenY(double worldY) {
        return viewportHeight / 2 + (worldY - centerY) * zoom;
    }

    public double toWorldX(double screenX) {
        return centerX + (screenX - viewportWidth / 2) / zoom;
    }

    public double toWorldY(double screenY) {
        return centerY + (screenY - viewportHeight / 2) / zoom;
    }

    /** Moves the view by a screen-space delta (e.g. mouse drag). */
    public void panPixels(double dx, double dy) {
        centerX -= dx / zoom;
        centerY -= dy / zoom;
    }

    public void centerOn(double worldX, double worldY) {
        centerX = worldX;
        centerY = worldY;
    }

    /** Zooms by {@code factor}, keeping the world point under the given screen position fixed. */
    public void zoomAt(double factor, double screenX, double screenY) {
        double worldX = toWorldX(screenX);
        double worldY = toWorldY(screenY);
        zoom = Math.clamp(zoom * factor, minZoom, maxZoom);
        centerX = worldX - (screenX - viewportWidth / 2) / zoom;
        centerY = worldY - (screenY - viewportHeight / 2) / zoom;
    }

    /** Fits a world-space box (plus margin in grid units) into the viewport. */
    public void fit(double minX, double minY, double maxX, double maxY, double margin) {
        double width = Math.max(1e-6, maxX - minX + 2 * margin);
        double height = Math.max(1e-6, maxY - minY + 2 * margin);
        zoom = Math.clamp(Math.min(viewportWidth / width, viewportHeight / height), minZoom, maxZoom);
        centerX = (minX + maxX) / 2;
        centerY = (minY + maxY) / 2;
    }
}
