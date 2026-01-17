package model.ports;


import java.io.Serializable;


public enum EventType implements Serializable {
    COLLISION,
    MUST_FIRE,
    NONE,
    REACHED_NORTH_LIMIT,
    REACHED_SOUTH_LIMIT,
    REACHED_EAST_LIMIT,
    REACHED_WEST_LIMIT,
    TRY_TO_GO_INSIDE,
    LIFE_OVER, 
    THRUST_ON
}
