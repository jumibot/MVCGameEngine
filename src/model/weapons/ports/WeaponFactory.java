package model.weapons.ports;

import model.weapons.BasicWeapon;
import model.weapons.BurstWeapon;
import model.weapons.MineLauncher;
import model.weapons.MissileLauncher;

public class WeaponFactory {

    /**
     * Crete a weapon using type and configuration
     */
    public static Weapon create(WeaponDto weaponConfig) {
        switch (weaponConfig.type) {
            case PRIMARY_WEAPON:
                return new BasicWeapon(weaponConfig);

            case SECONDARY_WEAPON:
                return new BurstWeapon(weaponConfig);

            case MISSILE_LAUNCHER:
                return new MissileLauncher(weaponConfig);

            case MINE_LAUNCHER:
                return new MineLauncher(weaponConfig);

            default:
                throw new IllegalArgumentException(
                        "Tipo de arma desconocido: " + weaponConfig.type);
        }
    }

}
