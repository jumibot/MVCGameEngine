package model.ports.body;

public class CreateDynamicBodyDto {
    public final double size;
    public final double posX;
    public final double posY;
    public final double speedX;
    public final double speedY;
    public final double accX;
    public final double accY;
    public final double angle;
    public final double angularSpeed;
    public final double angularAcc;
    public final double thrust;
    public final double maxLifeInSeconds;

    public CreateDynamicBodyDto(double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc,
            double thrust, double maxLifeInSeconds) {
        this.size = size;
        this.posX = posX;
        this.posY = posY;
        this.speedX = speedX;
        this.speedY = speedY;
        this.accX = accX;
        this.accY = accY;
        this.angle = angle;
        this.angularSpeed = angularSpeed;
        this.angularAcc = angularAcc;
        this.thrust = thrust;
        this.maxLifeInSeconds = maxLifeInSeconds;
    }
}