package model.bodies.implementations;

import model.bodies.core.AbstractPhysicsBody;
import model.bodies.ports.BodyEventProcessor;
import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.bodies.ports.PhysicsBody;
import model.physics.implementations.NullPhysicsEngine;

/**
 * StaticBody
 * ----------
 *
 * Represents a single static entity in the simulation model.
 *
 * Each StaticBody maintains:
 * - A unique identifier and visual attributes (assetId, size)
 * - A NullPhysicsEngine instance with fixed position and angle
 * - No dedicated thread (static bodies do not move or update)
 *
 * Static bodies are used for non-moving world elements such as obstacles,
 * platforms, or decorative elements that have physical presence but no
 * dynamic behavior.
 *
 * The view layer accesses static bodies through EntityInfoDTO snapshots,
 * following the same pattern as dynamic bodies but without the time-varying
 * physics data.
 *
 * Lifecycle control (STARTING → ALIVE → DEAD) is managed internally, and static
 * counters (inherited from AbstractEntity) track global quantities of created,
 * active and dead entities.
 *
 * Static vs. Dynamic
 * ------------------
 * Unlike DynamicBody, StaticBody:
 * - Uses NullPhysicsEngine (no physics updates)
 * - Has no thread (no run() loop)
 * - Returns EntityInfoDTO instead of DBodyInfoDTO (no velocity/acceleration)
 * - Is intended for fixed-position world elements
 *
 * This separation keeps the codebase clean and prevents unnecessary overhead
 * for entities that never move.
 */
public class StaticBody extends AbstractPhysicsBody {

    /**
     * CONSTRUCTORS
     */
    public StaticBody(BodyEventProcessor bodyEventProcessor, double size,
            double x, double y, double angle, long maxLifeInSeconds) {
                
        super(bodyEventProcessor, new NullPhysicsEngine(size, x, y, angle), BodyType.STATIC, maxLifeInSeconds);
    }

    /**
     * PUBLICS
     */
    @Override
    public synchronized void activate() {
        super.activate();

        this.setState(BodyState.ALIVE);
    }
}
