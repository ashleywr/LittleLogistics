/*
MIT License

Copyright (c) 2018 Xavier "jglrxavpok" Niochaut

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package dev.murad.shipping.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public class SpringPhysicsUtil {

    private static final double SOFT_TENSION_RANGE = 0.10D;
    private static final double PULL_RESPONSE = 0.80D;
    private static final double ROUTE_CORRECTION_GAIN = 0.35D;
    private static final double MAX_ROUTE_CORRECTION_SPEED = 0.12D;
    private static final double ROUTE_RESPONSE = 0.65D;

    public static float computeTargetYaw(Float currentYaw, Vec3 anchorPos, Vec3 otherAnchorPos) {
        float idealYaw = (float) (Math.atan2(otherAnchorPos.x - anchorPos.x, -(otherAnchorPos.z - anchorPos.z)) * (180f/Math.PI));
        float closestDistance = Float.POSITIVE_INFINITY;
        float closest = idealYaw;
        for(int sign : Arrays.asList(-1, 0, 1)) {
            float potentialYaw = idealYaw + sign * 360f;
            float distance = Math.abs(potentialYaw - currentYaw);
            if(distance < closestDistance) {
                closestDistance = distance;
                closest = potentialYaw;
            }
        }
        return closest;
    }


    public static <T extends Entity & LinkableEntity<T>> void adjustSpringedEntities(T dominant, T dominated) {
        if (dominated.distanceTo(dominant) > 20) {
            dominated.removeDominant();
            return;
        }

        pullLinkedEntities(dominant, dominated, WaterConvoyGeometry.CRUISING_CENTER_SPACING);
    }

    /**
     * A water link only transmits tension.  In particular, opening a docked
     * convoy to its larger cruising spacing must not push a follower backwards.
     */
    public static <T extends Entity & LinkableEntity<T>> void pullLinkedEntities(T dominant, T dominated, double targetDistance) {
        Vec3 frontAnchor = dominant.position();
        Vec3 backAnchor = dominated.position();
        Vec3 between = new Vec3(frontAnchor.x - backAnchor.x, 0.0D, frontAnchor.z - backAnchor.z);
        double dist = between.length();
        if (dist <= 1.0E-4D) {
            return;
        }

        final double alpha = 0.5;

        float targetYaw = SpringPhysicsUtil.computeTargetYaw(dominated.getYRot(), frontAnchor, backAnchor);
        dominated.setYRot((float) ((alpha * dominated.getYRot() + targetYaw * (1f - alpha)) % 360));
        dominated.setDeltaMovement(calculateFollowerVelocity(
                frontAnchor,
                dominant.getDeltaMovement(),
                backAnchor,
                dominated.getDeltaMovement(),
                targetDistance));
    }

    static Vec3 calculateFollowerVelocity(Vec3 leaderPosition, Vec3 leaderVelocity,
                                          Vec3 followerPosition, Vec3 followerVelocity,
                                          double targetDistance) {
        Vec3 horizontalOffset = new Vec3(
                leaderPosition.x - followerPosition.x,
                0.0D,
                leaderPosition.z - followerPosition.z);
        double distance = horizontalOffset.length();
        if (distance <= 1.0E-4D) {
            return followerVelocity;
        }

        double excess = Math.max(0.0D, distance - targetDistance);
        double tension = Math.min(1.0D, excess / SOFT_TENSION_RANGE);
        double leaderSpeed = Math.hypot(leaderVelocity.x, leaderVelocity.z);

        // Match the leader and add catch-up speed once the link is taut. The soft
        // engagement removes the stop/pull discontinuity at the target distance,
        // while the speed-scaled cap still lets a stretched convoy close its gap.
        double maxPull = Math.max(0.5D, leaderSpeed * 1.5D);
        double desiredSpeed = Math.min(
                maxPull,
                (Math.max(0.03D, leaderSpeed) + excess * 0.35D) * tension);
        Vec3 desiredHorizontal = horizontalOffset.scale(desiredSpeed / distance);
        double retainedVelocity = 1.0D - PULL_RESPONSE;

        return new Vec3(
                followerVelocity.x * retainedVelocity + desiredHorizontal.x * PULL_RESPONSE,
                followerVelocity.y,
                followerVelocity.z * retainedVelocity + desiredHorizontal.z * PULL_RESPONSE);
    }

    public static Vec3 calculateRouteFollowerVelocity(Vec3 followerPosition, Vec3 followerVelocity,
                                                      Vec3 targetPosition, Vec3 routeForward,
                                                      double cruiseSpeed) {
        Vec3 horizontalError = new Vec3(
                targetPosition.x - followerPosition.x,
                0.0D,
                targetPosition.z - followerPosition.z);
        double errorDistance = horizontalError.length();
        Vec3 correction = errorDistance <= 1.0E-4D
                ? Vec3.ZERO
                : horizontalError.scale(
                        Math.min(MAX_ROUTE_CORRECTION_SPEED, errorDistance * ROUTE_CORRECTION_GAIN)
                                / errorDistance);

        Vec3 horizontalForward = new Vec3(routeForward.x, 0.0D, routeForward.z);
        Vec3 cruiseVelocity = horizontalForward.lengthSqr() <= 1.0E-4D
                ? Vec3.ZERO
                : horizontalForward.normalize().scale(cruiseSpeed);
        Vec3 desiredVelocity = cruiseVelocity.add(correction);
        double retainedVelocity = 1.0D - ROUTE_RESPONSE;

        return new Vec3(
                followerVelocity.x * retainedVelocity + desiredVelocity.x * ROUTE_RESPONSE,
                followerVelocity.y,
                followerVelocity.z * retainedVelocity + desiredVelocity.z * ROUTE_RESPONSE);
    }
}
