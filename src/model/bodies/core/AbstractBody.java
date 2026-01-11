package model.bodies.core;

import java.util.ArrayList;
import java.util.UUID;

import model.bodies.ports.Body;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.spatial.core.SpatialGrid;

/**
 *
 * @author juanm
 */
public abstract class AbstractBody implements Body {

    private static volatile int aliveQuantity = 0;
    private static volatile int createdQuantity = 0;
    private static volatile int deadQuantity = 0;

    private final BodyEventProcessor bodyEventProcessor;
    private volatile BodyState state;
    private final BodyType bodyType;
    private final String entityId;
    private final PhysicsEngine phyEngine;
    private final long bornTime = System.nanoTime();
    private final double maxLifeInSeconds; // Infinite life by default

    // Buffers for collision detection and avoiding garbage creation during the
    // physics update. ==> Zero allocation strategy
    private final SpatialGrid spatialGrid;
    private final int[] scratchIdxs;
    private final ArrayList<String> collisionCandidates;

    /**
     * CONSTRUCTORS
     */

    public AbstractBody(BodyEventProcessor bodyEventProcessor, SpatialGrid spatialGrid,
            PhysicsEngine phyEngine, BodyType bodyType,
            double maxLifeInSeconds) {

        this.bodyEventProcessor = bodyEventProcessor;
        this.phyEngine = phyEngine;
        this.bodyType = bodyType;
        this.maxLifeInSeconds = maxLifeInSeconds;

        this.spatialGrid = spatialGrid;
        this.scratchIdxs = new int[spatialGrid.getMaxCellsPerBody()];
        this.collisionCandidates = new ArrayList<>(32);

        this.entityId = UUID.randomUUID().toString();
        this.state = BodyState.STARTING;
    }

    @Override
    public synchronized void activate() {
        if (this.state != BodyState.STARTING) {
            throw new IllegalArgumentException("Entity activation error due is not starting!");
        }

        AbstractBody.aliveQuantity++;
        this.state = BodyState.ALIVE;
    }

    @Override
    public synchronized void die() {
        this.state = BodyState.DEAD;
        AbstractBody.deadQuantity++;
        AbstractBody.aliveQuantity--;
    }

    @Override
    public long getBornTime() {
        return this.bornTime;
    }

    @Override
    public String getEntityId() {
        return this.entityId;
    }

    @Override
    public double getLifeInSeconds() {
        return (System.nanoTime() - this.bornTime) / 1_000_000_000.0D;
    }

    @Override
    public double getLifePercentage() {
        if (this.maxLifeInSeconds <= 0) {
            return 1D;
        }

        return Math.min(1D, this.getLifeInSeconds() / this.maxLifeInSeconds);
    }

    @Override
    public double getMaxLife() {
        return this.maxLifeInSeconds;
    }

    public PhysicsEngine getPhysicsEngine() {
        return this.phyEngine;
    }

    @Override
    public PhysicsValuesDTO getPhysicsValues() {
        return this.phyEngine.getPhysicsValues();
    }

    @Override
    public int[] getScratchIdxs() {
        return this.scratchIdxs;
    }

    @Override
    public SpatialGrid getSpatialGrid() {
        return this.spatialGrid;
    }

    @Override
    public BodyState getState() {
        return this.state;
    }

    @Override
    public BodyType getBodyType() {
        return this.bodyType;
    }

    @Override
    public boolean isLifeOver() {
        if (this.maxLifeInSeconds <= 0) {
            return false;
        }

        return this.getLifeInSeconds() >= this.maxLifeInSeconds;
    }

    public void processBodyEvents(AbstractBody body, PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues) {
        this.bodyEventProcessor.processBodyEvents(body, newPhyValues, oldPhyValues);
    }

    @Override
    public void setState(BodyState state) {
        this.state = state;
    }

    /**
     * 
     * STATICS
     */
    // @Override
    static public int getCreatedQuantity() {
        return AbstractBody.createdQuantity;
    }

    // @Override
    static public int getAliveQuantity() {
        return AbstractBody.aliveQuantity;
    }

    // @Override
    static public int getDeadQuantity() {
        return AbstractBody.deadQuantity;
    }

    // @Override
    static protected int incCreatedQuantity() {
        AbstractBody.createdQuantity++;

        return AbstractBody.createdQuantity;
    }

    // @Override
    static protected int incAliveQuantity() {
        AbstractBody.aliveQuantity++;

        return AbstractBody.aliveQuantity;
    }

    // @Override
    static protected int decAliveQuantity() {
        AbstractBody.aliveQuantity--;

        return AbstractBody.aliveQuantity;
    }

    // @Override
    static protected int incDeadQuantity() {
        AbstractBody.deadQuantity++;

        return AbstractBody.deadQuantity;
    }
}
