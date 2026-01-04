package model.weapons;

import model.weapons.core.AbstractWeapon;
import model.weapons.ports.WeaponDto;
import model.weapons.ports.WeaponState;

public class BasicWeapon extends AbstractWeapon {

    public BasicWeapon(WeaponDto weaponConfig) {
        super(weaponConfig);
    }

    @Override
    public boolean mustFireNow(double dtSeconds) {
        if (this.getCooldown() > 0) {
            // Cool down weapon. Any pending requests are discarded.
            this.decCooldown(dtSeconds);
            this.markAllRequestsHandled();
            return false; // ======== Weapon is overheated =========>
        }

        if (this.getCurrentAmmo() <= 0) {
            // No ammunition: reload, set time to reload and discard requests
            this.setState   (WeaponState.RELOADING);
            this.markAllRequestsHandled();
            this.setCooldown(this.getWeaponConfig().reloadTime);
            this.setCurrentAmmo(this.getWeaponConfig().maxAmmo);
            return false;
        }

        this.setState(WeaponState.READY);
        if (!this.hasRequest()) {
            // Nothing to do
            this.setCooldown(0);
            return false; // ==================>
        }

        // Fire
        this.markAllRequestsHandled();
        this.decCurrentAmmo();
        this.setCooldown(1.0 / this.getWeaponConfig().fireRate);
        return true;
    }
}
