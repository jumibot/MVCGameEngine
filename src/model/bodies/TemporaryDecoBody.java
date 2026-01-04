package model.bodies;

import model.bodies.ports.BodyState;
import model.bodies.ports.BodyType;
import model.ports.ModelState;

public class TemporaryDecoBody extends DecoBody implements Runnable {

    private final long maxLifeInSeconds; // Infinite life by default

    /**
     * CONSTRUCTORS
     */
    public TemporaryDecoBody(double size, double posX, double posY, double angle, long maxLifeInSeconds) {
        super(size, posX, posY, angle, BodyType.TEMPORARY_DECO);

        this.maxLifeInSeconds = maxLifeInSeconds;
    }

    /**
     * PUBLICS
     */
    @Override
    public synchronized void activate() {
        super.activate();
        this.setState(BodyState.ALIVE);
    }

    @Override
    public void run() {
        while ((this.getState() != BodyState.DEAD)
                && (this.getModel().getState() != ModelState.STOPPED)) {

            if ((this.getState() == BodyState.ALIVE)
                    && (this.getModel().getState() == ModelState.ALIVE)) {

                if ((this.maxLifeInSeconds > 0)
                        && (this.getLifeInSeconds() >= this.maxLifeInSeconds)) {
                    this.die();
                }
            }

            try {
                Thread.sleep(30);
            } catch (InterruptedException ex) {
                System.err.println("ERROR Sleeping in vObject thread! (VObject) · " + ex.getMessage());
            }
        }
    }
}
