package model.ports.body;

/**
 * Contract for spawning bodies inside the simulation.
 * <p>
 * Controllers and other collaborators can rely on this interface instead of
 * concrete model implementations when they need to create new entities.
 */
public interface BodyCreator {

    public String createDynamic(CreateDynamicBodyDto request);

    public String createPlayer(CreatePlayerDto request);

    public String createStatic(CreateStaticBodyDto request);

    public String createDecorator(CreateDecoratorDto request);
}