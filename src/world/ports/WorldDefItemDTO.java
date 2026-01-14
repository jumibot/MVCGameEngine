package world.ports;


/**
 *
 * @author juanm
 */
public class WorldDefItemDTO {

    public final String assetId;

    // Geometry
    public final double size;
    public final double angle;


    public WorldDefItemDTO(String assetId, double size, double angle) {
        this.assetId = assetId;
        this.size = size;
        this.angle = angle;
    }
}
