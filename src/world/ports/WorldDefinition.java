package world.ports;

import java.util.ArrayList;

import assets.core.AssetCatalog;

public class WorldDefinition {

    public final int worldWidth;
    public final int worldHeight;

    public final AssetCatalog gameAssets;
    public final WorldDefBackgroundDTO background;
    public final ArrayList<WorldDefPositionItemDTO2> spaceDecorators;
    public final ArrayList<WorldDefPositionItemDTO2> gravityBodies;
    public final ArrayList<WorldDefItemDTO2> asteroids;

    public final ArrayList<WorldDefWeaponDTO2> primaryWeapon;
    public final ArrayList<WorldDefWeaponDTO2> secondaryWeapon;
    public final ArrayList<WorldDefWeaponDTO2> mineLaunchers;
    public final ArrayList<WorldDefWeaponDTO2> missilLaunchers;

    public ArrayList<WorldDefItemDTO2> spaceshipsDef;

    public WorldDefinition(
            int worldWidth,
            int worldHeight,
            AssetCatalog gameAssets,
            WorldDefBackgroundDTO background,
            ArrayList<WorldDefPositionItemDTO2> spaceDecorators,
            ArrayList<WorldDefPositionItemDTO2> gravityBodies,

            ArrayList<WorldDefItemDTO2> asteroids,
            ArrayList<WorldDefItemDTO2> spaceships,

            ArrayList<WorldDefWeaponDTO2> primaryWeapon,
            ArrayList<WorldDefWeaponDTO2> secondaryWeapon,
            ArrayList<WorldDefWeaponDTO2> mineLaunchers,
            ArrayList<WorldDefWeaponDTO2> missilLaunchers) {

        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.gameAssets = gameAssets;
        this.background = background;
        this.spaceDecorators = spaceDecorators;
        this.gravityBodies = gravityBodies;
        this.asteroids = asteroids;
        this.primaryWeapon = primaryWeapon;
        this.secondaryWeapon = secondaryWeapon;
        this.mineLaunchers = mineLaunchers;
        this.missilLaunchers = missilLaunchers;
        this.spaceshipsDef = spaceships;
    }
}
