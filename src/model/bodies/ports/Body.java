package model.bodies.ports;

import model.physics.ports.PhysicsValuesDTO;
import model.spatial.core.SpatialGrid;

public interface Body {
    public void activate();

    public void die();

    public long getBornTime();

    public String getEntityId();

    public double getLifeInSeconds();

    public double getLifePercentage();

    public double getMaxLife();

    public PhysicsValuesDTO getPhysicsValues();

    public BodyState getState();

    public BodyType getBodyType();

    public boolean isLifeOver();

    public void setState(BodyState state);

    public SpatialGrid getSpatialGrid();

    int[] getScratchIdxs();
}
