package world.core;


public class WorldDefBackgroundDto {

    public final String assetId;
    public final double scrollSpeedX;
    public final double scrollSpeedY;


    public WorldDefBackgroundDto(String assetId, double scrollSpeedX, double scrollSpeedY) {

        this.assetId = assetId;
        this.scrollSpeedX = scrollSpeedX;
        this.scrollSpeedY = scrollSpeedY;
    }
}
