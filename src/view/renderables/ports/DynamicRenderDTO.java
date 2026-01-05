package view.renderables.ports;

public class DynamicRenderDTO extends RenderDTO {

    public final long timeStamp;
    public final double speedX;
    public final double speedY;
    public final double accX;
    public final double accY;

    public DynamicRenderDTO(
            String entityId,
            double posX, double posY, double angle,
            double size,
            long timeStamp,
            double speedX, double speedY,
            double accX, double accY) {

        super(entityId, posX, posY, angle, size);

        this.timeStamp = timeStamp;
        this.speedX = speedX;
        this.speedY = speedY;
        this.accX = accX;
        this.accY = accY;
    }
}
