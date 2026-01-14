package world.ports;


/**
 *
 * @author juanm
 */
public class WorldDefItemDTO2 {

    public final String assetId;

    // Geometry
    public final double size;
    public final double angle;


    public WorldDefItemDTO2(String assetId, double size, double angle) {
        this.assetId = assetId;
        this.size = size;
        this.angle = angle;
    }
}
