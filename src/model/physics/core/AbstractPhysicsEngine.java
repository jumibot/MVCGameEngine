package model.physics.core;

import java.util.concurrent.atomic.AtomicReference;

import model.physics.ports.PhysicsEngine;
import model.physics.ports.PhysicsValuesDTO;

public abstract class AbstractPhysicsEngine implements PhysicsEngine {

        private final AtomicReference<PhysicsValuesDTO> phyValues; // *+

        /**
         * CONSTRUCTORS
         */
        public AbstractPhysicsEngine(PhysicsValuesDTO phyValues) {
                this.phyValues = new AtomicReference<>(phyValues);
        }

        public AbstractPhysicsEngine(double size, double posX, double posY, double angle) {
                this.phyValues = new AtomicReference<>(
                                new PhysicsValuesDTO(size, posX, posY, angle));
        }

        /**
         * PUBLIC
         */

        public abstract PhysicsValuesDTO calcNewPhysicsValues();

        public abstract void addAngularAcceleration(double angularAcc);

        public PhysicsValuesDTO getPhysicsValues() {
                return this.phyValues.get();
        }

        public abstract void reboundInEast(
                        PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues,
                        double worldDim_x, double worldDim_y);

        public abstract void reboundInWest(
                        PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues,
                        double worldDim_x, double worldDim_y);

        public abstract void reboundInNorth(
                        PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues,
                        double worldDim_x, double worldDim_y);

        public abstract void reboundInSouth(
                        PhysicsValuesDTO newPhyValues, PhysicsValuesDTO oldPhyValues,
                        double worldDim_x, double worldDim_y);

        public void resetAcceleration() {
                PhysicsValuesDTO old = this.getPhysicsValues();
                this.setPhysicsValues(new PhysicsValuesDTO(
                                old.timeStamp,
                                old.posX, old.posY, old.angle,
                                old.size,
                                old.speedX, old.speedY,
                                0, 0,
                                old.angularSpeed,
                                old.angularAcc,
                                old.thrust));

        }

        public void setAngularAcceleration(double angularAcc) {
                PhysicsValuesDTO old = this.getPhysicsValues();
                this.setPhysicsValues(new PhysicsValuesDTO(
                                old.timeStamp,
                                old.posX, old.posY, old.angle,
                                old.size,
                                old.speedX, old.speedY,
                                old.accX, old.accY,
                                old.angularSpeed,
                                angularAcc,
                                old.thrust));
        }

        public abstract void setAngularSpeed(double angularSpeed);

        public void setPhysicsValues(PhysicsValuesDTO phyValues) {
                this.phyValues.set(phyValues);
        }

        public void setThrust(double thrust) {
                PhysicsValuesDTO old = this.getPhysicsValues();
                this.setPhysicsValues(new PhysicsValuesDTO(
                                old.timeStamp,
                                old.posX, old.posY, old.angle,
                                old.size,
                                old.speedX, old.speedY,
                                old.accX, old.accY,
                                old.angularSpeed,
                                old.angularAcc,
                                thrust));
        }
}
