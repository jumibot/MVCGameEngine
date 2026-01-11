package model.implementations;

import static java.lang.System.nanoTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.List;

import model.bodies.core.AbstractBody;
import model.bodies.implementations.DecoBody;
import model.bodies.implementations.DynamicBody;
import model.bodies.implementations.PlayerBody;
import model.bodies.implementations.StaticBody;
import model.physics.implementations.BasicPhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.bodies.ports.Body;
import model.bodies.ports.BodyDTO;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.bodies.ports.PhysicsBody;
import model.bodies.ports.PlayerDTO;
import model.ports.ActionDTO;
import model.ports.ActionType;
import model.ports.DomainEventProcessor;
import model.ports.Event;
import model.ports.EventType;
import model.ports.ModelState;
import model.spatial.core.SpatialGrid;
import model.spatial.ports.SpatialGridStatisticsDTO;
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

public class Model implements BodyEventProcessor {

    private int maxDynamicBodies;

    private DomainEventProcessor domainEventProcessor = null;
    private volatile ModelState state = ModelState.STARTING;

    private static final int MAX_ENTITIES = 5000;
    private final double worldWidth;
    private final double worldHeight;
    private final SpatialGrid spatialGrid;
    private final Map<String, Body> dynamicBodies = new ConcurrentHashMap<>(MAX_ENTITIES);
    private final Map<String, Body> decorators = new ConcurrentHashMap<>(100);
    private final Map<String, Body> gravityBodies = new ConcurrentHashMap<>(50);
    private final Map<String, Body> playerBodies = new ConcurrentHashMap<>(10);
    private final Map<String, Body> staticBodies = new ConcurrentHashMap<>(100);

    /**
     * CONSTRUCTORS
     */
    public Model(double worldWidth, double worldHeight, int maxDynamicBodies) {
        if (worldWidth <= 0 || worldHeight <= 0) {
            throw new IllegalArgumentException("Invalid world dimension");
        }

        if (maxDynamicBodies <= 0) {
            throw new IllegalArgumentException("Max dynamic bodies not set");
        }

        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.maxDynamicBodies = maxDynamicBodies;
        this.spatialGrid = new SpatialGrid(64, (int) worldWidth, (int) worldHeight, 16);
    }

    /**
     * PUBLIC
     */
    public void activate() {
        if (this.domainEventProcessor == null) {
            throw new IllegalArgumentException("Controller is not set");
        }

        this.state = ModelState.ALIVE;
    }

    public String addDynamicBody(double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust, double maxLifeInSeconds) {

        if (AbstractBody.getAliveQuantity() >= this.maxDynamicBodies) {
            return null; // ========= Max vObject quantity reached ==========>>
        }

        PhysicsValuesDTO phyVals = new PhysicsValuesDTO(nanoTime(), posX, posY, angle, size,
                speedX, speedY, accX, accY, angularSpeed, angularAcc, thrust);

        DynamicBody dBody = new DynamicBody(
                this, this.spatialGrid, new BasicPhysicsEngine(phyVals), BodyType.DYNAMIC, maxLifeInSeconds);

        dBody.activate();
        this.dynamicBodies.put(dBody.getEntityId(), dBody);

        return dBody.getEntityId();
    }

    public String addDecorator(double size, double posX, double posY, double angle, long maxLifeInSeconds) {
        DecoBody deco = new DecoBody(this, this.spatialGrid, size, posX, posY, angle, maxLifeInSeconds);

        deco.activate();
        this.decorators.put(deco.getEntityId(), deco);

        return deco.getEntityId();
    }

    public String addPlayer(double size,
            double posX, double posY, double speedX, double speedY,
            double accX, double accY,
            double angle, double angularSpeed, double angularAcc,
            double thrust, long maxLifeInSeconds) {

        if (AbstractBody.getAliveQuantity() >= this.maxDynamicBodies) {
            return null; // ========= Max vObject quantity reached ==========>
        }

        PhysicsValuesDTO phyVals = new PhysicsValuesDTO(
                nanoTime(), posX, posY, angle, size,
                speedX, speedY, accX, accY,
                angularSpeed, angularAcc, thrust);

        PlayerBody pBody = new PlayerBody(
                this, this.spatialGrid, new BasicPhysicsEngine(phyVals), maxLifeInSeconds);

        pBody.activate();
        String entityId = pBody.getEntityId();
        this.dynamicBodies.put(entityId, pBody);
        this.playerBodies.put(entityId, pBody);

        return entityId;
    }

    public String addStaticBody(double size,
            double posX, double posY, double angle, long maxLifeInSeconds) {

        StaticBody sBody = new StaticBody(this, this.spatialGrid, size, posX, posY, angle, maxLifeInSeconds);

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

    public int getMaxDynamicBodies() {
        return this.maxDynamicBodies;
    }

    public ArrayList<BodyDTO> getDynamicsData() {
        return this.getBodiesData(this.dynamicBodies);
    }

    public ArrayList<BodyDTO> getStaticsData() {
        ArrayList<BodyDTO> staticsInfo;

        staticsInfo = this.getBodiesData(this.decorators);
        staticsInfo.addAll(this.getBodiesData(this.staticBodies));
        staticsInfo.addAll(this.getBodiesData(this.gravityBodies));

        return staticsInfo;
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

    public PlayerDTO getPlayerData(String playerId) {
        PlayerBody pBody = (PlayerBody) this.playerBodies.get(playerId);
        if (pBody == null) {
            return null;
        }

        PlayerDTO playerData = new PlayerDTO(
                pBody.getEntityId(),
                "",
                pBody.getDamage(),
                pBody.getEnergy(),
                pBody.getShield(),
                pBody.getTemperature(),
                pBody.getActiveWeaponIndex(),
                pBody.getAmmoStatusPrimary(),
                pBody.getAmmoStatusSecondary(),
                pBody.getAmmoStatusMines(),
                pBody.getAmmoStatusMissiles());

        return playerData;
    }

    public SpatialGridStatisticsDTO getSpatialGridStatistics() {
        return this.spatialGrid.getStatistics();
    }

    public Dimension getWorldDimension() {
        return new Dimension((int) this.worldWidth, (int) this.worldHeight);
    }

    public boolean isAlive() {
        return this.state == ModelState.ALIVE;
    }

    public void killDynamicBody(Body body) {
        body.die();
        this.spatialGrid.remove(body.getEntityId());
        this.dynamicBodies.remove(body.getEntityId());
    }

    public void playerFire(String playerId) {
        PlayerBody pBody = (PlayerBody) this.playerBodies.get(playerId);
        if (pBody != null) {
            pBody.registerFireRequest();
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

    public void processBodyEvents(Body body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (!isProcessable(body)) {
            return; // To avoid duplicate or unnecesary event processing ======>
        }

        BodyState previousState = body.getState();
        body.setState(BodyState.HANDS_OFF);

        try {
            List<Event> events = this.detectEvents(body, newPhyValues, oldPhyValues);

            List<ActionDTO> actions = this.resolveActionsForEvents(events);

            this.doActions(body, actions, newPhyValues, oldPhyValues);

        } catch (Exception e) { // Fallback anti-zombi
            if (body.getState() == BodyState.HANDS_OFF) {
                body.setState(previousState);
            }

        } finally { // Getout: off HANDS_OFF ... if leaving
            if (body.getState() == BodyState.HANDS_OFF) {
                body.setState(BodyState.ALIVE);
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

    public void setDomainEventProcessor(DomainEventProcessor domainEventProcessor) {
        this.domainEventProcessor = domainEventProcessor;
    }

    public void setMaxDynamicBodies(int maxDynamicBody) {
        this.maxDynamicBodies = maxDynamicBody;
    }

    /**
     * PRIVATE
     */

    private List<Event> checkCollisions(PhysicsBody checkBody, PhysicsValuesDTO phyValues) {
        List<Event> collisionEvents = new ArrayList<>(4);
        BodyType checkBodyType = checkBody.getBodyType();

        // ArrayList<VO> vos = this.getVOList();
        // ArrayList<VO> voCollided = new ArrayList<VO>();

        this.dynamicBodies.forEach((entityId, body) -> {
            if (checkBody != body) {

                if (this.isProcessable(body)) {
                    // if (vo.getBoundingEllipse().intersects(vod.getBoundingBox())) {
                    // if (vod.getBoundingEllipse().intersects(vo.getBoundingBox())) {
                    body.setState(BodyState.COLLIDED);
                    checkBody.setState(BodyState.COLLIDED);
                    collisionEvents.add(new Event(checkBody, body, EventType.COLLIDED));
                    // Create event
                    // }
                    // }
                }
            }
        });

        return collisionEvents;
    }

    private List<Event> checkLimitEvents(Body body, PhysicsValuesDTO phyValues) {
        List<Event> limitEvents = new ArrayList<>(4);

        if (phyValues.posX < 0) {
            limitEvents.add(new Event(body, null, EventType.REACHED_EAST_LIMIT));
        }

        if (phyValues.posX >= this.worldWidth) {
            limitEvents.add(new Event(body, null, EventType.REACHED_WEST_LIMIT));
        }

        if (phyValues.posY < 0) {
            limitEvents.add(new Event(body, null, EventType.REACHED_NORTH_LIMIT));
        }

        if (phyValues.posY >= this.worldHeight) {
            limitEvents.add(new Event(body, null, EventType.REACHED_SOUTH_LIMIT));
        }

        return limitEvents;
    }

    private List<Event> detectEvents(Body checkBody,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        List<Event> events = this.checkLimitEvents(checkBody, newPhyValues);

        events.addAll(this.checkCollisions(checkBody, newPhyValues));

        if (checkBody.getBodyType() == BodyType.PLAYER) {
            if (((PlayerBody) checkBody).mustFireNow(newPhyValues)) {
                events.add(new Event(checkBody, null, EventType.MUST_FIRE));
            }
        }

        if (checkBody.isLifeOver()) {
            events.add(new Event(checkBody, null, EventType.LIFE_OVER));
        }

        // Eventos de colisión, zonas, etc.
        return events;
    }

    private void doActions(
            Body body, List<ActionDTO> actions,
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
                    this.doBodyAction(action.type, body, newPhyValues, oldPhyValues);
                    break;

                case PHYSICS_BODY:
                    this.doPhysicsBodyAction(action.type, (PhysicsBody) body, newPhyValues, oldPhyValues);
                    break;

                case MODEL:
                    this.doModelAction(action.type, body, newPhyValues, oldPhyValues);
                    break;

                default:
                    // Nada
            }

            if (body.getState() == BodyState.DEAD) {
                return; // no seguimos con más acciones
            }
        }
    }

    private void doBodyAction(ActionType action, Body body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        switch (action) {
            case DIE:
                this.killDynamicBody(body);
                break;

            case NONE:
            default:
                // Nothing to do...
        }
    }

    private void doPhysicsBodyAction(ActionType action, PhysicsBody body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        switch (action) {
            case MOVE:
                body.doMovement(newPhyValues);
                break;

            case REBOUND_IN_EAST:
                body.reboundInEast(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                break;

            case REBOUND_IN_WEST:
                body.reboundInWest(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                break;

            case REBOUND_IN_NORTH:
                body.reboundInNorth(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                break;

            case REBOUND_IN_SOUTH:
                body.reboundInSouth(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                break;

            case DIE:
                this.killDynamicBody(body);
                break;

            case GO_INSIDE:
                // To-Do: lógica futura
                break;

            case NONE:
            default:
                // Nothing to do...
        }
    }

    private void doModelAction(ActionType action, Body body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        switch (action) {
            case FIRE:
                this.spawnProjectileFrom(body, newPhyValues);
                break;

            case DIE:
                this.killDynamicBody(body);
                break;

            case EXPLODE_IN_FRAGMENTS:
                break;

            default:
        }
    }

    private ArrayList<BodyDTO> getBodiesData(Map<String, Body> bodies) {
        ArrayList<BodyDTO> bodyData = new ArrayList<BodyDTO>(bodies.size());

        bodies.forEach((entityId, body) -> {
            BodyDTO bodyInfo = new BodyDTO(entityId, body.getBodyType(), body.getPhysicsValues());
            if (bodyInfo != null) {
                bodyData.add(bodyInfo);
            }
        });

        return bodyData;
    }

    private boolean isProcessable(Body entity) {
        return entity != null
                && this.state == ModelState.ALIVE
                && entity.getState() == BodyState.ALIVE;
    }

    private List<ActionDTO> resolveActionsForEvents(List<Event> events) {

        List<ActionDTO> actionsFromController = this.domainEventProcessor.decideActions(events);

        if (actionsFromController == null || actionsFromController.isEmpty()) {
            return null; // ======== No actions to process =======>
        }

        List<ActionDTO> actions = new ArrayList<>(actionsFromController.size());
        for (ActionDTO a : actionsFromController) {
            if (a != null && a.type != null && a.type != ActionType.NONE) {
                actions.add(a);
            }
        }

        return actions;
    }

    private void spawnProjectileFrom(Body shooter, PhysicsValuesDTO shooterNewPhy) {
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