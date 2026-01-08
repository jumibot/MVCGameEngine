package model.bodies.implementations;

import model.bodies.core.AbstractBody;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.physics.implementations.NullPhysicsEngine;

public class DecoBody extends AbstractBody {

    /**
     * CONSTRUCTORS
     */
    public DecoBody(BodyEventProcessor bodyEventProcessor, double size, double posX, double posY, double angle, long maxLifeInSeconds) {
        super(bodyEventProcessor, new NullPhysicsEngine(size, posX, posY, angle), BodyType.DECO, maxLifeInSeconds);
    }

    /**
     * PUBLICS
     */
    @Override
    public synchronized void activate() {
        super.activate();
        this.setState(BodyState.ALIVE);
    }
}
