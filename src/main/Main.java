package main;

/**
 * TO-DO 
 * ===== 
 * 1) Create HUD for player info
 * 2) Colision detection
 * 3) Game rules injection
 * 4) Create a new physic engine with a gravitational field 
 * 5) Basic Fx
 * 6) Comms
 * =====
 */
import assets.ProjectAssets;
import controller.Controller;
import generators.LifeConfigDTO;
import generators.LifeGenerator;
import generators.SceneGenerator;
import model.Model;
import view.View;
import world.RandomWorldDefinitionProvider;
import world.ports.WorldDefinition;
import world.ports.WorldDefinitionProvider;

public class Main {

        public static void main(String[] args) {

                System.setProperty("sun.java2d.uiScale", "1.0");
                int worldWidth = 2450;
                int worldHeight = 1450;

                ProjectAssets projectAssets = new ProjectAssets();

                WorldDefinitionProvider world = new RandomWorldDefinitionProvider(worldWidth, worldHeight,
                                projectAssets);

                WorldDefinition worldDef = world.provide();

                Controller controller = new Controller(
                                worldWidth, worldHeight, // World dimensions
                                3500, // Max dynamic bodies
                                new View(), new Model(),
                                worldDef.gameAssets);

                controller.activate();

                SceneGenerator worldGenerator = new SceneGenerator(controller, worldDef);

                LifeConfigDTO lifeConfig = new LifeConfigDTO(
                                1200, // maxCreationDelay
                                15, 6, // maxSize, minSize
                                1000, 10, // maxMass, minMass
                                175, // maxSpeedModule
                                0); // maxAccModule

                LifeGenerator lifeGenerator = new LifeGenerator(controller, worldDef, lifeConfig);

                lifeGenerator.activate();
        }
}
