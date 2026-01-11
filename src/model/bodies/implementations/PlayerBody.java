package model.bodies.implementations;

import java.util.List;

import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyType;
import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.spatial.core.SpatialGrid;
import model.weapons.ports.Weapon;
import model.weapons.ports.WeaponDto;

public class PlayerBody extends DynamicBody {

    private double maxThrustForce = 80; //
    private double maxAngularAcc = 1000; // degrees*s^-2
    private double angularSpeed = 30; // degrees*s^-1
    private final List<Weapon> weapons = new java.util.ArrayList<>(4);
    private int currentWeaponIndex = -1; // -1 = sin arma
    private double damage = 0D;
    private double energye = 1D;
    private int temperature = 1;
    private double shield = 1D;

    public PlayerBody(BodyEventProcessor bodyEventProcessor, SpatialGrid spatialGrid,
            PhysicsEngine physicsEngine,
            double maxLifeInSeconds) {

        super(bodyEventProcessor, spatialGrid,
                physicsEngine,
                BodyType.PLAYER,
                maxLifeInSeconds);
    }

    public void addWeapon(Weapon weapon) {
        this.weapons.add(weapon);

        if (this.currentWeaponIndex < 0) {
            // Signaling existence of weapon in the spaceship
            this.currentWeaponIndex = 0;
        }
    }

    public Weapon getActiveWeapon() {
        if (this.currentWeaponIndex < 0 || this.currentWeaponIndex >= this.weapons.size()) {
            return null;
        }

        return this.weapons.get(this.currentWeaponIndex);
    }

    public int getActiveWeaponIndex() {
        if (this.currentWeaponIndex < 0 || this.currentWeaponIndex >= this.weapons.size()) {
            return -1;
        }

        return this.currentWeaponIndex;
    }

    public WeaponDto getActiveWeaponConfig() {
        Weapon weapon = getActiveWeapon();
        return (weapon != null) ? weapon.getWeaponConfig() : null;
    }

    public double getAmmoStatusPrimary() {
        return getAmmoStatus(0);
    }

    public double getAmmoStatusSecondary() {
        return getAmmoStatus(1);
    }

    public double getAmmoStatusMines() {
        return getAmmoStatus(2);
    }

    public double getAmmoStatusMissiles() {
        return getAmmoStatus(3);
    }

    private double getAmmoStatus(int weaponIndex) {
        if (weaponIndex < 0 || weaponIndex >= this.weapons.size()) {
            return 0D;
        }

        Weapon weapon = this.weapons.get(weaponIndex);
        if (weapon == null) {
            return 0D;
        }

        return weapon.getAmmoStatus();
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getEnergy() {
        return energye;
    }

    public double getShield() {
        return shield;
    }

    public int getTemperature() {
        return this.temperature;
    }

    public void thrustOn() {
        this.setThrust(this.maxThrustForce);
    }

    public void thrustOff() {
        this.resetAcceleration();
        this.setThrust(0.0d);
    }

    public void registerFireRequest() {
        if (this.currentWeaponIndex < 0 || this.currentWeaponIndex >= this.weapons.size()) {
            System.out.println("> No weapon active or no weapons!");
            return;
        }

        Weapon weapon = this.weapons.get(this.currentWeaponIndex);
        if (weapon == null) {
            // There is no weapon in this slot
            return;
        }

        weapon.registerFireRequest();
    }

    public void reverseThrust() {
        this.setThrust(-this.maxThrustForce);
    }

    public void rotateLeftOn() {
        PhysicsValuesDTO phyValues = this.getPhysicsValues();
        if (phyValues.angularSpeed == 0) {
            this.setAngularSpeed(-this.angularSpeed);
        }

        this.addAngularAcceleration(-this.maxAngularAcc);
    }

    public void rotateRightOn() {
        PhysicsValuesDTO phyValues = this.getPhysicsValues();
        if (phyValues.angularSpeed == 0) {
            this.setAngularSpeed(this.angularSpeed);
        }

        this.addAngularAcceleration(this.maxAngularAcc);
    }

    public void rotateOff() {
        this.setAngularAcceleration(0.0d);
        this.setAngularSpeed(0.0d);
    }

    public void selectNextWeapon() {
        if (this.weapons.size() <= 0) {
            return;
        }

        this.currentWeaponIndex++;
        this.currentWeaponIndex = this.currentWeaponIndex % this.weapons.size();
    }

    public void selectWeapon(int weaponIndex) {
        if (weaponIndex >= 0 && weaponIndex < this.weapons.size()) {
            this.currentWeaponIndex = weaponIndex;
        }
    }

    public void setEnergye(double energye) {
        this.energye = energye;
    }

    public void setMaxThrustForce(double maxThrust) {
        this.maxThrustForce = maxThrust;
    }

    public void setMaxAngularAcceleration(double maxAngularAcc) {
        this.setAngularSpeed(this.angularSpeed);
        this.maxAngularAcc = maxAngularAcc;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public boolean mustFireNow(PhysicsValuesDTO newPhyValues) {
        if (this.currentWeaponIndex < 0 || this.currentWeaponIndex >= this.weapons.size()) {
            return false;
        }

        Weapon weapon = this.weapons.get(this.currentWeaponIndex);
        if (weapon == null) {
            return false;
        }

        double dtNanos = newPhyValues.timeStamp - this.getPhysicsValues().timeStamp;
        double dtSeconds = dtNanos / 1_000_000_000;

        return weapon.mustFireNow(dtSeconds);
    }
}