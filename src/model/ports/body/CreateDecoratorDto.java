package model.ports.body;

public class CreateDecoratorDto {
    public final double size;
    public final double posX;
    public final double posY;
    public final double angle;

    public CreateDecoratorDto(double size, double posX, double posY, double angle) {
        this.size = size;
        this.posX = posX;
        this.posY = posY;
        this.angle = angle;
    }
}
