package controller.mappers;

import model.weapons.ports.WeaponType;
import world.ports.WorldDefWeaponType;

public class WeaponTypeMapper {
    public static WeaponType fromWorldDef(WorldDefWeaponType type) {
        // At the moment, the mapping is direct based on enum names
        return WeaponType.valueOf(type.name());
    }

}
