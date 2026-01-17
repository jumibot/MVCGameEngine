package world.ports;

import model.bodies.ports.BodyType;

public class WorldDefEmitterDTO {

    public final BodyType type;
    public final String assetId;
    public final double size;
    public final double xOffset;
    public final double yOffset;
    public final double speed;
    public final double acceleration;
    public final double accelerationTime;
    public final double angularSpeed;
    public final double angularAcc;
    public final double thrust;
    public final int emisionRate;
    public final int maxBodiesEmitted;
    public final double reloadTime;
    public final double bodyMass;
    public final double maxLifeTime;

    public WorldDefEmitterDTO(
            BodyType type,
            String assetId,
            double size,
            double xOffset,
            double yOffset,
            double speed,
            double acceleration,
            double accelerationTime,
            double angularSpeed,
            double angularAcc,
            double thrust,
            int emisionRate,
            int maxBodiesEmitted,
            double reloadTime,
            double bodyMass,
            double maxLifeTime) {

        this.type = type;
        this.assetId = assetId;
        this.size = size;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.speed = speed;
        this.acceleration = acceleration;
        this.accelerationTime = accelerationTime;
        this.angularSpeed = angularSpeed;
        this.angularAcc = angularAcc;
        this.thrust = thrust;
        this.emisionRate = emisionRate;
        this.maxBodiesEmitted = maxBodiesEmitted;
        this.reloadTime = reloadTime;
        this.bodyMass = bodyMass;
        this.maxLifeTime = maxLifeTime;
    }

    // Clone constructor
    public WorldDefEmitterDTO(WorldDefEmitterDTO other) {
        this.type = other.type;
        this.assetId = other.assetId;
        this.size = other.size;
        this.xOffset = other.xOffset;
        this.yOffset = other.yOffset;
        this.speed = other.speed;
        this.acceleration = other.acceleration;
        this.emisionRate = other.emisionRate;
        this.accelerationTime = other.accelerationTime;
        this.angularSpeed = other.angularSpeed;
        this.angularAcc = other.angularAcc;
        this.thrust = other.thrust;
        this.maxBodiesEmitted = other.maxBodiesEmitted;
        this.reloadTime = other.reloadTime;
        this.bodyMass = other.bodyMass;
        this.maxLifeTime = other.maxLifeTime;
    }
}
