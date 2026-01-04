package model.bodies;

import model.bodies.core.AbstractBody;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.physics.NullPhysicsEngine;

public class DecoBody extends AbstractBody {

    /**
     * CONSTRUCTORS
     */
    public DecoBody(double size, double posX, double posY, double angle) {
        super(new NullPhysicsEngine(size, posX, posY, angle), BodyType.DECO);
    }

    public DecoBody(double size, double posX, double posY, double angle, BodyType bodyType) {
        super(new NullPhysicsEngine(size, posX, posY, angle), bodyType);
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
