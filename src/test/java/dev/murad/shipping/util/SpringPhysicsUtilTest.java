package dev.murad.shipping.util;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpringPhysicsUtilTest {

    private static final float EPSILON = 0.001f;

    @Test
    void computeTargetYaw_straightNorth() {
        // anchor at origin, other anchor to the north (negative Z)
        float yaw = SpringPhysicsUtil.computeTargetYaw(0f, new Vec3(0, 0, 0), new Vec3(0, 0, -1));
        // atan2(dx=0, -dz=1) = 0 degrees
        assertEquals(0f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_straightEast() {
        float yaw = SpringPhysicsUtil.computeTargetYaw(90f, new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        // atan2(dx=1, -dz=0) = 90 degrees
        assertEquals(90f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_straightSouth() {
        float yaw = SpringPhysicsUtil.computeTargetYaw(180f, new Vec3(0, 0, 0), new Vec3(0, 0, 1));
        // atan2(dx=0, -dz=-1) = 180 degrees
        assertEquals(180f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_straightWest() {
        float yaw = SpringPhysicsUtil.computeTargetYaw(-90f, new Vec3(0, 0, 0), new Vec3(-1, 0, 0));
        // atan2(dx=-1, -dz=0) = -90 degrees
        assertEquals(-90f, yaw, EPSILON);
    }

    @Test
    void computeTargetYaw_choosesClosestWrap() {
        // Current yaw is 170, ideal is -170 (equivalent to 190).
        // Without wrapping, distance is 340. With +360 wrap (190), distance is 20.
        float yaw = SpringPhysicsUtil.computeTargetYaw(170f, new Vec3(0, 0, 0), new Vec3(-1, 0, 0.176f));
        // Should pick the value closest to 170, which is the +360 variant
        assertTrue(Math.abs(yaw - 170f) < 180f, "Should choose wrap closest to current yaw 170, got " + yaw);
    }

    @Test
    void computeTargetYaw_negativeCurrentYaw() {
        // Current yaw is -170, ideal is 170.
        // Should wrap to -190 (170 - 360) which is 20 away from -170.
        float yaw = SpringPhysicsUtil.computeTargetYaw(-170f, new Vec3(0, 0, 0), new Vec3(1, 0, 0.176f));
        assertTrue(Math.abs(yaw - (-170f)) < 180f, "Should choose wrap closest to current yaw -170, got " + yaw);
    }

    @Test
    void calculateFollowerVelocity_preservesBuoyancy() {
        Vec3 velocity = SpringPhysicsUtil.calculateFollowerVelocity(
                new Vec3(0, 2, 0), new Vec3(0.2, -0.4, 0),
                new Vec3(-2, 0, 0), new Vec3(0, 0.07, 0),
                1.2D);

        assertEquals(0.07D, velocity.y, 1.0E-9D);
        assertEquals(0.0D, velocity.z, 1.0E-9D);
        assertTrue(velocity.x > 0.2D, "A stretched follower should catch a 0.2 block/tick leader");
    }

    @Test
    void calculateFollowerVelocity_dampsSlackWithoutStoppingAbruptly() {
        Vec3 velocity = SpringPhysicsUtil.calculateFollowerVelocity(
                Vec3.ZERO, Vec3.ZERO,
                new Vec3(-1.1, 0, 0), new Vec3(0.2, 0.03, 0),
                1.2D);

        assertTrue(velocity.x > 0.0D && velocity.x < 0.2D);
        assertEquals(0.03D, velocity.y, 1.0E-9D);
    }

    @Test
    void calculateFollowerVelocity_engagesContinuouslyAtTargetDistance() {
        Vec3 below = SpringPhysicsUtil.calculateFollowerVelocity(
                Vec3.ZERO, new Vec3(0.1, 0, 0),
                new Vec3(-1.2, 0, 0), Vec3.ZERO,
                1.2D);
        Vec3 justAbove = SpringPhysicsUtil.calculateFollowerVelocity(
                Vec3.ZERO, new Vec3(0.1, 0, 0),
                new Vec3(-1.2001, 0, 0), Vec3.ZERO,
                1.2D);

        assertEquals(0.0D, below.x, 1.0E-9D);
        assertTrue(justAbove.x > 0.0D && justAbove.x < 0.001D,
                "Crossing the target distance must not introduce a velocity jump");
    }

    @Test
    void calculateFollowerVelocity_closesGapBehindFastLeader() {
        Vec3 velocity = SpringPhysicsUtil.calculateFollowerVelocity(
                Vec3.ZERO, new Vec3(0.4, 0, 0),
                new Vec3(-2.0, 0, 0), Vec3.ZERO,
                1.2D);

        assertTrue(velocity.x > 0.4D,
                "A stretched follower must move faster than its leader to close the gap");
    }

    @Test
    void calculateFollowerVelocity_settlesWithoutStopPullOscillation() {
        Vec3 leaderPosition = Vec3.ZERO;
        Vec3 followerPosition = new Vec3(-1.20D, 0.0D, 0.0D);
        Vec3 leaderVelocity = new Vec3(0.10D, 0.0D, 0.0D);
        Vec3 followerVelocity = Vec3.ZERO;
        double minimumDistance = Double.POSITIVE_INFINITY;
        double maximumDistance = Double.NEGATIVE_INFINITY;

        for (int tick = 0; tick < 200; tick++) {
            leaderPosition = leaderPosition.add(leaderVelocity);
            followerPosition = followerPosition.add(followerVelocity);
            followerVelocity = followerVelocity.multiply(0.80D, 1.0D, 0.80D);
            followerVelocity = SpringPhysicsUtil.calculateFollowerVelocity(
                    leaderPosition, leaderVelocity,
                    followerPosition, followerVelocity,
                    1.20D);

            if (tick >= 150) {
                double distance = leaderPosition.subtract(followerPosition).horizontalDistance();
                minimumDistance = Math.min(minimumDistance, distance);
                maximumDistance = Math.max(maximumDistance, distance);
            }
        }

        assertEquals(0.10D, followerVelocity.x, 1.0E-6D);
        assertTrue(minimumDistance > 1.20D);
        assertTrue(maximumDistance < 1.30D);
        assertTrue(maximumDistance - minimumDistance < 1.0E-6D);
    }

    @Test
    void calculateRouteFollowerVelocity_convergesWithoutPositionSnaps() {
        Vec3 target = new Vec3(0, 0, 0);
        Vec3 follower = new Vec3(-0.6, 0, 0);
        Vec3 velocity = Vec3.ZERO;
        Vec3 forward = new Vec3(1, 0, 0);
        double speed = 0.04D;

        for (int tick = 0; tick < 200; tick++) {
            target = target.add(speed, 0, 0);
            follower = follower.add(velocity);
            velocity = velocity.multiply(0.8D, 1.0D, 0.8D);
            velocity = SpringPhysicsUtil.calculateRouteFollowerVelocity(
                    follower, velocity, target, forward, speed);
        }

        assertEquals(speed, velocity.x, 1.0E-6D);
        assertTrue(target.x - follower.x > 0.0D && target.x - follower.x < 0.02D,
                "Follower should settle just behind the moving target without teleporting");
        assertEquals(0.0D, velocity.z, 1.0E-9D);
    }

    @Test
    void calculateRouteFollowerVelocity_preservesVerticalMotionAndCapsCorrection() {
        Vec3 velocity = SpringPhysicsUtil.calculateRouteFollowerVelocity(
                Vec3.ZERO, new Vec3(0, 0.08, 0),
                new Vec3(4, 3, 0), new Vec3(1, 0, 0),
                0.04D);

        assertEquals(0.08D, velocity.y, 1.0E-9D);
        assertTrue(velocity.x <= 0.16D,
                "Large route errors must not launch a follower at an unbounded correction speed");
    }
}
