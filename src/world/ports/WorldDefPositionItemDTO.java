package world.ports;

public class WorldDefPositionItemDTO extends WorldDefItemDTO {

    // Geometry
    public final double posX;
    public final double posY;


    public WorldDefPositionItemDTO(
            String assetId, double size, double angle, 
            double posX, double posY) {

        super(assetId, size, angle);

        this.posX = posX;
        this.posY = posY;
    }
}
