package model.bodies;

import model.physics.BasicPhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.weapons.ports.Weapon;
import model.weapons.ports.WeaponDto;

public class PlayerBody extends DynamicBody {

    private double maxThrustForce = 80; //
    private double maxAngularAcc = 1000; // degrees*s^-2
    private double angularSpeed = 30; // degrees*s^-1
    private final java.util.List<Weapon> weapons = new java.util.ArrayList<>(4);
    private int currentWeaponIndex = -1; // -1 = sin arma
    private double healthPercentage = 1D;
    

    public PlayerBody(BasicPhysicsEngine physicsEngine) {
        super(physicsEngine);
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

    public WeaponDto getActiveWeaponConfig() {
        Weapon weapon = getActiveWeapon();
        return (weapon != null) ? weapon.getWeaponConfig() : null;
    }

    public void thrustOn() {
        this.setThrust(this.maxThrustForce);
    }

    public void thrustOff() {
        this.resetAcceleration();
        this.setThrust(0.0d);
    }

    public void requestFire() {
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

    public void setMaxThrustForce(double maxThrust) {
        this.maxThrustForce = maxThrust;
    }

    public void setMaxAngularAcceleration(double maxAngularAcc) {
        this.setAngularSpeed(this.angularSpeed);
        this.maxAngularAcc = maxAngularAcc;
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