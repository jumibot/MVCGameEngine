package controller;

import assets.AssetCatalog;
import controller.mappers.DynamicRenderableMapper;
import controller.mappers.RenderableMapper;
import controller.mappers.WeaponMapper;
import controller.ports.DomainEventProcesor;
import controller.ports.EngineState;
import controller.ports.WorldEvolver;
import controller.ports.WorldInitializer;

import java.awt.Dimension;
import view.renderables.DynamicRenderDTO;
import view.View;
import model.Model;

import model.bodies.core.AbstractBody;
import model.bodies.core.BodyDTO;

import model.weapons.ports.WeaponDto;
import model.ports.ActionDTO;
import model.ports.ActionExecutor;
import model.ports.ActionPriority;
import model.ports.ActionType;
import model.ports.EventDTO;
import model.ports.EventType;

import java.util.ArrayList;
import java.util.List;

import view.renderables.RenderDTO;
import world.WorldDefWeaponDto;

/**
 * Controller
 * ----------
 *
 * Central coordinator of the MVC triad:
 * - Owns references to Model and View.
 * - Performs engine startup wiring (assets, world definition, dimensions,
 * limits).
 * - Bridges user input (View) into Model commands.
 * - Provides snapshot getters used by the Renderer (via the View).
 *
 * Responsibilities (high level)
 * -----------------------------
 *
 * 1) Bootstrapping / activation sequence
 * - Validates that all required dependencies are present (assets, world,
 * dimensions, max bodies, model, view).
 * - Loads visual resources into the View (View.loadAssets).
 * - Configures the View and starts the Renderer loop (View.activate).
 * - Configures the Model (dimension, max bodies) and starts simulation
 * (Model.activate).
 * - Switches controller state to ALIVE when everything is ready.
 *
 * 2) World building / entity creation
 * - addDBody / addSBody / addDecorator / addPlayer delegate entity creation
 * to the Model.
 * - Important: static bodies and decorators are "push-updated" into the View:
 * after adding a static/decorator entity, the controller fetches a fresh
 * static/decorator snapshot from the Model and pushes it to the View
 * (View.updateSBodyInfo / View.updateDecoratorsInfo). This matches the design
 * where static/decorator visuals usually do not change every frame, so you
 * avoid unnecessary per-frame updates.
 *
 * 3) Runtime command dispatch
 * - Exposes high-level player commands that the View calls in response to
 * input:
 * playerThrustOn / playerThrustOff / playerReverseThrust
 * playerRotateLeftOn / playerRotateRightOn / playerRotateOff
 * playerFire
 * - All of these are simple delegations to the Model, keeping the View free
 * of simulation logic.
 *
 * 4) Snapshot access for rendering
 * - getDBodyInfo(): returns dynamic snapshot data from the Model. This is
 * intended to be pulled frequently (typically once per frame by the
 * Renderer thread).
 * - getSBodyInfo() / getDecoratorInfo(): used to push snapshots when
 * static/decorator content changes.
 *
 * 5) Game rules / decision layer (rule-based actions)
 * - decideActions(entity, events) takes Model events (EventDTO) and produces
 * a list of actions (ActionDTO).
 * - applyGameRules(...) maps events -> actions:
 * * World boundary reached => DIE (high priority)
 * * MUST_FIRE => FIRE (high priority)
 * * COLLIDED / NONE => no additional action
 * - If no "death-like" action is present, MOVE is appended by default.
 * This creates a deterministic baseline: entities always move unless
 * explicitly killed/exploded.
 *
 * Engine state
 * ------------
 * engineState is volatile and represents the Controller's view of the engine
 * lifecycle:
 * - STARTING: initial state after construction
 * - ALIVE: set after activate() finishes successfully
 * - PAUSED: set via enginePause()
 * - STOPPED: set via engineStop()
 *
 * Dependency injection rules
 * --------------------------
 * - setModel(model): stores the model and injects the controller back into
 * the model (model.setController(this)). This enables callbacks / rules
 * decisions if the Model consults the Controller.
 * - setView(view): stores the view and injects the controller into the view
 * (view.setController(this)). This enables the View to send player commands
 * and to pull snapshots.
 *
 * Threading notes
 * ---------------
 * - The Controller itself mostly acts as a facade. The key concurrency point
 * is snapshot access: Renderer thread pulls getDBodyInfo() frequently.
 * Static/decorator snapshots are pushed occasionally from the "logic side"
 * (model->controller->view).
 * - Keeping Controller methods small and side-effect-light reduces contention
 * and makes it easier to reason about where cross-thread interactions happen.
 */
public class Controller implements WorldEvolver, WorldInitializer, DomainEventProcesor {

    private volatile EngineState engineState;
    private int maxEntities;
    private Model model;
    private View view;
    private Dimension worldDimension;

    public Controller(int worldWidth, int worldHigh, int maxDBodies,
            View view, Model model, AssetCatalog assets) {

        this.engineState = EngineState.STARTING;
        this.setWorldDimension(worldWidth, worldHigh);
        this.setMaxEntities(maxDBodies);

        this.setModel(model);

        this.setView(view);
        this.view.loadAssets(assets);
    }

    /**
     * PUBLICS
     */
    public void activate() {
        if (this.worldDimension == null) {
            throw new IllegalArgumentException("Null world dimension");
        }

        if (this.maxEntities <= 0) {
            throw new IllegalArgumentException("Max visual objects not setted");
        }

        if (this.view == null) {
            throw new IllegalArgumentException("No view injected");
        }

        if (this.model == null) {
            throw new IllegalArgumentException("No model injected");
        }

        this.view.setDimension(this.worldDimension);
        this.view.activate();

        this.model.setDimension(this.worldDimension);
        this.model.setMaxDBody(this.maxEntities);
        this.model.activate();

        this.engineState = EngineState.ALIVE;
    }

    public void addDynamicBody(String assetId, double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust) {

        String entityId = this.model.addDynamicBody(size, posX, posY, speedX, speedY,
                accX, accY, angle, angularSpeed, angularAcc, thrust);

        if (entityId == null || entityId.isEmpty()) {
            return; // ======= Max entity quantity reached =======>
        }
        this.view.addDynamicRenderable(entityId, assetId);
    }

    public void addDecorator(String assetId, double size, double posX, double posY, double angle) {
        String entityId = this.model.addDecorator(size, posX, posY, angle);

        if (entityId == null || entityId.isEmpty()) {
            return; // ======= Max entity quantity reached =======>
        }
        this.view.addStaticRenderable(entityId, assetId);
        ArrayList<BodyDTO> bodiesData = this.model.getStaticsData();
        ArrayList<RenderDTO> renderablesData = RenderableMapper.fromBodyDTO(bodiesData);

        this.view.updateStaticRenderables(renderablesData);
    }

    public void notifyNewProjectileFired(String entityId, String assetId) {
        this.view.addDynamicRenderable(entityId, assetId);
    }

    public String addPlayer(String assetId, double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust) {

        String entityId = this.model.addPlayer(size, posX, posY, speedX, speedY,
                accX, accY, angle, angularSpeed, angularAcc, thrust);

        if (entityId == null) {
            return null; // ======= Max entity quantity reached =======>>
        }

        this.view.addDynamicRenderable(entityId, assetId);
        return entityId;
    }

    public void addStaticBody(String assetId, double size, double posX, double posY, double angle) {

        String entityId = this.model.addStaticBody(size, posX, posY, angle);
        ArrayList<BodyDTO> bodiesData = this.model.getStaticsData();
        ArrayList<RenderDTO> renderablesData = RenderableMapper.fromBodyDTO(bodiesData);

        if (entityId == null || entityId.isEmpty()) {
            return; // ======= Max entity quantity reached =======>>
        }
        this.view.addStaticRenderable(entityId, assetId);

        this.view.updateStaticRenderables(renderablesData);
    }

    public void addWeaponToPlayer(String playerId, WorldDefWeaponDto weaponDef, int shootingOffset) {

        WeaponDto weapon = WeaponMapper.fromWorldDef(weaponDef, shootingOffset);

        this.model.addWeaponToPlayer(playerId, weapon);
    }

    private List<ActionDTO> applyGameRules(AbstractBody body, EventDTO event) {

        List<ActionDTO> actions = new ArrayList<>(2);

        switch (event.eventType) {
            case REACHED_NORTH_LIMIT:
            case REACHED_SOUTH_LIMIT:
            case REACHED_EAST_LIMIT:
            case REACHED_WEST_LIMIT:
                actions.add(new ActionDTO(
                        ActionType.DIE, ActionExecutor.BODY, ActionPriority.HIGH));
                break;

            case MUST_FIRE:
                actions.add(new ActionDTO(
                        ActionType.FIRE, ActionExecutor.MODEL, ActionPriority.HIGH));
                break;

            case LIFE_OVER:
                actions.add(new ActionDTO(
                        ActionType.DIE, ActionExecutor.MODEL, ActionPriority.HIGH));
                break;

            case COLLIDED:
            case NONE:
            default:
                break;
        }

        return actions;
    }

    public List<ActionDTO> decideActions(AbstractBody body, List<EventDTO> events) {
        List<ActionDTO> actions = new ArrayList<>();

        if (events != null) {
            for (EventDTO event : events) {
                if (event != null && event.eventType != null && event.eventType != EventType.NONE) {
                    actions.addAll(applyGameRules(body, event));
                }
            }
        }

        if (!containsDeathLikeAction(actions)) {
            actions.add(new ActionDTO(
                    ActionType.MOVE, ActionExecutor.BODY, ActionPriority.NORMAL));
        }

        return actions;
    }

    public void enginePause() {
        this.engineState = EngineState.PAUSED;
    }

    public void engineStop() {
        this.engineState = EngineState.STOPPED;
    }

    public EngineState getEngineState() {
        return this.engineState;
    }

    public ArrayList<DynamicRenderDTO> getDynamicRenderablesData() {
        ArrayList<BodyDTO> bodyData = this.model.getDynamicsData();
        ArrayList<DynamicRenderDTO> renderables = new ArrayList<>();

        for (BodyDTO bodyDto : bodyData) {
            DynamicRenderDTO renderable = DynamicRenderableMapper.fromBodyDTO(bodyDto);
            renderables.add(renderable);
        }

        return renderables;
    }

    public int getEntityCreatedQuantity() {
        return this.model.getCreatedQuantity();
    }

    public int getEntityAliveQuantity() {
        return this.model.getAliveQuantity();
    }

    public int getEntityDeadQuantity() {
        return this.model.getDeadQuantity();
    }

    public int getMaxEntities() {
        return this.maxEntities;
    }

    public Dimension getWorldDimension() {
        return this.worldDimension;
    }

    public void loadAssets(AssetCatalog assets) {
        this.view.loadAssets(assets);
    }

    public void playerFire(String playerId) {
        this.model.playerFire(playerId);
    }

    public void playerThrustOn(String playerId) {
        this.model.playerThrustOn(playerId);
    }

    public void playerThrustOff(String playerId) {
        this.model.playerThrustOff(playerId);
    }

    public void playerReverseThrust(String playerId) {
        this.model.playerReverseThrust(playerId);
    }

    public void playerRotateLeftOn(String playerId) {
        model.playerRotateLeftOn(playerId);
    }

    public void playerRotateOff(String playerId) {
        this.model.playerRotateOff(playerId);
    }

    public void playerRotateRightOn(String playerId) {
        this.model.playerRotateRightOn(playerId);
    }

    public void selectNextWeapon(String playerId) {
        this.model.selectNextWeapon(playerId);
    }

    public void setLocalPlayer(String playerId) {
        this.view.setLocalPlayer(playerId);
    }

    public void setModel(Model model) {
        this.model = model;
        this.model.setDomainEventProcessor(this);
    }

    public void setView(View view) {
        this.view = view;
        this.view.setController(this);
    }

    public void setWorldDimension(int width, int height) {
        this.worldDimension = new Dimension(width, height);
    }

    public void setMaxEntities(int maxEntities) {
        this.maxEntities = maxEntities;
    }

    /**
     * PRIVATE
     */
    private boolean containsDeathLikeAction(List<ActionDTO> actions) {
        if (actions == null || actions.isEmpty()) {
            return false;
        }

        for (ActionDTO a : actions) {
            if (a != null && a.type != null) {
                if (a.type == ActionType.DIE || a.type == ActionType.EXPLODE_IN_FRAGMENTS) {
                    return true;
                }
            }
        }

        return false;
    }
}
