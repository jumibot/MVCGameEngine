package model.bodies.implementations;

import java.util.List;

import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyType;
import model.bodies.ports.PlayerDTO;
import model.emitter.implementations.BasicEmitter;
import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;
import model.spatial.core.SpatialGrid;
import model.weapons.ports.Weapon;
import model.weapons.ports.WeaponDto;

public class PlayerBody extends DynamicBody {

    private BasicEmitter emitter;
    private final List<Weapon> weapons = new java.util.ArrayList<>(4);
    private int currentWeaponIndex = -1; // -1 = sin arma
    private double damage = 0D;
    private double energye = 1D;
    private int temperature = 1;
    private double shield = 1D;
    private int score = 0;

    public PlayerBody(BodyEventProcessor bodyEventProcessor,
            SpatialGrid spatialGrid,
            PhysicsEngine physicsEngine,
            double maxLifeInSeconds) {

        super(bodyEventProcessor,
                spatialGrid,
                physicsEngine,
                BodyType.PLAYER,
                maxLifeInSeconds);

        this.setMaxThrustForce(80);
        this.setMaxAngularAcceleration(1000);
        this.setAngularSpeed(30);
    }

    public void addEmitter(BasicEmitter emitter) {
        if (emitter == null) {
            throw new IllegalStateException("Emitter is null. Cannot add to player body.");
        }
        this.emitter = emitter;
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

    public PlayerDTO getData() {
        PlayerDTO playerData = new PlayerDTO(
                this.getEntityId(),
                "",
                this.damage,
                this.energye,
                this.shield,
                this.temperature,
                this.getActiveWeaponIndex(),
                this.getAmmoStatusPrimary(),
                this.getAmmoStatusSecondary(),
                this.getAmmoStatusMines(),
                this.getAmmoStatusMissiles(),
                this.score);
        return playerData;
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

    public BasicEmitter getEmitter() {
        return this.emitter;
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
        this.setThrust(-this.getMaxThrustForce());
    }

    public void rotateLeftOn() {
        PhysicsValuesDTO phyValues = this.getPhysicsValues();

        if (phyValues.angularSpeed == 0) {
            this.setAngularSpeed(-this.getAngularSpeed());
        }

        this.addAngularAcceleration(-this.getMaxAngularAcceleration());
    }

    public void rotateRightOn() {
        PhysicsValuesDTO phyValues = this.getPhysicsValues();
        if (phyValues.angularSpeed == 0) {
            this.setAngularSpeed(this.getAngularSpeed());
        }

        this.addAngularAcceleration(this.getMaxAngularAcceleration());
    }

    public void rotateOff() {
        this.setAngularAcceleration(0.0d);
        this.setAngularSpeed(0.0d);
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public void setEmitter(BasicEmitter emitter) {
        this.emitter = emitter;
    }

    public void setEnergye(double energye) {
        this.energye = energye;
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

    public void setShield(double shield) {
        this.shield = shield;
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

    public boolean mustTrailEmit(PhysicsValuesDTO newPhyValues) {
        if (this.emitter == null) {
            return false;
        }

        double dtNanos = newPhyValues.timeStamp - this.getPhysicsValues().timeStamp;
        double dtSeconds = dtNanos / 1_000_000_000;

        return this.emitter.mustEmitNow(dtSeconds);
    }
}