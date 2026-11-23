package com.eclipseware.imnotcheatingyouare;

import com.eclipseware.imnotcheatingyouare.client.module.impl.SprintResetController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SprintResetTest {

    @Test
    void wtapImmediateReleaseEnforcesMinimumTickAndPulsesReSprint() {
        SprintResetController.WTapEngine engine = new SprintResetController.WTapEngine();
        Assertions.assertEquals(SprintResetController.TapPhase.IDLE, engine.phase());

        boolean started = engine.trigger(1_000L, 0L, 85L, false, true);
        Assertions.assertTrue(started);
        Assertions.assertEquals(SprintResetController.TapPhase.ACTIVE, engine.phase());
        Assertions.assertTrue(engine.isNormalReleaseActive());
        Assertions.assertFalse(engine.isSilentActive());
        Assertions.assertTrue(engine.isSuppressingSprint());
        Assertions.assertFalse(engine.trigger(1_010L, 0L, 85L, false, true));

        boolean reSprintTick1 = engine.stepTick(1_200L, true);
        Assertions.assertFalse(reSprintTick1);
        Assertions.assertEquals(SprintResetController.TapPhase.ACTIVE, engine.phase());

        boolean reSprintTick2 = engine.stepTick(1_250L, true);
        Assertions.assertTrue(reSprintTick2);
        Assertions.assertEquals(SprintResetController.TapPhase.IDLE, engine.phase());
        Assertions.assertFalse(engine.isNormalReleaseActive());
        Assertions.assertFalse(engine.isSuppressingSprint());
    }

    @Test
    void wtapWaitPhaseTransitionsToActiveOrAbortsIfForwardReleased() {
        SprintResetController.WTapEngine engine = new SprintResetController.WTapEngine();
        Assertions.assertTrue(engine.trigger(2_000L, 60L, 45L, false, true));
        Assertions.assertEquals(SprintResetController.TapPhase.WAITING, engine.phase());
        Assertions.assertFalse(engine.isSuppressingSprint());

        Assertions.assertFalse(engine.stepTick(2_030L, true));
        Assertions.assertEquals(SprintResetController.TapPhase.WAITING, engine.phase());

        Assertions.assertFalse(engine.stepTick(2_065L, true));
        Assertions.assertEquals(SprintResetController.TapPhase.ACTIVE, engine.phase());
        Assertions.assertTrue(engine.isNormalReleaseActive());

        Assertions.assertFalse(engine.stepTick(2_090L, true));
        Assertions.assertEquals(SprintResetController.TapPhase.ACTIVE, engine.phase());

        Assertions.assertTrue(engine.stepTick(2_120L, true));
        Assertions.assertEquals(SprintResetController.TapPhase.IDLE, engine.phase());

        Assertions.assertTrue(engine.trigger(3_000L, 80L, 50L, false, true));
        Assertions.assertEquals(SprintResetController.TapPhase.WAITING, engine.phase());
        Assertions.assertFalse(engine.stepTick(3_020L, false));
        Assertions.assertEquals(SprintResetController.TapPhase.IDLE, engine.phase());
    }

    @Test
    void wtapSilentModeAndReleasedForwardOnCompletion() {
        SprintResetController.WTapEngine engine = new SprintResetController.WTapEngine();
        Assertions.assertTrue(engine.trigger(5_000L, 0L, 40L, true, true));
        Assertions.assertTrue(engine.isSilentActive());
        Assertions.assertFalse(engine.isNormalReleaseActive());
        Assertions.assertTrue(engine.isSuppressingSprint());

        boolean reSprint = engine.stepTick(5_060L, false);
        Assertions.assertFalse(reSprint);
        Assertions.assertEquals(SprintResetController.TapPhase.IDLE, engine.phase());
    }

    @Test
    void stapDelayedAndActiveBackwardTapWithCooldown() {
        SprintResetController.STapEngine engine = new SprintResetController.STapEngine();
        Assertions.assertEquals(SprintResetController.TapPhase.IDLE, engine.phase());

        boolean started = engine.trigger(1, 2, false, true);
        Assertions.assertTrue(started);
        Assertions.assertEquals(SprintResetController.TapPhase.WAITING, engine.phase());
        Assertions.assertFalse(engine.isBackwardTapActive());

        Assertions.assertFalse(engine.stepTick(true, true, 2));
        Assertions.assertEquals(SprintResetController.TapPhase.ACTIVE, engine.phase());
        Assertions.assertTrue(engine.isBackwardTapActive());
        Assertions.assertTrue(engine.isSuppressingSprint());

        Assertions.assertFalse(engine.stepTick(true, true, 2));
        Assertions.assertEquals(SprintResetController.TapPhase.ACTIVE, engine.phase());

        Assertions.assertTrue(engine.stepTick(true, true, 2));
        Assertions.assertEquals(SprintResetController.TapPhase.IDLE, engine.phase());
        Assertions.assertEquals(2, engine.cooldownTicks());

        Assertions.assertFalse(engine.trigger(0, 2, false, true));
        Assertions.assertFalse(engine.stepTick(true, true, 2));
        Assertions.assertEquals(1, engine.cooldownTicks());
        Assertions.assertFalse(engine.stepTick(true, true, 2));
        Assertions.assertEquals(0, engine.cooldownTicks());

        Assertions.assertTrue(engine.trigger(0, 1, false, true));
        Assertions.assertEquals(SprintResetController.TapPhase.ACTIVE, engine.phase());
    }

    @Test
    void stapSilentModeAndForwardReleaseAbort() {
        SprintResetController.STapEngine engine = new SprintResetController.STapEngine();
        Assertions.assertTrue(engine.trigger(0, 2, true, true));
        Assertions.assertTrue(engine.isSilentActive());
        Assertions.assertFalse(engine.isBackwardTapActive());
        Assertions.assertTrue(engine.isSuppressingSprint());

        Assertions.assertFalse(engine.stepTick(false, true, 3));
        Assertions.assertEquals(SprintResetController.TapPhase.IDLE, engine.phase());
        Assertions.assertEquals(3, engine.cooldownTicks());
    }
}
