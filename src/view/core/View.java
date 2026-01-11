package view.core;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import assets.core.AssetCatalog;
import assets.ports.AssetType;
import images.Images;
import controller.implementations.Controller;
import controller.ports.EngineState;
import view.renderables.ports.DynamicRenderDTO;
import view.renderables.ports.PlayerRenderDTO;
import view.renderables.ports.RenderDTO;
import view.renderables.ports.SpatialGridStatisticsRenderDTO;

/**
 * View
 * ----
 *
 * Swing top-level window that represents the presentation layer of the engine.
 * This class wires together:
 * - The rendering surface (Renderer)
 * - Asset loading and image catalogs (Images)
 * - User input (KeyListener) and command dispatch to the Controller
 *
 * Architectural role
 * ------------------
 * View is a thin façade over rendering + input:
 * - It does not simulate anything.
 * - It does not own world state.
 * - It communicates with the model exclusively through the Controller.
 *
 * The Renderer pulls dynamic snapshots every frame (via View -> Controller),
 * while static/decorator snapshots are pushed into the View/Renderer only when
 * they change (to avoid redundant per-frame updates for entities that do not
 * move).
 *
 * Lifecycle
 * ---------
 * Construction:
 * - Creates the ControlPanel (UI controls, if any).
 * - Creates the Renderer (Canvas).
 * - Builds the JFrame layout and attaches the key listener.
 *
 * Activation (activate()):
 * - Validates mandatory dependencies (dimensions, background, image catalogs).
 * - Injects view dimensions and images into the Renderer.
 * - Starts the Renderer thread (active rendering loop).
 *
 * Asset management
 * ----------------
 * loadAssets(...) loads and registers all visual resources required by the
 * world:
 * - Background image (single BufferedImage).
 * - Dynamic body sprites (ships, asteroids, missiles, etc.).
 * - Static body sprites (gravity bodies, bombs, etc.).
 * - Decorator sprites (parallax / space decor).
 *
 * The View stores catalogs as Images collections, which are later converted
 * into GPU/compatible caches inside the Renderer (ImageCache).
 *
 * Engine state delegation
 * -----------------------
 * View exposes getEngineState() as a convenience bridge for the Renderer.
 * The render loop can stop or pause based on Controller-owned engine state.
 *
 * Input handling
 * --------------
 * Keyboard input is captured at the rendering Canvas level (Renderer is
 * focusable and receives the KeyListener) and translated into high-level
 * Controller commands:
 * - Thrust on/off (forward uses positive thrust; reverse thrust is handled
 * as negative thrust, and both are stopped via the same thrustOff command).
 * - Rotation left/right and rotation off.
 * - Fire: handled as an edge-triggered action using fireKeyDown to prevent
 * key repeat from generating continuous shots while SPACE is held.
 *
 * Focus and Swing considerations
 * -------------------------------
 * The Renderer is the focus owner for input. Focus is requested after the frame
 * becomes visible using SwingUtilities.invokeLater(...) to improve reliability
 * with Swing's event dispatch timing.
 *
 * Threading considerations
 * ------------------------
 * Swing is single-threaded (EDT), while rendering runs on its own thread.
 * This class keeps its responsibilities minimal:
 * - It only pushes static/decorator updates when needed.
 * - Dynamic snapshot pulling is done inside the Renderer thread through
 * View -> Controller getters.
 *
 * Design goals
 * ------------
 * - Keep the View as a coordinator, not a state holder.
 * - Keep rendering independent and real-time (active rendering).
 * - Translate user input into controller commands cleanly and predictably.
 */
public class View extends JFrame implements KeyListener {

    private BufferedImage background;
    private Controller controller;
    private final ControlPanel controlPanel;
    private final Images images;
    private String localPlayerId;
    private final Renderer renderer;
    private Dimension viewDimension;
    private boolean fireKeyDown = false;

    /**
     * CONSTRUCTOR
     */
    public View() {
        this.images = new Images("");
        this.controlPanel = new ControlPanel(this);
        this.renderer = new Renderer(this);
        this.createFrame();
    }

    /**
     * PUBLIC
     */
    public void activate() {
        if (this.viewDimension == null) {
            throw new IllegalArgumentException("View dimensions not setted");
        }

        this.renderer.SetViewDimension(this.viewDimension);
        this.renderer.setImages(this.background, this.images);
        this.renderer.activate();
        this.pack();
    }

    public void addStaticRenderable(String entityId, String assetId) {
        this.renderer.addStaticRenderable(entityId, assetId);
    }

    public void addDynamicRenderable(String entityId, String assetId) {
        this.renderer.addDynamicRenderable(entityId, assetId);
    }

    public void loadAssets(AssetCatalog assets) {
        String fileName;
        String path = assets.getPath();

        for (String assetId : assets.getAssetIds()) {
            fileName = assets.get(assetId).fileName;
            this.images.add(assetId, path + fileName);
        }

        // Setting background
        String backgroundId = assets.randomId(AssetType.BACKGROUND);
        this.background = this.images.getImage(backgroundId).image;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void setDimension(Dimension worldDim) {
        this.viewDimension = worldDim;
    }

    public void setLocalPlayer(String localPlayerId) {
        this.localPlayerId = localPlayerId;
    }

    public void updateStaticRenderables(ArrayList<RenderDTO> renderablesData) {
        this.renderer.updateStaticRenderables(renderablesData);
    }

    /**
     * PROTECTED
     */
    protected ArrayList<DynamicRenderDTO> getDynamicRenderablesData() {
        if (this.controller == null) {
            throw new IllegalArgumentException("Controller not setted");
        }

        return this.controller.getDynamicRenderablesData();
    }

    protected EngineState getEngineState() {
        return this.controller.getEngineState();
    }

    protected int getEntityAliveQuantity() {
        return this.controller.getEntityAliveQuantity();
    }

    protected int getEntityCreatedQuantity() {
        return this.controller.getEntityCreatedQuantity();
    }

    protected int getEntityDeadQuantity() {
        return this.controller.getEntityDeadQuantity();
    }

    protected PlayerRenderDTO getLocalPlayerRenderData() {
        if (this.localPlayerId == null || this.localPlayerId.isEmpty()) {
            return null;
        }

        return this.controller.getPlayerRenderData(this.localPlayerId);
    }

    protected SpatialGridStatisticsRenderDTO getSpatialGridStatistics() {
        return this.controller.getSpatialGridStatistics();
    }

    /**
     * PRIVATE
     */
    private void addRendererCanva(Container container) {
        GridBagConstraints c = new GridBagConstraints();

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1F;
        c.weighty = 0;
        c.gridheight = 10;
        c.gridwidth = 8;
        container.add(this.renderer, c);
    }

    private void createFrame() {
        Container panel;

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new GridBagLayout());

        panel = this.getContentPane();
        this.addRendererCanva(panel);
        this.renderer.setFocusable(true);
        this.renderer.addKeyListener(this);

        this.pack();
        this.setVisible(true);
        SwingUtilities.invokeLater(() -> this.renderer.requestFocusInWindow());

    }

    /**
     * OVERRIDES
     */
    @Override
    public void keyPressed(KeyEvent e) {
        if (this.localPlayerId == null) {
            System.out.println("Local player not setted!");
            return;
        }

        if (this.controller == null) {
            System.out.println("Controller not set yet");
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                this.controller.playerThrustOn(this.localPlayerId);
                break;

            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_X:
                this.controller.playerReverseThrust(this.localPlayerId);
                break;

            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                this.controller.playerRotateLeftOn(this.localPlayerId);
                break;

            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                this.controller.playerRotateRightOn(this.localPlayerId);
                break;

            case KeyEvent.VK_SPACE:
                if (!this.fireKeyDown) { // Discard autoreptition PRESS
                    this.fireKeyDown = true;
                    this.controller.playerFire(this.localPlayerId);
                }
                break;

            case KeyEvent.VK_1:
                this.controller.selectNextWeapon(this.localPlayerId);
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (this.localPlayerId == null) {
            System.out.println("Local player not setted!");
            return;
        }

        if (this.controller == null) {
            System.out.println("Controller not set yet");
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_A:
                this.controller.playerThrustOff(this.localPlayerId);
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_Z:
                this.controller.playerThrustOff(this.localPlayerId);
                break;
            case KeyEvent.VK_LEFT:
                this.controller.playerRotateOff(this.localPlayerId);
                break;
            case KeyEvent.VK_RIGHT:
                this.controller.playerRotateOff(this.localPlayerId);
                break;
            case KeyEvent.VK_SPACE:
                fireKeyDown = false; // << permite el siguiente disparo
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Nothing to do
    }
}
