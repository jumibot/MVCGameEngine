package model.bodies.implementations;

import model.bodies.core.AbstractPhysicsBody;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.spatial.core.SpatialGrid;

/**
 * ProjectileBody
 * --------------
 * 
 * Represents a projectile entity in the simulation (bullets, missiles, etc.).
 * 
 * Key characteristics:
 * - Has a shooterId to track who fired it
 * - Temporary immunity period to prevent collision with shooter at launch
 * - Limited lifetime (auto-destruction after maxLifeInSeconds)
 * - Runs on its own thread for independent physics updates
 * 
 * Design notes:
 * - Extends DynamicBody behavior but with PROJECTILE type for clearer semantics
 * - The shooterId enables immunity and collision rules specific to projectiles
 * - Future enhancements: damage values, penetration, homing behavior, etc.
 */
public class ProjectileBody extends AbstractPhysicsBody implements Runnable {

    private static final double SHOOTER_IMMUNITY_TIME = 1; // seconds
    private final String shooterId; // ID of the entity that shot this projectile
    private Thread thread;

    //
    //  CONSTRUCTORS
    //

    public ProjectileBody(
            BodyEventProcessor bodyEventProcessor, 
            SpatialGrid spatialGrid,
            PhysicsEngine phyEngine, 
            double maxLifeInSeconds, 
            String shooterId) {

        super(bodyEventProcessor, spatialGrid,
                phyEngine,
                BodyType.PROJECTILE,
                maxLifeInSeconds);
        
        this.shooterId = shooterId;
    }

    /**
     * PUBLICS
     */
    @Override
    public synchronized void activate() {
        super.activate();

        this.thread = new Thread(this);
        this.thread.setName("Projectile " + this.getEntityId());
        this.thread.setPriority(Thread.NORM_PRIORITY - 1);
        this.thread.start();
        this.setState(BodyState.ALIVE);
    }

    public void addAngularAcceleration(double angularSpeed) {
        this.getPhysicsEngine().addAngularAcceleration(angularSpeed);
    }

    public void resetAcceleration() {
        this.getPhysicsEngine().resetAcceleration();
    }

    public void setAngularSpeed(double angularSpeed) {
        this.getPhysicsEngine().setAngularSpeed(angularSpeed);
    }

    public void setThrust(double thrust) {
        this.getPhysicsEngine().setThrust(thrust);
    }

    /**
     * @return The ID of the entity that fired this projectile
     */
    public String getShooterId() {
        return this.shooterId;
    }

    /**
     * @return true if projectile is still within immunity period (< 0.2s old)
     *         to prevent collision with shooter at launch
     */
    public boolean isImmune() {
        return this.shooterId != null && 
               this.getLifeInSeconds() < SHOOTER_IMMUNITY_TIME;
    }

    /**
     * Physics update loop - runs continuously until projectile dies
     */
    @Override
    public void run() {
        PhysicsValuesDTO newPhyValues;

        while (this.getState() != BodyState.DEAD) {

            if (this.getState() == BodyState.ALIVE) {
                newPhyValues = this.getPhysicsEngine().calcNewPhysicsValues();

                double r = newPhyValues.size * 0.5;
                double minX = newPhyValues.posX - r;
                double maxX = newPhyValues.posX + r;
                double minY = newPhyValues.posY - r;
                double maxY = newPhyValues.posY + r;

                this.getSpatialGrid().upsert(
                        this.getEntityId(), minX, maxX, minY, maxY, this.getScratchIdxs());

                this.processBodyEvents(this, newPhyValues, this.getPhysicsEngine().getPhysicsValues());
            }

            try {
                Thread.sleep(30); // ~33 FPS update rate
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
