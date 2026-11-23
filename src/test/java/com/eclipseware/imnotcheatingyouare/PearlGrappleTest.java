package com.eclipseware.imnotcheatingyouare;

import com.eclipseware.imnotcheatingyouare.client.module.impl.PearlGrapplePhysics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PearlGrappleTest {

    @Test
    void ballisticSolverHitsStationaryAndElevatedTargetsPrecisely() {
        Vec3 spawn = new Vec3(0.0, 65.52, 0.0);
        Vec3 zeroVel = Vec3.ZERO;
        Vec3[] targets = new Vec3[]{
            new Vec3(12.0, 64.0, 0.0),
            new Vec3(-18.5, 68.0, 21.0),
            new Vec3(34.0, 61.0, -26.0),
            new Vec3(0.0, 70.5, -29.5)
        };

        for (Vec3 target : targets) {
            PearlGrapplePhysics.ThrowSolution sol = PearlGrapplePhysics.solveThrowFromState(
                null,
                null,
                spawn,
                zeroVel,
                target
            );
            Assertions.assertNotNull(sol);
            Assertions.assertTrue(sol.pitch() >= -90.0f && sol.pitch() <= 90.0f);
            Assertions.assertTrue(sol.flightTicks() > 0.0);

            Vec3 impact = PearlGrapplePhysics.simulateAirPointAtTicks(
                spawn,
                zeroVel,
                sol.yaw(),
                sol.pitch(),
                sol.flightTicks()
            );
            Assertions.assertEquals(0.0, impact.distanceTo(target), 0.01);
        }
    }

    @Test
    void ballisticSolverCompensatesForMovingPlayerVelocity() {
        Vec3 spawn = new Vec3(10.0, 71.52, -5.0);
        Vec3 sprintStrafeVel = new Vec3(0.26, 0.18, -0.19);
        Vec3 target = new Vec3(36.0, 69.0, 14.0);

        PearlGrapplePhysics.ThrowSolution sol = PearlGrapplePhysics.solveThrowFromState(
            null,
            null,
            spawn,
            sprintStrafeVel,
            target
        );
        Assertions.assertNotNull(sol);

        Vec3 impact = PearlGrapplePhysics.simulateAirPointAtTicks(
            spawn,
            sprintStrafeVel,
            sol.yaw(),
            sol.pitch(),
            sol.flightTicks()
        );
        Assertions.assertEquals(0.0, impact.distanceTo(target), 0.01);

        float naiveYaw = (float) Math.toDegrees(Math.atan2(-(target.x - spawn.x), target.z - spawn.z));
        Assertions.assertTrue(Math.abs(Mth.wrapDegrees(sol.yaw() - naiveYaw)) > 1.0f);
    }

    @Test
    void discreteTickSimulationMatchesClosedFormBasePosition() {
        Vec3 spawn = new Vec3(-4.0, 80.0, 12.0);
        Vec3 inheritedVel = new Vec3(0.22, 0.0, -0.14);
        float yaw = -37.5f;
        float pitch = -24.0f;
        Vec3 dir = PearlGrapplePhysics.directionFromRotation(yaw, pitch);

        double px = spawn.x;
        double py = spawn.y;
        double pz = spawn.z;
        double vx = dir.x * PearlGrapplePhysics.LAUNCH_SPEED + inheritedVel.x;
        double vy = dir.y * PearlGrapplePhysics.LAUNCH_SPEED + inheritedVel.y;
        double vz = dir.z * PearlGrapplePhysics.LAUNCH_SPEED + inheritedVel.z;

        for (int tick = 1; tick <= 45; tick++) {
            vy -= PearlGrapplePhysics.GRAVITY;
            vx *= PearlGrapplePhysics.AIR_DRAG;
            vy *= PearlGrapplePhysics.AIR_DRAG;
            vz *= PearlGrapplePhysics.AIR_DRAG;
            px += vx;
            py += vy;
            pz += vz;

            Vec3 closedForm = PearlGrapplePhysics.simulateAirPointAtTicks(spawn, inheritedVel, yaw, pitch, tick);
            Assertions.assertEquals(px, closedForm.x, 1.0e-6);
            Assertions.assertEquals(py, closedForm.y, 1.0e-6);
            Assertions.assertEquals(pz, closedForm.z, 1.0e-6);
        }
    }

    @Test
    void unreachableTargetAndEligibilityFiltersBehaveLikeAutoPearl() {
        Vec3 spawn = new Vec3(0.0, 64.0, 0.0);
        PearlGrapplePhysics.ThrowSolution impossible = PearlGrapplePhysics.solveThrowFromState(
            null,
            null,
            spawn,
            Vec3.ZERO,
            new Vec3(350.0, 120.0, 0.0)
        );
        Assertions.assertNull(impossible);

        Vec3 localPos = new Vec3(0.0, 64.0, 0.0);
        Vec3 ownerPos = new Vec3(0.0, 64.0, 10.0);
        Vec3 escapingLanding = new Vec3(0.0, 64.0, 28.0);
        Vec3 approachingLanding = new Vec3(0.0, 64.0, 4.0);
        Assertions.assertTrue(PearlGrapplePhysics.isPearlEscaping(localPos, ownerPos, escapingLanding));
        Assertions.assertFalse(PearlGrapplePhysics.isPearlEscaping(localPos, ownerPos, approachingLanding));

        Assertions.assertTrue(PearlGrapplePhysics.isWithinAngleLimit(localPos, 0.0f, escapingLanding, 90.0));
        Assertions.assertFalse(PearlGrapplePhysics.isWithinAngleLimit(localPos, 180.0f, escapingLanding, 90.0));
        Assertions.assertTrue(PearlGrapplePhysics.isWithinAngleLimit(localPos, 180.0f, escapingLanding, 360.0));

        Assertions.assertTrue(PearlGrapplePhysics.passesVerticalCheck(64.0, new Vec3(0.0, 70.9, 20.0), true));
        Assertions.assertFalse(PearlGrapplePhysics.passesVerticalCheck(64.0, new Vec3(0.0, 71.5, 20.0), true));
        Assertions.assertTrue(PearlGrapplePhysics.passesVerticalCheck(64.0, new Vec3(0.0, 95.0, 20.0), false));
    }

    @Test
    void gcdRotationStepperConvergesWithoutOvershoot() {
        float gcd = 0.15f;
        float currentYaw = 170.0f;
        float targetYaw = -165.0f;
        for (int i = 0; i < 12; i++) {
            currentYaw = PearlGrapplePhysics.stepAngleGcd(currentYaw, targetYaw, 10.0f, gcd, true);
        }
        Assertions.assertTrue(Math.abs(Mth.wrapDegrees(targetYaw - currentYaw)) <= 0.36f);

        float currentPitch = 15.0f;
        float targetPitch = -42.0f;
        for (int i = 0; i < 12; i++) {
            currentPitch = PearlGrapplePhysics.stepAngleGcd(currentPitch, targetPitch, 10.0f, gcd, false);
        }
        Assertions.assertTrue(Math.abs(targetPitch - currentPitch) <= 0.36f);
    }

    @Test
    void bezierSpeedCurveAndCurvedWaypointsBehaveHumanlyAndConvergeExact() {
        double[] sCurve = new double[]{0.25, 0.10, 0.25, 1.00};
        Assertions.assertEquals(0.0, PearlGrapplePhysics.sampleCubicBezier(sCurve, 0.0), 1.0e-6);
        Assertions.assertEquals(1.0, PearlGrapplePhysics.sampleCubicBezier(sCurve, 1.0), 1.0e-6);
        double early = PearlGrapplePhysics.sampleCubicBezier(sCurve, 0.25);
        double mid = PearlGrapplePhysics.sampleCubicBezier(sCurve, 0.55);
        double late = PearlGrapplePhysics.sampleCubicBezier(sCurve, 0.85);
        Assertions.assertTrue(early < mid && mid < late);

        float startSpeed = PearlGrapplePhysics.computeHumanizedTurnSpeed(
            40.0, 50.0, 45.0, sCurve, 0.05, 0.40, 1.15, 0.50, 1.0
        );
        float peakSpeed = PearlGrapplePhysics.computeHumanizedTurnSpeed(
            40.0, 35.0, 45.0, sCurve, 0.50, 0.40, 1.15, 0.50, 1.0
        );
        float settleSpeed = PearlGrapplePhysics.computeHumanizedTurnSpeed(
            40.0, 8.0, 45.0, sCurve, 0.92, 0.40, 1.15, 0.50, 1.0
        );
        Assertions.assertTrue(peakSpeed > startSpeed);
        Assertions.assertTrue(peakSpeed > settleSpeed);

        float[] midArc = PearlGrapplePhysics.computeCurvedWaypoint(0.0f, 0.0f, 60.0f, -20.0f, 0.45, 7.5f, 1.0f);
        Assertions.assertTrue(Math.abs(midArc[1] - (-20.0f)) > 0.5f || Math.abs(Mth.wrapDegrees(midArc[0] - 60.0f)) > 0.5f);

        float[] finalLock = PearlGrapplePhysics.computeCurvedWaypoint(58.5f, -19.2f, 60.0f, -20.0f, 0.96, 7.5f, 1.0f);
        Assertions.assertEquals(60.0f, finalLock[0], 0.001f);
        Assertions.assertEquals(-20.0f, finalLock[1], 0.001f);
    }
}
