package view.renderables;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import images.ImageCache;
import view.renderables.ports.RenderDTO;

public class Renderable {

    private final String entityId;
    private final String assetId;
    private final ImageCache cache;

    private long lastFrameSeen;
    private RenderDTO renderableValues = null;
    private BufferedImage image = null;

    public Renderable(RenderDTO renderInfo, String assetId, ImageCache cache, long currentFrame) {
        this.entityId = renderInfo.entityId;
        this.assetId = assetId;
        this.lastFrameSeen = currentFrame;
        this.renderableValues = renderInfo;
        this.cache = cache;
        this.updateImageFromCache(this.assetId, (int) renderInfo.size, renderInfo.angle);
    }

    public Renderable(String entityId, String assetId, ImageCache cache, long currentFrame) {
        if (entityId == null || entityId.isEmpty()) {
            throw new IllegalArgumentException("Entity ID not set");
        }
        if (assetId == null || assetId.isEmpty()) {
            throw new IllegalArgumentException("Asset ID not set");
        }
        if (cache == null) {
            throw new IllegalArgumentException("Image cache not set");
        }

        this.entityId = entityId;
        this.assetId = assetId;
        this.cache = cache;
        this.lastFrameSeen = currentFrame;
    }

    /**
     * PUBLICS
     */
    public long getLastFrameSeen() {
        return this.lastFrameSeen;
    }

    public RenderDTO getRenderableValues() {
        return this.renderableValues;
    }

    public BufferedImage getImage() {
        return this.image;
    }

    public void update(RenderDTO renderInfo, long currentFrame) {
        this.updateImageFromCache(this.assetId, (int) renderInfo.size, renderInfo.angle);
        this.lastFrameSeen = currentFrame;
        this.renderableValues = renderInfo;
    }

    public void paint(Graphics2D g) {

        if (this.image == null) {
            return;
        }

        AffineTransform defaultTransform = g.getTransform();

        AffineTransform mainRotation = AffineTransform.getRotateInstance(
                Math.toRadians(this.renderableValues.angle),
                this.renderableValues.posX, this.renderableValues.posY);

        g.setTransform(mainRotation);

        g.drawImage(
                this.image,
                (int) (this.renderableValues.posX - this.renderableValues.size / 2),
                (int) (this.renderableValues.posY - this.renderableValues.size / 2),
                null);
        g.setTransform(defaultTransform);
    }

    public void updateImageFromCache(RenderDTO entityInfo) {
        this.updateImageFromCache(this.assetId, (int) entityInfo.size, entityInfo.angle);
    }

    private boolean updateImageFromCache(String assetId, int size, double angle) {
        boolean imageNeedsUpdate = this.image == null
                || this.renderableValues == null
                || !this.assetId.equals(assetId)
                || this.renderableValues.size != size
                || (int) this.renderableValues.angle != (int) angle;

        if (imageNeedsUpdate) {
            int normalizedAngle = ((int) angle % 360 + 360) % 360;
            this.image = this.cache.getImage(normalizedAngle, assetId, size);

            return true; // ====
        }

        return false;
    }
}
