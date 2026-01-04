package model;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import controller.ports.DomainEventProcesor;

import java.awt.Dimension;
import static java.lang.System.nanoTime;
import java.util.Comparator;
import java.util.List;

import model.bodies.DecoBody;
import model.bodies.DynamicBody;
import model.bodies.PlayerBody;
import model.bodies.StaticBody;
import model.bodies.core.AbstractBody;
import model.bodies.core.BodyDTO;
import model.bodies.ports.BodyState;

import model.physics.BasicPhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;

import model.ports.ActionDTO;
import model.ports.ActionType;
import model.ports.EventDTO;
import model.ports.EventType;
import model.ports.ModelState;

import model.weapons.ports.Weapon;
import model.weapons.ports.WeaponDto;
import model.weapons.ports.WeaponFactory;

/**
 * Model
 * -----
 *
 * Core simulation layer of the MVC triad. The Model owns and manages all
 * entities (dynamic bodies, static bodies, players, decorators) and
 * orchestrates
 * their lifecycle, physics updates, and interactions.
 *
 * Responsibilities
 * ----------------
 * - Entity management: create, activate, and track all simulation entities
 * - Provide thread-safe snapshot data (EntityInfoDTO / DBodyInfoDTO) to the
 * Controller for rendering
 * - Delegate physics updates to individual entity threads
 * - Maintain entity collections with appropriate concurrency strategies
 * - Enforce world boundaries and entity limits
 *
 * Entity types
 * ------------
 * The Model manages several distinct entity categories:
 *
 * 1) Dynamic Bodies (dBodies)
 * - Entities with active physics simulation (ships, asteroids, projectiles)
 * - Each runs on its own thread, continuously updating position/velocity
 * - Stored in ConcurrentHashMap for thread-safe access
 *
 * 2) Player Bodies (pBodies)
 * - Special dynamic bodies with player controls and weapons
 * - Keyed by player ID string
 * - Support thrust, rotation, and firing commands
 *
 * 3) Static Bodies (sBodies)
 * - Non-moving entities with fixed positions (obstacles, platforms)
 * - No physics thread
 * - Push-updated to View when created/modified
 *
 * 4) Gravity Bodies (gravityBodies)
 * - Static bodies that exert gravitational influence
 * - Used for planetary bodies or black holes
 *
 * 5) Decorators (decorators)
 * - Visual-only entities with no gameplay impact (background elements)
 * - Push-updated to View when created/modified
 *
 * Lifecycle
 * ---------
 * Construction:
 * - Model is created in STARTING state
 * - Entity maps are pre-allocated with expected capacities
 *
 * Activation (activate()):
 * - Validates that Controller, world dimensions, and max entities are set
 * - Transitions to ALIVE state
 * - After activation, entities can be created and activated
 *
 * Snapshot generation
 * -------------------
 * The Model provides snapshot methods that return immutable DTOs:
 * - getDBodyInfo(): returns List<DBodyInfoDTO> for all active dynamic bodies
 * - getSBodyInfo(): returns List<EntityInfoDTO> for all active static bodies
 * - getDecoratorInfo(): returns List<EntityInfoDTO> for all decorators
 *
 * These snapshots are pulled by the Controller and pushed to the View/Renderer.
 * The pattern ensures clean separation: rendering never accesses mutable
 * entity state directly.
 *
 * Concurrency strategy
 * --------------------
 * - All entity maps use ConcurrentHashMap for thread-safe access
 * - Individual entities manage their own thread synchronization
 * - Model state transitions are protected by volatile fields
 * - Snapshot methods create independent DTO lists to avoid concurrent
 * modification during rendering
 *
 * Design goals
 * ------------
 * - Keep simulation logic isolated from view concerns
 * - Provide deterministic, thread-safe entity management
 * - Support high entity counts (up to MAX_ENTITIES = 5000)
 * - Enable efficient parallel physics updates via per-entity threads
 */

public class Model {

    private int maxDBody;
    private Dimension worldDim;

    private DomainEventProcesor domainEventProcessor = null;
    private volatile ModelState state = ModelState.STARTING;

    private static final int MAX_ENTITIES = 5000;
    private final Map<String, AbstractBody> dynamicBodies = new ConcurrentHashMap<>(MAX_ENTITIES);
    private final Map<String, AbstractBody> decorators = new ConcurrentHashMap<>(100);
    private final Map<String, AbstractBody> gravityBodies = new ConcurrentHashMap<>(50);
    private final Map<String, AbstractBody> playerBodies = new ConcurrentHashMap<>(10);
    private final Map<String, AbstractBody> staticBodies = new ConcurrentHashMap<>(100);

    /**
     * CONSTRUCTORS
     */
    public Model() {

    }

    /**
     * PUBLIC
     */
    public void activate() {
        if (this.domainEventProcessor == null) {
            throw new IllegalArgumentException("Controller is not set");
        }

        if (this.worldDim == null) {
            throw new IllegalArgumentException("Null world dimension");
        }

        if (this.maxDBody <= 0) {
            throw new IllegalArgumentException("Max visual objects not set");
        }
        this.state = ModelState.ALIVE;
    }

    public String addDynamicBody(double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust) {

        return this.addDynamicBody(size, posX, posY, speedX, speedY, accX, accY, angle, angularSpeed, angularAcc,
                thrust, -1L);
    }

    public String addDynamicBody(double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust, double maxLifeInSeconds) {

        if (AbstractBody.getAliveQuantity() >= this.maxDBody) {
            return null; // ========= Max vObject quantity reached ==========>>
        }

        PhysicsValuesDTO phyVals = new PhysicsValuesDTO(nanoTime(), posX, posY, angle, size,
                speedX, speedY, accX, accY, angularSpeed, angularAcc, thrust);

        DynamicBody dBody = new DynamicBody(new BasicPhysicsEngine(phyVals), maxLifeInSeconds);

        dBody.setModel(this);
        dBody.activate();
        this.dynamicBodies.put(dBody.getEntityId(), dBody);

        return dBody.getEntityId();
    }

    public String addDecorator(double size, double posX, double posY, double angle) {
        DecoBody deco = new DecoBody(size, posX, posY, angle);

        deco.setModel(this);
        deco.activate();
        this.decorators.put(deco.getEntityId(), deco);

        return deco.getEntityId();
    }

    public String addPlayer(double size,
            double posX, double posY, double speedX, double speedY,
            double accX, double accY,
            double angle, double angularSpeed, double angularAcc,
            double thrust) {

        if (AbstractBody.getAliveQuantity() >= this.maxDBody) {
            return null; // ========= Max vObject quantity reached ==========>>
        }

        PhysicsValuesDTO phyVals = new PhysicsValuesDTO(
                nanoTime(), posX, posY, angle, size,
                speedX, speedY, accX, accY,
                angularSpeed, angularAcc, thrust);

        PlayerBody pBody = new PlayerBody(new BasicPhysicsEngine(phyVals));

        pBody.setModel(this);
        pBody.activate();
        String entityId = pBody.getEntityId();
        this.dynamicBodies.put(entityId, pBody);
        this.playerBodies.put(entityId, pBody);

        return entityId;
    }

    public String addStaticBody(double size,
            double posX, double posY, double angle) {

        StaticBody sBody = new StaticBody(size, posX, posY, angle);

        sBody.setModel(this);
        sBody.activate();
        this.staticBodies.put(sBody.getEntityId(), sBody);

        return sBody.getEntityId();
    }

    public void addWeaponToPlayer(
            String playerId, WeaponDto weaponConfig) {

        PlayerBody pBody = (PlayerBody) this.playerBodies.get(playerId);
        if (pBody == null) {
            return; // ========= Player not found =========>
        }

        Weapon weapon = WeaponFactory.create(weaponConfig);

        pBody.addWeapon(weapon);
    }

    public int getMaxDBody() {
        return this.maxDBody;
    }

    public ArrayList<BodyDTO> getDynamicsData() {
        return this.getBodyData(this.dynamicBodies);
    }

    public ArrayList<BodyDTO> getStaticsData() {
        ArrayList<BodyDTO> staticsInfo;

        staticsInfo = this.getBodyData(this.decorators);
        staticsInfo.addAll(this.getBodyData(this.staticBodies));
        staticsInfo.addAll(this.getBodyData(this.gravityBodies));

        return staticsInfo;
    }

    public ArrayList<BodyDTO> getBodyData(Map<String, AbstractBody> bodies) {
        ArrayList<BodyDTO> bodyData = new ArrayList<BodyDTO>(bodies.size());

        bodies.forEach((entityId, body) -> {
            BodyDTO bodyInfo = new BodyDTO(entityId, body.getPhysicsValues());
            if (bodyInfo != null) {
                bodyData.add(bodyInfo);
            }
        });

        return bodyData;
    }

    public ModelState getState() {
        return this.state;
    }

    public int getCreatedQuantity() {
        return AbstractBody.getCreatedQuantity();
    }

    public int getAliveQuantity() {
        return AbstractBody.getAliveQuantity();
    }

    public int getDeadQuantity() {
        return AbstractBody.getDeadQuantity();
    }

    public Dimension getWorldDimension() {
        return this.worldDim;
    }

    public boolean isAlive() {
        return this.state == ModelState.ALIVE;
    }

    public void killDBody(DynamicBody dBody) {
        dBody.die();
        this.dynamicBodies.remove(dBody.getEntityId());
    }

    public void playerFire(String playerId) {
        PlayerBody pBody = (PlayerBody) this.playerBodies.get(playerId);
        if (pBody != null) {
            pBody.requestFire();
        }
    }

    public void playerThrustOn(String playerId) {
        PlayerBody pBody = (PlayerBody) this.playerBodies.get(playerId);
        if (pBody != null) {
            pBody.thrustOn();
        }
    }

    public void playerThrustOff(String playerId) {
        PlayerBody pBody = (PlayerBody) this.playerBodies.get(playerId);
        if (pBody != null) {
            pBody.thrustOff();
        }
    }

    public void playerReverseThrust(String playerId) {
        PlayerBody pBody = (PlayerBody) this.playerBodies.get(playerId);
        if (pBody != null) {
            pBody.reverseThrust();
        }
    }

    public void playerRotateLeftOn(String playerId) {
        PlayerBody pBody = (PlayerBody) this.playerBodies.get(playerId);
        if (pBody != null) {
            pBody.rotateLeftOn();
        }
    }

    public void playerRotateOff(String playerId) {
        PlayerBody pBody = (PlayerBody) this.playerBodies.get(playerId);
        if (pBody != null) {
            pBody.rotateOff();
        }
    }

    public void playerRotateRightOn(String playerId) {
        PlayerBody pBody = (PlayerBody) this.playerBodies.get(playerId);
        if (pBody != null) {
            pBody.rotateRightOn();
        }
    }

    public void processDBodyEvents(DynamicBody dynamicBody,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (!isProcessable(dynamicBody)) {
            return; // To avoid duplicate or unnecesary event processing ======>
        }

        BodyState previousState = dynamicBody.getState();
        dynamicBody.setState(BodyState.HANDS_OFF);

        try {
            List<EventDTO> events = this.detectEvents(
                    dynamicBody, newPhyValues, oldPhyValues);

            List<ActionDTO> actions = this.resolveActionsForEvents(
                    dynamicBody, events);

            this.doActions(
                    dynamicBody, actions, newPhyValues, oldPhyValues);

        } catch (Exception e) { // Fallback anti-zombi
            if (dynamicBody.getState() == BodyState.HANDS_OFF) {
                dynamicBody.setState(previousState);
            }

        } finally { // Getout: off HANDS_OFF ... if leaving
            if (dynamicBody.getState() == BodyState.HANDS_OFF) {
                dynamicBody.setState(BodyState.ALIVE);
            }
        }
    }

    public void selectNextWeapon(String playerId) {
        PlayerBody pBody = (PlayerBody) this.playerBodies.get(playerId);
        if (pBody == null) {
            return;
        }

        pBody.selectNextWeapon();
    }

    public void setDomainEventProcessor(DomainEventProcesor domainEventProcessor) {
        this.domainEventProcessor = domainEventProcessor;
    }

    public void setDimension(Dimension worldDim) {
        this.worldDim = worldDim;
    }

    public void setMaxDBody(int maxDynamicBody) {
        this.maxDBody = maxDynamicBody;
    }

    /**
     * PRIVATE
     */
    private List<EventDTO> checkLimitEvents(AbstractBody entity, PhysicsValuesDTO phyValues) {
        List<EventDTO> limitEvents = new ArrayList<>(4);

        if (phyValues.posX < 0) {
            limitEvents.add(new EventDTO(entity, EventType.REACHED_EAST_LIMIT));
        }

        if (phyValues.posX >= this.worldDim.width) {
            limitEvents.add(new EventDTO(entity, EventType.REACHED_WEST_LIMIT));
        }

        if (phyValues.posY < 0) {
            limitEvents.add(new EventDTO(entity, EventType.REACHED_NORTH_LIMIT));
        }

        if (phyValues.posY >= this.worldDim.height) {
            limitEvents.add(new EventDTO(entity, EventType.REACHED_SOUTH_LIMIT));
        }

        return limitEvents;
    }

    private List<ActionDTO> resolveActionsForEvents(
            AbstractBody entity, List<EventDTO> events) {

        List<ActionDTO> actionsFromController = this.domainEventProcessor.decideActions(entity, events);

        if (actionsFromController == null || actionsFromController.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<ActionDTO> actions = new ArrayList<>(actionsFromController.size());
        for (ActionDTO a : actionsFromController) {
            if (a != null && a.type != null && a.type != ActionType.NONE) {
                actions.add(a);
            }
        }

        return actions;
    }

    private List<EventDTO> detectEvents(DynamicBody body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        List<EventDTO> events = this.checkLimitEvents(body, newPhyValues);

        if (body instanceof PlayerBody) {
            if (((PlayerBody) body).mustFireNow(newPhyValues)) {
                events.add(new EventDTO(body, EventType.MUST_FIRE));
            }
        }

        if (body.isLifeOver()) {
            events.add(new EventDTO(body, EventType.LIFE_OVER));
        }

        // Eventos de colisión, zonas, etc.
        return events;
    }

    private void doActions(
            DynamicBody body, List<ActionDTO> actions,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (actions == null || actions.isEmpty()) {
            return;
        }

        actions.sort(Comparator.comparing(a -> a.priority));

        for (ActionDTO action : actions) {
            if (action == null || action.type == null) {
                continue;
            }

            switch (action.executor) {
                case BODY:
                    doDBodyAction(action.type, body, newPhyValues, oldPhyValues);
                    break;

                case MODEL:
                    doModelAction(action.type, body, newPhyValues, oldPhyValues);
                    break;

                default:
                    // Nada
            }

            if (body.getState() == BodyState.DEAD) {
                return; // no seguimos con más acciones
            }
        }
    }

    private void doDBodyAction(ActionType action, DynamicBody dBody,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        switch (action) {
            case MOVE:
                dBody.doMovement(newPhyValues);
                break;

            case REBOUND_IN_EAST:
                dBody.reboundInEast(newPhyValues, oldPhyValues,
                        this.worldDim.width, this.worldDim.height);
                break;

            case REBOUND_IN_WEST:
                dBody.reboundInWest(newPhyValues, oldPhyValues,
                        this.worldDim.width, this.worldDim.height);
                break;

            case REBOUND_IN_NORTH:
                dBody.reboundInNorth(newPhyValues, oldPhyValues,
                        this.worldDim.width, this.worldDim.height);
                break;

            case REBOUND_IN_SOUTH:
                dBody.reboundInSouth(newPhyValues, oldPhyValues,
                        this.worldDim.width, this.worldDim.height);
                break;

            case DIE:
                this.killDBody(dBody);
                break;

            case GO_INSIDE:
                // To-Do: lógica futura
                break;

            case NONE:
            default:
                // Nada que hacer
        }
    }

    private void doModelAction(ActionType action, DynamicBody dBody,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        switch (action) {
            case FIRE:
                this.spawnProjectileFrom(dBody, newPhyValues);
                break;

            case DIE:
                this.killDBody(dBody);
                break;

            case EXPLODE_IN_FRAGMENTS:
                break;

            default:
        }
    }

    private boolean isProcessable(AbstractBody entity) {
        return entity != null
                && this.state == ModelState.ALIVE
                && entity.getState() == BodyState.ALIVE;
    }

    private void spawnProjectileFrom(DynamicBody shooter, PhysicsValuesDTO shooterNewPhy) {
        if (!(shooter instanceof PlayerBody)) {
            return;
        }
        PlayerBody pBody = (PlayerBody) shooter;

        Weapon activeWeapon = pBody.getActiveWeapon();
        if (activeWeapon == null) {
            return;
        }

        WeaponDto weaponConfig = activeWeapon.getWeaponConfig();
        if (weaponConfig == null) {
            return;
        }

        double angleDeg = shooterNewPhy.angle;
        double angleRad = Math.toRadians(angleDeg);

        double dirX = Math.cos(angleRad);
        double dirY = Math.sin(angleRad);

        double angleInRads = Math.toRadians(shooterNewPhy.angle - 90);
        double posX = shooterNewPhy.posX + Math.cos(angleInRads) * weaponConfig.shootingOffset;
        double posY = shooterNewPhy.posY + Math.sin(angleInRads) * weaponConfig.shootingOffset;

        // double projSpeedX = weaponConfig.firingSpeed * dirX;
        // double projSpeedY = weaponConfig.firingSpeed * dirY;
        double projSpeedX = shooterNewPhy.speedX + weaponConfig.firingSpeed * dirX;
        double projSpeedY = shooterNewPhy.speedY + weaponConfig.firingSpeed * dirY;

        double accX = weaponConfig.acceleration * dirX;
        double accY = weaponConfig.acceleration * dirY;

        String entityId = this.addDynamicBody(weaponConfig.projectileSize,
                posX, posY, projSpeedX, projSpeedY,
                accX, accY, angleDeg, 0d, 0d, 0d, weaponConfig.maxLifeTime);

        if (entityId == null || entityId.isEmpty()) {
            return; // ======= Max entity quantity reached =======>>
        }
        this.domainEventProcessor.notifyNewProjectileFired(
                entityId, weaponConfig.projectileAssetId);
    }
}