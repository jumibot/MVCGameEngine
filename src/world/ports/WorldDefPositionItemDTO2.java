package world.ports;

public class WorldDefPositionItemDTO2 extends WorldDefItemDTO2 {

    // Geometry
    public final double posX;
    public final double posY;


    public WorldDefPositionItemDTO2(
            String assetId, double size, double angle, 
            double posX, double posY) {

        super(assetId, size, angle);

        this.posX = posX;
        this.posY = posY;
    }
}
