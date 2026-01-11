package view.core;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import images.Images;
import images.ImageCache;
import view.huds.implementations.PlayerHUD;
import view.huds.implementations.SpatialGridHUD;
import view.huds.implementations.SystemHUD;
import view.renderables.implementations.DynamicRenderable;
import view.renderables.implementations.Renderable;
import view.renderables.ports.DynamicRenderDTO;
import view.renderables.ports.PlayerRenderDTO;
import view.renderables.ports.RenderDTO;
import view.renderables.ports.SpatialGridStatisticsRenderDTO;
import controller.ports.EngineState;

/**
 * Renderer
 * --------
 *
 * Active rendering loop responsible for drawing the current frame to the
 * screen. This class owns the rendering thread and performs all drawing using
 * a BufferStrategy-based back buffer.
 *
 * Architectural role
 * ------------------
 * The Renderer is a pull-based consumer of visual snapshots provided by the
 * View.
 * It never queries or mutates the model directly.
 *
 * Rendering is decoupled from simulation through immutable snapshot DTOs
 * (EntityInfoDTO / DBodyInfoDTO), ensuring that rendering remains deterministic
 * and free of model-side race conditions.
 *
 * Threading model
 * ---------------
 * - A dedicated render thread drives the render loop (Runnable).
 * - Rendering is active only while the engine state is ALIVE.
 * - The loop terminates cleanly when the engine reaches STOPPED.
 *
 * Data access patterns
 * --------------------
 * Three different renderable collections are used, each with a consciously
 * chosen
 * concurrency strategy based on update frequency and thread ownership:
 *
 * 1) Dynamic bodies (DBodies)
 * - Stored in a plain HashMap.
 * - Updated and rendered exclusively by the render thread.
 * - No concurrent access → no synchronization required.
 *
 * 2) Static bodies (SBodies)
 * - Rarely updated, potentially from non-render threads
 * (model → controller → view).
 * - Stored using a copy-on-write strategy:
 * * Updates create a new Map instance.
 * * The reference is swapped atomically via a volatile field.
 * - The render thread only reads stable snapshots.
 *
 * 3) Decorators
 * - Same access pattern as static bodies.
 * - Uses the same copy-on-write + atomic swap strategy.
 *
 * This design avoids locks, minimizes contention, and guarantees that the
 * render thread always iterates over a fully consistent snapshot.
 *
 * Frame tracking
 * --------------
 * A monotonically increasing frame counter (currentFrame) is used to:
 * - Track renderable liveness.
 * - Remove obsolete renderables deterministically.
 *
 * Each update method captures a local frame snapshot to ensure internal
 * consistency, even if the global frame counter advances later.
 *
 * Rendering pipeline
 * ------------------
 * Per frame:
 * 1) Background is rendered to a VolatileImage for fast blitting.
 * 2) Decorators are drawn.
 * 3) Static bodies are drawn.
 * 4) Dynamic bodies are updated and drawn.
 * 5) HUD elements (FPS) are rendered last.
 *
 * Alpha compositing is used to separate opaque background rendering from
 * transparent entities.
 *
 * Performance considerations
 * --------------------------
 * - Triple buffering via BufferStrategy.
 * - VolatileImage used for background caching.
 * - Target frame rate ~60 FPS (16 ms delay).
 * - FPS is measured using a rolling one-second window.
 *
 * Design goals
 * ------------
 * - Deterministic rendering.
 * - Zero blocking in the render loop.
 * - Clear ownership of mutable state.
 * - Explicit, documented concurrency decisions.
 *
 * This class is intended to behave as a low-level rendering component suitable
 * for a small game engine rather than a UI-centric Swing renderer.
 */
public class Renderer extends Canvas implements Runnable {

    private long lastTimeMonitoring = System.nanoTime();
    private int fpsFrames = 0;
    private volatile int fps = 0;
    private volatile double renderTimeInMs = 0;
    private volatile double initDrawTimeStamp;

    private Dimension viewDimension;
    private View view;
    private int delayInMillis = 0;
    private long currentFrame = 0;
    private Thread thread;

    private BufferedImage background;
    private Images images;
    private ImageCache imagesCache;
    private VolatileImage viBackground;
    private final PlayerHUD playerHUD = new PlayerHUD();
    private final SystemHUD systemHUD = new SystemHUD();
    private final SpatialGridHUD spatialGridHUD = new SpatialGridHUD();

    private final Map<String, DynamicRenderable> dynamicRenderables = new ConcurrentHashMap<>();
    private volatile Map<String, Renderable> staticRenderables = new ConcurrentHashMap<>();

    /**
     * CONSTRUCTORS
     */
    public Renderer(View view) {
        this.view = view;

        this.setIgnoreRepaint(true);
    }

    /**
     * PUBLICS
     */
    public boolean activate() {
        // Be sure all is ready to begin render!
        if (this.viewDimension == null) {
            throw new IllegalArgumentException("View dimensions not setted");
        }

        if ((this.viewDimension.width <= 0) || (this.viewDimension.height <= 0)) {
            throw new IllegalArgumentException("Canvas size error: ("
                    + this.viewDimension.width + "," + this.viewDimension.height + ")");
        }

        while (!this.isDisplayable()) {
            try {
                Thread.sleep(this.delayInMillis);
            } catch (InterruptedException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        this.setPreferredSize(this.viewDimension);
        this.thread = new Thread(this);
        this.thread.setName("Renderer");
        this.thread.setPriority(Thread.NORM_PRIORITY + 2);
        this.thread.start();

        return true;
    }

    public void addStaticRenderable(String entityId, String assetId) {
        Renderable renderable = new Renderable(entityId, assetId, this.imagesCache, this.currentFrame);
        this.staticRenderables.put(entityId, renderable);
    }

    public void addDynamicRenderable(String entityId, String assetId) {
        DynamicRenderable renderable = new DynamicRenderable(entityId, assetId, this.imagesCache, this.currentFrame);
        this.dynamicRenderables.put(entityId, renderable);
    }

    public void setImages(BufferedImage background, Images images) {
        this.background = background;
        this.viBackground = null;

        this.images = images;
        this.imagesCache = new ImageCache(this.getGraphicsConfSafe(), this.images);
    }

    public void SetViewDimension(Dimension viewDim) {
        this.viewDimension = viewDim;
        this.setPreferredSize(this.viewDimension);
    }

    @Override
    public void run() {
        this.createBufferStrategy(3);
        BufferStrategy bs = getBufferStrategy();

        while (true) {
            EngineState engineState = this.view.getEngineState();
            if (engineState == EngineState.STOPPED) {
                break;
            }

            if (engineState == EngineState.ALIVE) { // TO-DO Pause condition
                this.currentFrame++;

                this.initDrawTimeStamp = System.nanoTime();
                this.drawScene(bs);
                this.monitoring();
            }

            try {
                Thread.sleep(this.delayInMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void updateStaticRenderables(ArrayList<RenderDTO> renderablesData) {
        if (renderablesData == null) {
            return; // ========= Nothing to render by the moment ... =========>>
        }

        Map<String, Renderable> newRenderables = new java.util.concurrent.ConcurrentHashMap<>(this.staticRenderables);

        if (renderablesData.isEmpty()) {
            newRenderables.clear(); //
            this.staticRenderables = newRenderables;
            return;
        }

        // Update a renderable associated with each DBodyRenderInfoDTO
        long cFrame = this.currentFrame;
        for (RenderDTO renderableData : renderablesData) {
            String entityId = renderableData.entityId;
            if (entityId == null || entityId.isEmpty()) {
                continue;
            }

            Renderable renderable = newRenderables.get(entityId);
            if (renderable == null) {
                System.err.println("Renderer: Static renderable objet not found " + entityId);
            } else {
                renderable.update(renderableData, cFrame);
            }
        }

        newRenderables.entrySet().removeIf(e -> e.getValue().getLastFrameSeen() != cFrame);
        this.staticRenderables = newRenderables; // atomic swap
    }

    /**
     * PRIVATES
     */
    private void drawDynamicRenderable(Graphics2D g) {
        ArrayList<DynamicRenderDTO> renderablesData = this.view.getDynamicRenderablesData(); // *+

        this.updateDynamicRenderables(renderablesData);

        Map<String, DynamicRenderable> renderables = this.dynamicRenderables;
        for (DynamicRenderable renderable : renderables.values()) {
            renderable.paint(g);
        }
    }

    private void drawHUDs(Graphics2D g) {

        this.systemHUD.draw(g,
                this.fps,
                String.format("%.0f", this.renderTimeInMs) + " ms",
                this.imagesCache.size(),
                String.format("%.0f", this.imagesCache.getHitsPercentage()) + "%",
                this.view.getEntityAliveQuantity(),
                this.view.getEntityDeadQuantity());

        PlayerRenderDTO playerData = this.view.getLocalPlayerRenderData();
        if (playerData != null) {
            this.playerHUD.draw(g, playerData.toObjectArray());
        }

        SpatialGridStatisticsRenderDTO spatialGridStats = this.view.getSpatialGridStatistics();
        if (spatialGridStats != null) {
            this.spatialGridHUD.draw(g, spatialGridStats.toObjectArray());
        }
    }

    private void drawStaticRenderables(Graphics2D g) {
        Map<String, Renderable> renderables = this.staticRenderables;

        for (Renderable renderable : renderables.values()) {
            renderable.paint(g);
        }
    }

    private void drawScene(BufferStrategy bs) {
        Graphics2D gg;

        do {
            gg = (Graphics2D) bs.getDrawGraphics();
            try {
                gg.setComposite(AlphaComposite.Src); // Opaque
                gg.drawImage(this.getVIBackground(), 0, 0, null);

                gg.setComposite(AlphaComposite.SrcOver); // With transparency
                this.drawStaticRenderables(gg);
                this.drawHUDs(gg);

                this.drawDynamicRenderable(gg);

            } finally {
                gg.dispose();
            }

            bs.show();
        } while (bs.contentsLost());
    }

    private GraphicsConfiguration getGraphicsConfSafe() {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        if (gc == null) {
            gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();
        }

        return gc;
    }

    private VolatileImage getVIBackground() {
        this.viBackground = this.getVolatileImage(
                this.viBackground, this.background, this.viewDimension);

        return this.viBackground;

    }

    private VolatileImage getVolatileImage(
            VolatileImage vi, BufferedImage src, Dimension dim) {

        GraphicsConfiguration gc = this.getGraphicsConfSafe();

        if (vi == null || vi.getWidth() != dim.width || vi.getHeight() != dim.height
                || vi.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
            // New volatile image
            vi = gc.createCompatibleVolatileImage(dim.width, dim.height, Transparency.OPAQUE);
        }

        int val;
        do {
            val = vi.validate(gc);
            if (val != VolatileImage.IMAGE_OK || vi.contentsLost()) {
                Graphics2D g = vi.createGraphics();
                g.drawImage(src, 0, 0, dim.width, dim.height, null);
                g.dispose();
            }
        } while (vi.contentsLost());

        return vi;
    }

    private void monitoring() {
        this.fpsFrames++;
        long now = System.nanoTime();
        long elapsed = now - this.lastTimeMonitoring;

        if (elapsed >= 500_000_000L) { // 0.5 second
            this.fps = (int) Math.round(fpsFrames * (1_000_000_000.0 / elapsed));
            this.fpsFrames = 0;
            this.lastTimeMonitoring = now;

            this.renderTimeInMs = (System.nanoTime() - this.initDrawTimeStamp) / 1_000_000;

        }
    }

    private void updateDynamicRenderables(ArrayList<DynamicRenderDTO> renderablesData) {
        // If no objects are alive this frame, clear the snapshot entirely
        if (renderablesData == null || renderablesData.isEmpty()) {
            this.dynamicRenderables.clear();
            return; // ========= Nothing to render by the moment ... =========>>
        }

        // Update or create a renderable associated with each DBodyRenderInfoDTO
        long cFrame = this.currentFrame;
        for (DynamicRenderDTO renderableData : renderablesData) {
            String entityId = renderableData.entityId;
            if (entityId == null || entityId.isEmpty()) {
                continue;
            }

            DynamicRenderable renderable = this.dynamicRenderables.get(entityId);
            if (renderable == null) {
                System.err.println("Renderer: Dynamic renderable objet not found " + entityId);
            } else {
                // Existing renderable → update its snapshot and sprite if needed
                renderable.update(renderableData, cFrame);
            }
        }

        // Remove renderables not updated this frame (i.e., objects no longer alive)
        this.dynamicRenderables.entrySet().removeIf(entry -> entry.getValue().getLastFrameSeen() != cFrame);
    }
}
