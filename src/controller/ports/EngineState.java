package controller.ports;


import model.*;
import java.io.Serializable;


/**
 *
 * @author juanm
 */
public enum EngineState implements Serializable {
    STARTING,
    ALIVE,
    PAUSED,
    STOPPED
}
