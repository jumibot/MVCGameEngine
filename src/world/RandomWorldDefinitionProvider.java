package world;

import assets.AssetCatalog;
import assets.AssetInfoDTO;
import assets.AssetType;
import assets.ProjectAssets;
import java.util.ArrayList;
import java.util.Random;

import world.core.WorldDefBackgroundDto;
import world.ports.WorldDefItemDto;
import world.ports.WorldDefPositionItemDto;
import world.ports.WorldDefWeaponDto;
import world.ports.WorldDefWeaponType;
import world.ports.WorldDefinition;
import world.ports.WorldDefinitionProvider;

public class RandomWorldDefinitionProvider implements WorldDefinitionProvider {

    private final Random rnd = new Random();
    private final int width;
    private final int height;
    private final ProjectAssets projectAssets;
    private final AssetCatalog gameAssets = new AssetCatalog("src/resources/images/");

    private WorldDefBackgroundDto background;

    private ArrayList<WorldDefPositionItemDto> decoratorsDef = new ArrayList<>();
    private ArrayList<WorldDefPositionItemDto> gravityBodiesDef = new ArrayList<>();

    private ArrayList<WorldDefItemDto> asteroidsDef = new ArrayList<>();
    private ArrayList<WorldDefItemDto> spaceshipsDef = new ArrayList<>();

    private ArrayList<WorldDefWeaponDto> primaryWeapon = new ArrayList<>();
    private ArrayList<WorldDefWeaponDto> secondaryWeaponDef = new ArrayList<>();
    private ArrayList<WorldDefWeaponDto> mineLaunchersDef = new ArrayList<>();
    private ArrayList<WorldDefWeaponDto> missilLaunchersDef = new ArrayList<>();

    public RandomWorldDefinitionProvider(int worldWidth, int worldHeight, ProjectAssets assets) {
        this.width = worldWidth;
        this.height = worldHeight;
        this.projectAssets = assets;
    }

    @Override
    public WorldDefinition provide() {

        this.background = randomBackgroundDef();

        this.decorators(this.decoratorsDef, 1, AssetType.STARS, 150, 75);

        this.staticBodies(this.gravityBodiesDef, 1, AssetType.PLANET, 200, 100);

        this.staticBodies(this.gravityBodiesDef, 1, AssetType.MOON, 80, 40);

        this.staticBodies(this.gravityBodiesDef, 1, AssetType.SUN, 40, 20);

        this.staticBodies(this.gravityBodiesDef, 1, AssetType.BLACK_HOLE, 55, 45);

        this.dynamicBodies(this.asteroidsDef, 5, AssetType.ASTEROID, 35, 15);

        this.dynamicBodies(this.spaceshipsDef, 1, AssetType.SPACESHIP, 40, 40);

        this.primaryWeapon(this.primaryWeapon, 1, AssetType.BULLET,
                15, 15, 350d, 8);

        this.secondaryWeapon(this.secondaryWeaponDef, 1, AssetType.BULLET,
                7, 7, 1000d, 8,
                190, 5);

        this.mineLaunchers(this.mineLaunchersDef, 1, AssetType.MINE,
                35, 35, 1);

        this.missilLaunchers(this.missilLaunchersDef, 1, AssetType.MISSILE,
                42, 42,
                6000d, 1d, 4);

        WorldDefinition worlDef = new WorldDefinition(this.width, this.height, this.gameAssets,
                background, decoratorsDef, gravityBodiesDef, asteroidsDef, spaceshipsDef,
                primaryWeapon, secondaryWeaponDef, missilLaunchersDef, mineLaunchersDef);

        return worlDef;
    }

    private double randomAngle() {
        return this.rnd.nextFloat() * 360d;
    }

    private void decorators(
            ArrayList<WorldDefPositionItemDto> decos,
            int num, AssetType type,
            int maxSize, int minSize) {

        String randomId;
        AssetInfoDTO assetInfo;

        for (int i = 0; i < num; i++) {
            randomId = this.projectAssets.catalog.randomId(type);
            assetInfo = this.projectAssets.catalog.get(randomId);
            this.gameAssets.register(assetInfo);

            decos.add(new WorldDefPositionItemDto(
                    randomId,
                    this.randomSize(maxSize, minSize),
                    this.randomAngle(),
                    rnd.nextDouble() * this.width, // x
                    rnd.nextDouble() * this.height)); // y
        }
    }

    private void dynamicBodies(ArrayList<WorldDefItemDto> dBodies,
            int num, AssetType type,
            int maxSize, int minSize) {

        String randomId;
        AssetInfoDTO assetInfo;

        for (int i = 0; i < num; i++) {
            randomId = this.projectAssets.catalog.randomId(type);
            assetInfo = this.projectAssets.catalog.get(randomId);
            this.gameAssets.register(assetInfo);

            dBodies.add(new WorldDefItemDto(
                    randomId,
                    this.randomSize(maxSize, minSize),
                    this.randomAngle()));
        }
    }

    private void staticBodies(
            ArrayList<WorldDefPositionItemDto> sBodies,
            int num, AssetType type,
            int maxSize, int minSize) {

        String randomId;
        AssetInfoDTO assetInfo;

        for (int i = 0; i < num; i++) {
            randomId = this.projectAssets.catalog.randomId(type);
            assetInfo = this.projectAssets.catalog.get(randomId);
            this.gameAssets.register(assetInfo);

            sBodies.add(new WorldDefPositionItemDto(
                    randomId,
                    this.randomSize(maxSize, minSize),
                    this.randomAngle(),
                    rnd.nextDouble() * this.width,
                    rnd.nextDouble() * this.height));
        }
    }

    private WorldDefBackgroundDto randomBackgroundDef() {
        String randomId = this.projectAssets.catalog.randomId(AssetType.BACKGROUND);
        AssetInfoDTO assetInfo = this.projectAssets.catalog.get(randomId);
        this.gameAssets.register(assetInfo);

        return new WorldDefBackgroundDto(randomId, 0.0d, 0.0d);
    }

    private double randomSize(int maxSize, int minSize) {
        return (minSize + (this.rnd.nextFloat() * (maxSize - minSize)));
    }

    private void mineLaunchers(ArrayList<WorldDefWeaponDto> weapons, int num, AssetType type,
            int maxSize, int minSize, int fireRate) {

        String randomId;
        AssetInfoDTO assetInfo;

        for (int i = 0; i < num; i++) {
            randomId = this.projectAssets.catalog.randomId(type);
            assetInfo = this.projectAssets.catalog.get(randomId);
            this.gameAssets.register(assetInfo);

            weapons.add(new WorldDefWeaponDto(
                    randomId, this.randomSize(maxSize, minSize),
                    WorldDefWeaponType.MINE_LAUNCHER,
                    0, 0, 0,
                    1, 0, fireRate, 2, 10,
                    10000, 20));
        }
    }

    private void primaryWeapon(ArrayList<WorldDefWeaponDto> weapons, int num, AssetType type,
            int maxSize, int minSize, double firingSpeed, int fireRate) {

        String randomAssetId;
        AssetInfoDTO assetInfo;

        for (int i = 0; i < num; i++) {
            randomAssetId = this.projectAssets.catalog.randomId(type);
            assetInfo = this.projectAssets.catalog.get(randomAssetId);
            this.gameAssets.register(assetInfo);

            weapons.add(new WorldDefWeaponDto(
                    randomAssetId, this.randomSize(maxSize, minSize),
                    WorldDefWeaponType.PRIMARY_WEAPON,
                    firingSpeed, 0, 0,
                    1, 0, fireRate, 100, 2,
                    100, 6D));
        }
    }

    private void secondaryWeapon(ArrayList<WorldDefWeaponDto> weapons,
            int num, AssetType type,
            int maxSize, int minSize, double projectileSpeed,
            int burstSize, int burstFireRate, int fireRate) {

        String randomAssetId;
        AssetInfoDTO assetInfo;

        for (int i = 0; i < num; i++) {
            randomAssetId = this.projectAssets.catalog.randomId(type);
            assetInfo = this.projectAssets.catalog.get(randomAssetId);
            this.gameAssets.register(assetInfo);

            weapons.add(new WorldDefWeaponDto(
                    randomAssetId,
                    this.randomSize(maxSize, minSize),
                    WorldDefWeaponType.SECONDARY_WEAPON,
                    projectileSpeed, 0, 0,
                    burstSize, burstFireRate, fireRate,
                    200, 4,
                    10, 0.40D));
        }
    }

    private void missilLaunchers(ArrayList<WorldDefWeaponDto> weapons,
            int num, AssetType type,
            int maxSize, int minSize,
            double acceleration, double accelerationDuration, int fireRate) {

        String randomId;
        AssetInfoDTO assetInfo;

        for (int i = 0; i < num; i++) {
            randomId = this.projectAssets.catalog.randomId(type);
            assetInfo = this.projectAssets.catalog.get(randomId);
            this.gameAssets.register(assetInfo);

            weapons.add(new WorldDefWeaponDto(
                    randomId, this.randomSize(maxSize, minSize),
                    WorldDefWeaponType.MISSILE_LAUNCHER,
                    0, acceleration, accelerationDuration,
                    1, 0, fireRate, 4, 4,
                    1000, 1D));

        }
    }
}
