package model.weapons;

import model.weapons.core.AbstractWeapon;
import model.weapons.ports.WeaponDto;
import model.weapons.ports.WeaponState;

public class BurstWeapon extends AbstractWeapon {

    private int shotsRemainingInBurst = 0;

    public BurstWeapon(WeaponDto weaponConfig) {
        super(weaponConfig);
    }

    @Override
    public boolean mustFireNow(double dtSeconds) {
        if (this.getCooldown() > 0) {
            // Cool down weapon between shots or between bursts.
            // Any pending requests.
            this.decCooldown(dtSeconds);
            this.markAllRequestsHandled();
            return false; // ======== Weapon is overheated =========>
        }

        if (this.getCurrentAmmo() <= 0) {
            // No ammunition: set time to reload, reload and discard requests
            this.setState(WeaponState.RELOADING);
            this.markAllRequestsHandled();
            this.shotsRemainingInBurst = 0; // cancel any ongoing burst
            this.setCooldown(this.getWeaponConfig().reloadTime);
            this.setCurrentAmmo(this.getWeaponConfig().maxAmmo);
            return false;
        }

        if (this.shotsRemainingInBurst > 0) {
            // Burst mode ongoing...
            // Discard any pending requests while in burst mode
            this.markAllRequestsHandled();

            this.shotsRemainingInBurst--;
            this.decCurrentAmmo();

            if (this.shotsRemainingInBurst == 0) {
                // Burst finished. Cooldown between bursts
                this.setCooldown(1.0 / this.getWeaponConfig().fireRate);
            } else {
                // More shots to fire in this burst. Cooldown between shots
                this.setCooldown(1.0 / this.getWeaponConfig().burstFireRate);
            }

            return true; // ======== Requesting shot ======>
        }

        if (!this.hasRequest()) {
            return false; // ===== No burst to start ======>
        }

        // Consume request and start new burst
        this.markAllRequestsHandled();

        int burstSize = Math.max(1, getWeaponConfig().burstSize);

        this.shotsRemainingInBurst = burstSize - 1; // One shot now
        this.decCurrentAmmo();

        // Cooldown depends on whether burst continues
        if (this.shotsRemainingInBurst == 0) {
            this.setCooldown(1.0 / this.getWeaponConfig().fireRate); // between bursts
        } else {
            this.setCooldown(1.0 / this.getWeaponConfig().burstFireRate); // between burst shots
        }
        
        return true; // ====== Requesting first shot ======>
    }
}
