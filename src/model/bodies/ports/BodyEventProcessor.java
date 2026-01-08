package model.bodies.ports;

import model.physics.ports.PhysicsValuesDTO;

public interface BodyEventProcessor {

    public void processBodyEvents(Body body, PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues);

}
