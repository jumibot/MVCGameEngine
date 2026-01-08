package model.bodies.core;

import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyType;
import model.bodies.ports.PhysicsBody;
import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;

public class AbstractPhysicsBody extends AbstractBody implements PhysicsBody {

    public AbstractPhysicsBody(BodyEventProcessor bodyEventProcessor, PhysicsEngine phyEngine, BodyType bodyType, double maxLifeInSeconds) {
        super(bodyEventProcessor, phyEngine, bodyType, maxLifeInSeconds);
    }

    public PhysicsValuesDTO getPhysicsValues() {
        return this.getPhysicsEngine().getPhysicsValues();
    }

    public void doMovement(PhysicsValuesDTO phyValues) {
        PhysicsEngine engine = this.getPhysicsEngine();
        engine.setPhysicsValues(phyValues);
    }

    public void reboundInEast(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
            double worldWidth, double worldHeight) {

        PhysicsEngine engine = this.getPhysicsEngine();
        engine.reboundInEast(newVals, oldVals, worldWidth, worldHeight);
    }

    public void reboundInWest(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
            double worldWidth, double worldHeight) {

        PhysicsEngine engine = this.getPhysicsEngine();
        engine.reboundInWest(newVals, oldVals, worldWidth, worldHeight);
    }

    public void reboundInNorth(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
            double worldWidth, double worldHeight) {
        PhysicsEngine engine = this.getPhysicsEngine();
        engine.reboundInNorth(newVals, oldVals, worldWidth, worldHeight);
    }

    public void reboundInSouth(PhysicsValuesDTO newVals, PhysicsValuesDTO oldVals,
            double worldWidth, double worldHeight) {
        PhysicsEngine engine = this.getPhysicsEngine();
        engine.reboundInSouth(newVals, oldVals, worldWidth, worldHeight);
    }
}
