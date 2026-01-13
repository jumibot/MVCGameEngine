package model.ports;

import model.bodies.ports.Body;
import model.bodies.ports.BodyType;
import model.bodies.implementations.ProjectileBody;

public class Event {

    public final String entityIdPrimaryBody;
    public final String entityIdSecondaryBody;
    public final BodyType primaryBodyType;
    public final BodyType secondaryBodyType;
    public final String primaryShooterId;
    public final String secondaryShooterId;
    public final boolean primaryImmuneToShooter;
    public final boolean secondaryImmuneToShooter;
    public final EventType eventType;

    public Event(Body primaryBody, Body secondaryBody, EventType eventType) {
        if (primaryBody == null) {
            throw new IllegalArgumentException("Primary body cannot be null");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }

        this.entityIdPrimaryBody = primaryBody.getEntityId();
        this.entityIdSecondaryBody = (secondaryBody != null) ? secondaryBody.getEntityId() : null;
        this.primaryBodyType = primaryBody.getBodyType();
        this.secondaryBodyType = (secondaryBody != null) ? secondaryBody.getBodyType() : null;
        
        // Extract shooter info only if primary body is a projectile
        if (primaryBody instanceof ProjectileBody) {
            ProjectileBody projectile = (ProjectileBody) primaryBody;
            this.primaryShooterId = projectile.getShooterId();
            this.primaryImmuneToShooter = projectile.isImmuneToShooter();
        } else {
            this.primaryShooterId = null;
            this.primaryImmuneToShooter = false;
        }
        
        // Extract shooter info only if secondary body is a projectile
        if (secondaryBody instanceof ProjectileBody) {
            ProjectileBody projectile = (ProjectileBody) secondaryBody;
            this.secondaryShooterId = projectile.getShooterId();
            this.secondaryImmuneToShooter = projectile.isImmuneToShooter();
        } else {
            this.secondaryShooterId = null;
            this.secondaryImmuneToShooter = false;
        }
        
        this.eventType = eventType;
    }
}

