package model.bodies.implementations;

import model.bodies.core.AbstractBody;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.physics.implementations.NullPhysicsEngine;
import model.spatial.core.SpatialGrid;

public class DecoBody extends AbstractBody {

    /**
     * CONSTRUCTORS
     */
    public DecoBody(BodyEventProcessor bodyEventProcessor, SpatialGrid spatialGrid,
            double size, double posX, double posY, double angle,
            long maxLifeInSeconds) {

        super(bodyEventProcessor, spatialGrid,
                new NullPhysicsEngine(size, posX, posY, angle),
                BodyType.DECO,
                maxLifeInSeconds);
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
