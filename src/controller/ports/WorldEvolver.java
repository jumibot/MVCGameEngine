package controller.ports;

import java.awt.Dimension;

import world.WorldDefWeaponDto;

public interface WorldEvolver {

    public void addDynamicBody(String assetId, double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust);

    public String addPlayer(String assetId, double size, double posX, double posY,
            double speedX, double speedY, double accX, double accY,
            double angle, double angularSpeed, double angularAcc, double thrust);

    public void addWeaponToPlayer(String playerId, WorldDefWeaponDto weaponDef, int shootingOffset);

    public Dimension getWorldDimension();

    public EngineState getEngineState();

    public void setLocalPlayer(String playerId);

}
