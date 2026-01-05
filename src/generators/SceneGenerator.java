package generators;

import java.util.ArrayList;
import java.util.Random;

import controller.ports.WorldInitializer;
import world.ports.WorldDefPositionItemDto;
import world.ports.WorldDefinition;

public class SceneGenerator {

    private final Random rnd = new Random();

    private final WorldInitializer controller;
    WorldDefinition worldDefinition;

    public SceneGenerator(WorldInitializer worldInitializer, WorldDefinition worldDef) {
        this.controller = worldInitializer;
        this.worldDefinition = worldDef;

        this.createWorld();
    }

    private void createWorld() {
        this.controller.loadAssets(this.worldDefinition.gameAssets);

        this.createSpaceDecorators();
        this.createSBodies();
    }

    private void createSBodies() {
        ArrayList<WorldDefPositionItemDto> sBodies = this.worldDefinition.gravityBodies;

        for (WorldDefPositionItemDto body : sBodies) {
            this.controller.addStaticBody(body.assetId, body.size, body.posX, body.posY, body.angle);
        }
    }

    private void createSpaceDecorators() {
        ArrayList<WorldDefPositionItemDto> decorators = this.worldDefinition.spaceDecorators;

        for (WorldDefPositionItemDto deco : decorators) {
            this.controller.addDecorator(deco.assetId, deco.size, deco.posX, deco.posY, deco.angle);
        }
    }

    private double randomAngularSpeed(double maxAngularSpeed) {
        return this.rnd.nextFloat() * maxAngularSpeed;
    }
}
