package model.ports.body;

/**
 * Data required to spawn a static body.
 */
public class CreateStaticBodyDto {

    public final double size;
    public final double posX;
    public final double posY;
    public final double angle;

    public CreateStaticBodyDto(double size, double posX, double posY, double angle) {
        this.size = size;
        this.posX = posX;
        this.posY = posY;
        this.angle = angle;
    }
}