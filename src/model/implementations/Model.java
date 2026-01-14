package model.implementations;

import static java.lang.System.nanoTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Dimension;
import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import model.bodies.core.AbstractBody;
import model.bodies.implementations.DecoBody;
import model.bodies.implementations.DynamicBody;
import model.bodies.implementations.PlayerBody;
import model.bodies.implementations.ProjectileBody;
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
import model.ports.ActionExecutor;
import model.ports.ActionPriority;
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
        this.spatialGrid = new SpatialGrid(128, (int) worldWidth, (int) worldHeight, 16);
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

        Body body = new DynamicBody(
                this, this.spatialGrid, new BasicPhysicsEngine(phyVals),
                BodyType.DYNAMIC, maxLifeInSeconds);

        body.activate();
        this.dynamicBodies.put(body.getEntityId(), body);
        this.upsertCommittedToGrid(body);

        return body.getEntityId();
    }

    public String addProjectile(double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust,
            double maxLifeInSeconds, String shooterId) {

        if (AbstractBody.getAliveQuantity() >= this.maxDynamicBodies) {
            return null; // ========= Max vObject quantity reached ==========>>
        }

        PhysicsValuesDTO phyVals = new PhysicsValuesDTO(nanoTime(), posX, posY, angle, size,
                speedX, speedY, accX, accY, angularSpeed, angularAcc, thrust);

        ProjectileBody projectile = new ProjectileBody(
                this, this.spatialGrid, new BasicPhysicsEngine(phyVals),
                maxLifeInSeconds, shooterId);

        projectile.activate();
        this.dynamicBodies.put(projectile.getEntityId(), projectile);
        this.upsertCommittedToGrid(projectile);

        return projectile.getEntityId();
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
        this.upsertCommittedToGrid(pBody);

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
            List<ActionDTO> actions = null;

            if (events != null && !events.isEmpty())
                actions = this.domainEventProcessor.decideActions(events);

            if (actions == null)
                actions = new ArrayList<>(4);

            // MOVE is the default action to commit physics values when no other
            // PHYSICS_BODY action (rebound, teleport, etc.) is already doing it
            boolean hasPhysicsBodyAction = actions.stream()
                    .anyMatch(a -> a.executor == ActionExecutor.PHYSICS_BODY);

            if (!hasPhysicsBodyAction) {
                actions.add(new ActionDTO(body.getEntityId(),
                        ActionType.MOVE, ActionExecutor.PHYSICS_BODY, ActionPriority.NORMAL));
            }

            this.doActions(actions, newPhyValues, oldPhyValues);

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

    //
    // PRIVATE
    //

    private List<Event> checkCollisions(Body checkBody, PhysicsValuesDTO newPhyValues) {
        if (checkBody == null || newPhyValues == null)
            return List.of();

        if (!this.isCollidable(checkBody))
            return List.of();

        final String checkBodyId = checkBody.getEntityId();

        ArrayList<String> candidates = checkBody.getScratchCandidateIds();
        this.spatialGrid.queryCollisionCandidates(checkBodyId, candidates);
        if (candidates.isEmpty())
            return List.of();

        List<Event> collisionEvents = null;
        HashSet<String> seen = checkBody.getScratchSeenCandidateIds();
        seen.clear();
        for (String bodyId : candidates) {
            if (bodyId == null || bodyId.isEmpty())
                continue;

            // Dedupe by multiple references y differents cells
            if (!seen.add(bodyId))
                continue;

            // Dedupe by symetry
            if (checkBodyId.compareTo(bodyId) >= 0)
                continue;

            final Body otherBody = this.dynamicBodies.get(bodyId);
            if (otherBody == null)
                continue;

            if (!this.isCollidable(otherBody))
                continue;

            final PhysicsValuesDTO otherPhyValues = otherBody.getPhysicsValues();
            if (otherPhyValues == null)
                continue;

            if (!intersectsCircleCircle(newPhyValues, otherPhyValues))
                continue;

            if (collisionEvents == null) {
                collisionEvents = new ArrayList<>(8);
            }
            collisionEvents.add(new Event(checkBody, otherBody, EventType.COLLISIONED));
        }
        return collisionEvents == null ? List.of() : collisionEvents;
    }

    private List<Event> checkLimitEvents(Body body, PhysicsValuesDTO phyValues) {
        // List<Event> limitEvents = new ArrayList<>(4);

        ArrayList<Event> limitEvents = null;
        if (phyValues.posX < 0) {
            if (limitEvents == null)
                limitEvents = new ArrayList<>(2);
            limitEvents.add(new Event(body, null, EventType.REACHED_EAST_LIMIT));
        }

        if (phyValues.posX >= this.worldWidth) {
            if (limitEvents == null)
                limitEvents = new ArrayList<>(2);
            limitEvents.add(new Event(body, null, EventType.REACHED_WEST_LIMIT));
        }

        if (phyValues.posY < 0) {
            if (limitEvents == null)
                limitEvents = new ArrayList<>(2);
            limitEvents.add(new Event(body, null, EventType.REACHED_NORTH_LIMIT));
        }

        if (phyValues.posY >= this.worldHeight) {
            if (limitEvents == null)
                limitEvents = new ArrayList<>(1);
            limitEvents.add(new Event(body, null, EventType.REACHED_SOUTH_LIMIT));
        }

        return limitEvents == null ? List.of() : limitEvents;
    }

    private List<Event> detectEvents(Body checkBody,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        ArrayList<Event> events = null;

        // 1) Limits
        final List<Event> limitEvents = this.checkLimitEvents(checkBody, newPhyValues);
        if (limitEvents != null && !limitEvents.isEmpty()) {
            events = new ArrayList<>(limitEvents.size() + 4);
            events.addAll(limitEvents);
        }

        // 2) Collisions
        final List<Event> collisionEvents = this.checkCollisions(checkBody, newPhyValues);
        if (collisionEvents != null && !collisionEvents.isEmpty()) {
            if (events == null) {
                events = new ArrayList<>(collisionEvents.size() + 4);
            }
            events.addAll(collisionEvents);
        }

        // 3) Player fire
        if (checkBody.getBodyType() == BodyType.PLAYER) {
            if (((PlayerBody) checkBody).mustFireNow(newPhyValues)) {
                if (events == null)
                    events = new ArrayList<>(2);
                events.add(new Event(checkBody, null, EventType.MUST_FIRE));
            }
        }

        // 4) Life over
        if (checkBody.isLifeOver()) {
            if (events == null)
                events = new ArrayList<>(1);
            events.add(new Event(checkBody, null, EventType.LIFE_OVER));
        }

        return events == null ? List.of() : events;
    }

    private void doActions(
            List<ActionDTO> actions, PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (actions == null || actions.isEmpty()) {
            return;
        }

        actions.sort(Comparator.comparing(a -> a.priority));

        for (ActionDTO action : actions) {
            if (action == null || action.type == null) {
                continue;
            }

            Body targetBody = this.dynamicBodies.get(action.entityId);
            if (targetBody == null) {
                continue; // Body already removed, skip this action
            }

            switch (action.executor) {
                case BODY:
                    this.doBodyAction(action.type, targetBody, newPhyValues, oldPhyValues);
                    break;

                case PHYSICS_BODY:
                    this.doPhysicsBodyAction(action.type, (PhysicsBody) targetBody, newPhyValues, oldPhyValues);
                    break;

                case MODEL:
                    this.doModelAction(action.type, targetBody, newPhyValues, oldPhyValues);
                    break;

                default:
                    // Nada
            }
        }
    }

    private void doBodyAction(ActionType action, Body body,
            PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {

        if (body == null) {
            return;
        }

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

        if (body == null) {
            return;
        }

        switch (action) {
            case MOVE:
                body.doMovement(newPhyValues);
                upsertCommittedToGrid((Body) body);
                break;

            case REBOUND_IN_EAST:
                body.reboundInEast(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                upsertCommittedToGrid((Body) body);
                break;

            case REBOUND_IN_WEST:
                body.reboundInWest(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                upsertCommittedToGrid((Body) body);
                break;

            case REBOUND_IN_NORTH:
                body.reboundInNorth(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                upsertCommittedToGrid((Body) body);
                break;

            case REBOUND_IN_SOUTH:
                body.reboundInSouth(newPhyValues, oldPhyValues,
                        this.worldWidth, this.worldHeight);
                upsertCommittedToGrid((Body) body);
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

        if (body == null) {
            return;
        }

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

    private boolean intersectsCircleCircle(PhysicsValuesDTO a, PhysicsValuesDTO b) {
        // OJO: asumo size = diámetro. Si size ya es radio: ra=a.size; rb=b.size;
        final double ra = a.size * 0.5;
        final double rb = b.size * 0.5;

        final double dx = a.posX - b.posX;
        final double dy = a.posY - b.posY;
        final double r = ra + rb;

        return (dx * dx + dy * dy) <= (r * r);
    }

    private boolean isCollidable(Body b) {
        return b != null
                && b.getState() != BodyState.DEAD;
    }

    private boolean isProcessable(Body entity) {
        return entity != null
                && this.state == ModelState.ALIVE
                && entity.getState() == BodyState.ALIVE;
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

        String entityId = this.addProjectile(weaponConfig.projectileSize,
                posX, posY, projSpeedX, projSpeedY,
                accX, accY, angleDeg, 0d, 0d, 0d, weaponConfig.maxLifeTime,
                shooter.getEntityId());

        if (entityId == null || entityId.isEmpty()) {
            return; // ======= Max entity quantity reached =======>>
        }
        this.domainEventProcessor.notifyNewProjectileFired(
                entityId, weaponConfig.projectileAssetId);
    }

    private void upsertCommittedToGrid(Body body) {
        if (body == null)
            return;

        final PhysicsValuesDTO phyValues = body.getPhysicsValues();

        final double r = phyValues.size * 0.5; // si size es radio, r = committed.size
        final double minX = phyValues.posX - r;
        final double maxX = phyValues.posX + r;
        final double minY = phyValues.posY - r;
        final double maxY = phyValues.posY + r;

        this.spatialGrid.upsert(body.getEntityId(), minX, maxX, minY, maxY, body.getScratchIdxs());
    }
}