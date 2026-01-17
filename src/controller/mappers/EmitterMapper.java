package controller.mappers;

import model.emitter.ports.EmitterDto;
import world.ports.WorldDefEmitterDTO;

public class EmitterMapper {

    public static EmitterDto fromWorldDef(
            WorldDefEmitterDTO emitterDef) {

        if (emitterDef == null) {
            return null;
        }
        return new EmitterDto(
                emitterDef.type,
                emitterDef.assetId,
                emitterDef.size,
                emitterDef.xOffset,
                emitterDef.yOffset,
                emitterDef.speed,
                emitterDef.acceleration,
                emitterDef.accelerationTime,
                emitterDef.angularSpeed,
                emitterDef.angularAcc,
                emitterDef.thrust,
                emitterDef.emisionRate,
                emitterDef.maxBodiesEmitted,
                emitterDef.reloadTime,
                emitterDef.bodyMass,
                emitterDef.maxLifeTime);
    }
}