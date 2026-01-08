package model.bodies.ports;

import model.physics.ports.PhysicsValuesDTO;

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
}
