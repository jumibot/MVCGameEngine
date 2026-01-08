package model.bodies.implementations;

import model.bodies.core.AbstractBody;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.physics.implementations.NullPhysicsEngine;

public class TemporaryDecoBody extends AbstractBody implements Runnable {

    private final long maxLifeInSeconds; // Infinite life by default

    /**
     * CONSTRUCTORS
     */
    public TemporaryDecoBody(BodyEventProcessor bodyEventProcessor, double size,
            double posX, double posY, double angle, long maxLifeInSeconds) {

        super(
                bodyEventProcessor, new NullPhysicsEngine(size, posX, posY, angle),
                BodyType.TEMPORARY_DECO, maxLifeInSeconds);

        this.maxLifeInSeconds = maxLifeInSeconds;
    }

    /**
     * PUBLICS
     */
    @Override
    public synchronized void activate() {
        this.setState(BodyState.ALIVE);
    }

    @Override
    public void run() {
        while (this.getState() != BodyState.DEAD) {

            if (this.getState() == BodyState.ALIVE) {

                if ((this.maxLifeInSeconds > 0)
                        && (this.getLifeInSeconds() >= this.maxLifeInSeconds)) {
                    this.die();
                }
            }

            try {
                Thread.sleep(30);
            } catch (InterruptedException ex) {
                System.err.println("ERROR Sleeping in vObject thread! (VObject) · " + ex.getMessage());
            }
        }
    }
}
