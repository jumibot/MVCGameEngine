package world.ports;

import assets.AssetCatalog;
import world.core.WorldDefBackgroundDto;

import java.util.ArrayList;

public class WorldDefinition {

    public final int worldWidth;
    public final int worldHeight;

    public final AssetCatalog gameAssets;
    public final WorldDefBackgroundDto background;
    public final ArrayList<WorldDefPositionItemDto> spaceDecorators;
    public final ArrayList<WorldDefPositionItemDto> gravityBodies;
    public final ArrayList<WorldDefItemDto> asteroids;

    public final ArrayList<WorldDefWeaponDto> primaryWeapon;
    public final ArrayList<WorldDefWeaponDto> secondaryWeapon;
    public final ArrayList<WorldDefWeaponDto> mineLaunchers;
    public final ArrayList<WorldDefWeaponDto> missilLaunchers;

    public ArrayList<WorldDefItemDto> spaceshipsDef;

    public WorldDefinition(
            int worldWidth,
            int worldHeight,
            AssetCatalog gameAssets,
            WorldDefBackgroundDto background,
            ArrayList<WorldDefPositionItemDto> spaceDecorators,
            ArrayList<WorldDefPositionItemDto> gravityBodies,

            ArrayList<WorldDefItemDto> asteroids,
            ArrayList<WorldDefItemDto> spaceships,

            ArrayList<WorldDefWeaponDto> primaryWeapon,
            ArrayList<WorldDefWeaponDto> secondaryWeapon,
            ArrayList<WorldDefWeaponDto> mineLaunchers,
            ArrayList<WorldDefWeaponDto> missilLaunchers) {

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
