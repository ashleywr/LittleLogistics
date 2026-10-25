package dev.murad.shipping.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocomotiveSpeedControlTest {
    @Test
    void accelerationStopsAtTargetInsteadOfOvershooting() {
        assertEquals(0.5D, LocomotiveSpeedControl.approach(0.48D, 0.5D, 0.07D), 1.0E-9D);
    }

    @Test
    void brakingApproachesTargetWithoutCrossingIt() {
        assertEquals(0.1D, LocomotiveSpeedControl.approach(0.12D, 0.1D, 0.03D), 1.0E-9D);
    }

    @Test
    void speedChangesByAtMostConfiguredStep() {
        assertEquals(0.27D, LocomotiveSpeedControl.approach(0.2D, 0.5D, 0.07D), 1.0E-9D);
        assertEquals(0.43D, LocomotiveSpeedControl.approach(0.5D, 0.1D, 0.07D), 1.0E-9D);
    }

    @Test
    void zeroStepLeavesSpeedToVanillaRailPhysics() {
        assertEquals(0.3D, LocomotiveSpeedControl.approach(0.3D, 0.0D, 0.0D), 1.0E-9D);
    }
}
