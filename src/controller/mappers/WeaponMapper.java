package controller.mappers;

import model.weapons.ports.WeaponDto;
import world.ports.WorldDefWeaponDTO;

public class WeaponMapper {

    public static WeaponDto fromWorldDef(WorldDefWeaponDTO weaponDef, int shootingOffset) {
        if (weaponDef == null) {
            return null;
        }
        return new WeaponDto(
            WeaponTypeMapper.fromWorldDef(weaponDef.type),
                weaponDef.assetId,
                weaponDef.size,
                weaponDef.projectileSpeed,
                weaponDef.acceleration,
                weaponDef.accelerationDuration,
                weaponDef.burstSize,
                weaponDef.burstFireRate,
                weaponDef.fireRate,
                weaponDef.maxAmmo,
                weaponDef.reloadTime,
                weaponDef.projectileMass,
                weaponDef.maxLifetimeInSeconds,
                shootingOffset
        );   
    }
} 