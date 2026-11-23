package com.eclipseware.imnotcheatingyouare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.clickgui.GuiTaxonomy;
import com.eclipseware.imnotcheatingyouare.client.module.ModuleManager;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Hitboxes;
import com.eclipseware.imnotcheatingyouare.client.module.impl.KillSounds;
import com.eclipseware.imnotcheatingyouare.client.module.impl.MaceDTap;
import com.eclipseware.imnotcheatingyouare.client.module.impl.Menu;
import com.eclipseware.imnotcheatingyouare.client.module.impl.PearlCatchPlanner;
import com.eclipseware.imnotcheatingyouare.client.module.impl.SpearMaceDTap;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CombatAndUtilityAdditionsTest {

    @Test
    void diagonalPearlCatchFindsVerifiedInterceptAcrossAngles() {
        Vec3 origin = new Vec3(10.0, 64.0, -20.0);
        double eyeHeight = 1.62;

        PearlCatchPlanner.InterceptPlan diagUp = PearlCatchPlanner.findDiagonalPlan(
            null,
            null,
            origin,
            eyeHeight,
            Vec3.ZERO,
            true,
            42.0f,
            -32.0f,
            2
        );
        assertNotNull(diagUp);
        assertEquals(42.0f, diagUp.chargeYaw(), 0.01f);
        assertEquals(-32.0f, diagUp.chargePitch(), 0.01f);
        assertTrue(diagUp.chargeTick() >= 2);
        assertTrue(diagUp.missDistanceSq() <= PearlCatchPlanner.MAX_INTERCEPT_DISTANCE_SQ);

        Vec3 sprintVel = new Vec3(0.21, 0.0, -0.14);
        PearlCatchPlanner.InterceptPlan movingDiag = PearlCatchPlanner.findDiagonalPlan(
            null,
            null,
            origin,
            eyeHeight,
            sprintVel,
            true,
            -55.0f,
            -18.0f,
            3
        );
        assertNotNull(movingDiag);
        assertTrue(movingDiag.chargeTick() >= 3);
        assertTrue(movingDiag.missDistanceSq() <= PearlCatchPlanner.MAX_INTERCEPT_DISTANCE_SQ);

        PearlCatchPlanner.InterceptPlan flatLook = PearlCatchPlanner.findDiagonalPlan(
            null,
            null,
            origin,
            eyeHeight,
            Vec3.ZERO,
            true,
            90.0f,
            0.0f,
            2
        );
        assertNotNull(flatLook);
        assertTrue(flatLook.missDistanceSq() <= PearlCatchPlanner.MAX_INTERCEPT_DISTANCE_SQ);
    }

    @Test
    void upwardPearlCatchSolvesStationaryAndMovingThrows() {
        Vec3 origin = new Vec3(0.0, 70.0, 0.0);
        PearlCatchPlanner.InterceptPlan stationary = PearlCatchPlanner.findUpwardPlan(
            origin,
            1.62,
            Vec3.ZERO,
            true,
            15.0f,
            2
        );
        assertNotNull(stationary);
        assertTrue(stationary.pearlPitch() <= -76.0f);
        assertTrue(stationary.chargePitch() <= -86.0f);
        assertTrue(stationary.missDistanceSq() <= PearlCatchPlanner.MAX_INTERCEPT_DISTANCE_SQ);

        PearlCatchPlanner.InterceptPlan moving = PearlCatchPlanner.findUpwardPlan(
            origin,
            1.62,
            new Vec3(0.18, 0.0, 0.12),
            true,
            -120.0f,
            1
        );
        assertNotNull(moving);
        assertTrue(moving.missDistanceSq() <= PearlCatchPlanner.MAX_INTERCEPT_DISTANCE_SQ);
    }

    @Test
    void hitboxInteractClampingScalesOutsideCoordsInsideVanillaBox() {
        Vec3 entityBase = new Vec3(5.0, 64.0, 5.0);
        AABB playerBox = new AABB(4.7, 64.0, 4.7, 5.3, 65.8, 5.3);
        float extra = 0.30f;

        Vec3 inside = new Vec3(0.15, 0.90, -0.10);
        Vec3 untouched = Hitboxes.clampBoxHitVector(playerBox, entityBase, extra, inside);
        assertEquals(inside.x, untouched.x, 1.0e-6);
        assertEquals(inside.y, untouched.y, 1.0e-6);
        assertEquals(inside.z, untouched.z, 1.0e-6);

        Vec3 expandedEdge = new Vec3(0.55, 2.05, -0.58);
        Vec3 clamped = Hitboxes.clampBoxHitVector(playerBox, entityBase, extra, expandedEdge);
        assertTrue(Math.abs(clamped.x) <= 0.300001);
        assertTrue(Math.abs(clamped.z) <= 0.300001);
        assertTrue(clamped.y >= 0.0 && clamped.y <= 1.800001);
    }

    @Test
    void customMenuDefaultsOnAndTogglesCleanly() {
        ImnotcheatingyouareClient prev = ImnotcheatingyouareClient.INSTANCE;
        try {
            ImnotcheatingyouareClient stub = new ImnotcheatingyouareClient();
            stub.moduleManager = new ModuleManager();
            stub.settingsManager = new SettingsManager();
            ImnotcheatingyouareClient.INSTANCE = stub;

            Menu customMenu = new Menu();
            ModuleManager mm = stub.moduleManager;
            mm.modules.add(customMenu);

            assertEquals("CustomMenu", customMenu.getName());
            assertTrue(customMenu.isToggled());
            assertTrue(Menu.isCustomMenuEnabled());

            customMenu.setToggled(false);
            assertFalse(Menu.isCustomMenuEnabled());
            assertFalse(Menu.shouldReplacePanorama());

            customMenu.setToggled(true);
            assertTrue(Menu.isCustomMenuEnabled());
        } finally {
            ImnotcheatingyouareClient.INSTANCE = prev;
        }
    }

    @Test
    void guiTaxonomyRoutesNewModulesToExpectedSections() {
        ImnotcheatingyouareClient prev = ImnotcheatingyouareClient.INSTANCE;
        try {
            ImnotcheatingyouareClient stub = new ImnotcheatingyouareClient();
            stub.moduleManager = new ModuleManager();
            stub.settingsManager = new SettingsManager();
            ImnotcheatingyouareClient.INSTANCE = stub;

            Hitboxes hitboxes = new Hitboxes();
            SpearMaceDTap spearMace = new SpearMaceDTap();
            MaceDTap maceDTap = new MaceDTap();
            KillSounds killSounds = new KillSounds();
            Menu customMenu = new Menu();

            assertEquals(GuiTaxonomy.Group.COMBAT, GuiTaxonomy.resolveGroup(hitboxes));
            assertEquals("Blatant", GuiTaxonomy.resolveSubCategory(hitboxes));

            assertEquals(GuiTaxonomy.Group.COMBAT, GuiTaxonomy.resolveGroup(spearMace));
            assertEquals("Mace", GuiTaxonomy.resolveSubCategory(spearMace));

            assertEquals(GuiTaxonomy.Group.COMBAT, GuiTaxonomy.resolveGroup(maceDTap));
            assertEquals("Mace", GuiTaxonomy.resolveSubCategory(maceDTap));

            assertEquals(GuiTaxonomy.Group.UTILITY, GuiTaxonomy.resolveGroup(killSounds));
            assertEquals("Player & Totem", GuiTaxonomy.resolveSubCategory(killSounds));

            assertEquals(GuiTaxonomy.Group.CLIENT, GuiTaxonomy.resolveGroup(customMenu));
            assertEquals("Client & GUI", GuiTaxonomy.resolveSubCategory(customMenu));
        } finally {
            ImnotcheatingyouareClient.INSTANCE = prev;
        }
    }
}
