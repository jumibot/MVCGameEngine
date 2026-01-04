package model.bodies;

import model.physics.BasicPhysicsEngine;
import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.ports.ModelState;
import model.bodies.core.AbstractBody;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.bodies.ports.PhysicsBody;

/**
 * DynamicBody
 * -----------
 *
 * Represents a single dynamic entity in the simulation model.
 *
 * Each DynamicBody maintains:
 * - A unique identifier and visual attributes (assetId, size)
 * - Its own PhysicsEngine instance, which stores and updates the immutable
 * PhysicsValues snapshot (position, speed, acceleration, angle, etc.)
 * - A dedicated thread responsible for advancing its physics state over time
 *
 * Dynamic bodies interact exclusively with the Model, reporting physics updates
 * and requesting event processing (collisions, rebounds, etc.). The view layer
 * never reads mutable state directly; instead, DynamicBody produces a
 * DBodyInfoDTO snapshot encapsulating all visual and physical data required
 * for rendering.
 *
 * Lifecycle control (STARTING → ALIVE → DEAD) is managed internally, and static
 * counters (inherited from AbstractEntity) track global quantities of created,
 * active and dead entities.
 *
 * Threading model
 * ---------------
 * Each DynamicBody runs on its own thread (implements Runnable). The physics
 * engine is updated continuously in the run() loop, with the entity checking
 * for events and processing actions based on game rules determined by the
 * Controller.
 *
 * The goal of this class is to isolate per-object behavior and physics
 * evolution
 * while keeping the simulation thread-safe through immutable snapshots and a
 * clearly separated rendering pipeline.
 */
public class DynamicBody extends AbstractBody implements PhysicsBody, Runnable {

    private Thread thread;
    private final BasicPhysicsEngine phyEngine;

    /**
     * CONSTRUCTORS
     */
    public DynamicBody(BasicPhysicsEngine phyEngine) {
        super(phyEngine, BodyType.DYNAMIC);

        this.phyEngine = phyEngine;
    }

    public DynamicBody(BasicPhysicsEngine phyEngine, double maxLifeInSeconds) {
        super(phyEngine, maxLifeInSeconds, BodyType.DYNAMIC);

        this.phyEngine = phyEngine;
    }

    /**
     * PUBLICS
     */
    @Override
    public synchronized void activate() {
        super.activate();

        this.thread = new Thread(this);
        this.thread.setName("Body " + this.getEntityId());
        this.thread.setPriority(Thread.NORM_PRIORITY - 1);
        this.thread.start();
        this.setState(BodyState.ALIVE);
    }

    public void addAngularAcceleration(double angularSpeed) {
        this.phyEngine.addAngularAcceleration(angularSpeed);
    }

    public PhysicsEngine getPhysicsEngine() {
        return this.phyEngine;
    }

    public void resetAcceleration() {
        this.phyEngine.resetAcceleration();
    }

    @Override
    public void run() {
        PhysicsValuesDTO newPhyValues;

        while ((this.getState() != BodyState.DEAD)
                && (this.getModel().getState() != ModelState.STOPPED)) {

            if ((this.getState() == BodyState.ALIVE)
                    && (this.getModel().getState() == ModelState.ALIVE)) {

                newPhyValues = this.phyEngine.calcNewPhysicsValues();
                this.getModel().processDBodyEvents(this, newPhyValues, this.phyEngine.getPhysicsValues());
            }

            try {
                Thread.sleep(30);
            } catch (InterruptedException ex) {
                System.err.println("ERROR Sleeping in vObject thread! (VObject) · " + ex.getMessage());
            }
        }
    }

    public void setAngularAcceleration(double angularAcc) {
        this.phyEngine.setAngularAcceleration(angularAcc);
    }

    public void setAngularSpeed(double angularSpeed) {
        this.phyEngine.setAngularSpeed(angularSpeed);
    }

    public void setThrust(double thrust) {
        this.phyEngine.setThrust(thrust);
    }
}
