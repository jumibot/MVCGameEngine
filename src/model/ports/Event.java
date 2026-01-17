package model.ports;

import model.bodies.ports.Body;
import model.bodies.ports.BodyType;
import model.bodies.implementations.ProjectileBody;

public class Event {

    public final EventType eventType;
    public final String entityIdPrimaryBody;
    public final String entityIdSecondaryBody;
    public final BodyType primaryBodyType;
    public final BodyType secondaryBodyType;
    public final boolean shooterInmunity;

    public Event(Body primaryBody, Body secondaryBody, EventType eventType) {
        if (primaryBody == null) {
            throw new IllegalArgumentException("Primary body cannot be null");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (eventType == EventType.COLLISION && secondaryBody == null) {
            throw new IllegalArgumentException("Secondary body cannot be null for COLLISION events");
        }

        this.eventType = eventType;
        this.entityIdPrimaryBody = primaryBody.getEntityId();
        this.primaryBodyType = primaryBody.getBodyType();
        this.entityIdSecondaryBody = (secondaryBody != null) ? secondaryBody.getEntityId() : null;
        this.secondaryBodyType = (secondaryBody != null) ? secondaryBody.getBodyType() : null;

        if (eventType != EventType.COLLISION) {
            this.shooterInmunity = false;
            return; // Further processing only for COLLISION events ===============>
        }

        Boolean haveInmunity = false;

        // Secondary body have inmunity
        if (primaryBodyType == BodyType.PROJECTILE) {
            ProjectileBody projectile = (ProjectileBody) primaryBody;
            if (projectile.getShooterId().equals(this.entityIdSecondaryBody)) {
                haveInmunity = projectile.isImmune();
            }
        }

        // Primary body have inmunity
        if (secondaryBodyType == BodyType.PROJECTILE) {
            ProjectileBody projectile = (ProjectileBody) secondaryBody;
            if (projectile.getShooterId().equals(this.entityIdPrimaryBody)) {
                haveInmunity = projectile.isImmune();
            }
        }

        this.shooterInmunity = haveInmunity;
    }
}
