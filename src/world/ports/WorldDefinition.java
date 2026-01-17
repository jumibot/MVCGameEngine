package world.ports;

import java.util.ArrayList;

import assets.core.AssetCatalog;

public class WorldDefinition {

    public final int worldWidth;
    public final int worldHeight;
    public final AssetCatalog gameAssets;

    public final WorldDefBackgroundDTO background;
    public final ArrayList<WorldDefPositionItemDTO> spaceDecorators;
    public final ArrayList<WorldDefPositionItemDTO> gravityBodies;
    public final ArrayList<WorldDefItemDTO> asteroids;
    public final ArrayList<WorldDefItemDTO> spaceshipsDef;
    public final ArrayList<WorldDefEmitterDTO> trailEmitterDef;
    public final ArrayList<WorldDefWeaponDTO> primaryWeaponDef;
    public final ArrayList<WorldDefWeaponDTO> secondaryWeaponDef;
    public final ArrayList<WorldDefWeaponDTO> mineLaunchersDef;
    public final ArrayList<WorldDefWeaponDTO> missilLaunchersDef;

    public WorldDefinition(
            int worldWidth,
            int worldHeight,
            AssetCatalog gameAssets,
            WorldDefBackgroundDTO background,
            ArrayList<WorldDefPositionItemDTO> spaceDecorators,
            ArrayList<WorldDefPositionItemDTO> gravityBodies,
            ArrayList<WorldDefItemDTO> asteroids,
            ArrayList<WorldDefItemDTO> spaceships,
            ArrayList<WorldDefEmitterDTO> trailEmitter,
            ArrayList<WorldDefWeaponDTO> primaryWeapon,
            ArrayList<WorldDefWeaponDTO> secondaryWeapon,
            ArrayList<WorldDefWeaponDTO> mineLaunchers,
            ArrayList<WorldDefWeaponDTO> missilLaunchers) {

        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.gameAssets = gameAssets;
        this.background = background;
        this.spaceDecorators = spaceDecorators;
        this.gravityBodies = gravityBodies;
        this.asteroids = asteroids;
        this.primaryWeaponDef = primaryWeapon;
        this.secondaryWeaponDef = secondaryWeapon;
        this.mineLaunchersDef = mineLaunchers;
        this.missilLaunchersDef = missilLaunchers;
        this.spaceshipsDef = spaceships;
        this.trailEmitterDef = trailEmitter;
    }
}
