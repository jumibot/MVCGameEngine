package model.bodies.ports;

import model.physics.ports.PhysicsValuesDTO;

public class BodyDTO {
    public final String entityId;
    public final BodyType type;
    public final PhysicsValuesDTO physicsValues;

    public BodyDTO(String entityId, BodyType type, PhysicsValuesDTO phyValues) {
        this.entityId = entityId;
        this.type = type;
        this.physicsValues = phyValues;
    }

}
