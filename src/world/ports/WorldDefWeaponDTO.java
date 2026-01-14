package world.ports;

public class WorldDefWeaponDTO extends WorldDefItemDTO {

    public WorldDefWeaponType type; // Weapon type
    public final double projectileSpeed; // Projectile initial speed
    public final double acceleration; // Projectile acceleration (if applicable)
    public final double accelerationDuration; // Time during which the acceleration applies
    public final int burstSize; // Number of shots per burst
    public final int fireRate; // Fire rate (shots per second)
    public final int burstFireRate; // Fire rate within a burst (shots per second)
    public final int maxAmmo; // Maximum ammunition capacity
    public final double reloadTime; // Reload time (seconds)
    public final double projectileMass; // Mass of the projectile (kilograms)
    public final double maxLifetimeInSeconds; // Maximum lifetime of the projectile (seconds)

    public WorldDefWeaponDTO(String assetId, double size, WorldDefWeaponType type,
            double projectileSpeed, double acceleration, double accelerationDuration,
            int burstSize, int burstFireRate,int fireRate, int maxAmmo, double reloadTime, 
            double projectileMass, double maxLifetimeInSeconds) {

        super(assetId, size, 0);

        this.type = type;
        this.projectileSpeed = projectileSpeed;
        this.acceleration = acceleration;
        this.accelerationDuration = accelerationDuration;
        this.burstSize = burstSize;
        this.burstFireRate = burstFireRate;
        this.fireRate = fireRate;
        this.maxAmmo = maxAmmo;
        this.reloadTime = reloadTime;
        this.projectileMass = projectileMass;
        this.maxLifetimeInSeconds = maxLifetimeInSeconds;
    }
}
