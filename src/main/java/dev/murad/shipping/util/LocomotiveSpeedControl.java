package dev.murad.shipping.util;

/** Pure speed-control helpers shared by locomotive movement and its regression tests. */
public final class LocomotiveSpeedControl {
    private LocomotiveSpeedControl() {
    }

    public static double approach(double currentSpeed, double targetSpeed, double maximumChange) {
        if (maximumChange <= 0) return currentSpeed;
        if (currentSpeed < targetSpeed) return Math.min(currentSpeed + maximumChange, targetSpeed);
        if (currentSpeed > targetSpeed) return Math.max(currentSpeed - maximumChange, targetSpeed);
        return targetSpeed;
    }
}
