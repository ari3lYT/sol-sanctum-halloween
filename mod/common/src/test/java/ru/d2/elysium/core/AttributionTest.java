package ru.d2.elysium.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttributionTest {
    private static final String Z = "minecraft:zombie";
    @Test void localServerReportedDamageThenDeathCreatesCandidate() {
        var a = new Attribution(); a.damage("a", Z, 1, 42, 42, 100);
        var kill = a.death("a", Z, 1, 101, 1000); assertNotNull(kill); assertFalse(kill.glowing());
    }
    @Test void glowingHeadCreatesUniqueCandidate() {
        var a = new Attribution(); a.damage("a", Z, 2, 42, 42, 100);
        assertTrue(a.death("a", Z, 2, 101, 1000).glowing());
    }
    @Test void someoneElseFinalHitInvalidatesCandidate() {
        var a = new Attribution(); a.damage("a", Z, 2, 42, 42, 100); a.damage("a", Z, 2, 99, 42, 101);
        assertNull(a.death("a", Z, 2, 102, 1000));
    }
    @Test void seeingDeathOrSwingingWithoutServerDamageNeverCounts() {
        assertNull(new Attribution().death("a", Z, 2, 102, 1000));
    }
    @Test void noPumpkinNoCandy() {
        var a = new Attribution(); a.damage("a", Z, 0, 42, 42, 100);
        assertNull(a.death("a", Z, 0, 101, 1000));
    }
    @Test void damageSnapshotSurvivesEquipmentClearingOnDeath() {
        var a = new Attribution(); a.damage("a", Z, 2, 42, 42, 100);
        assertTrue(a.death("a", Z, 0, 101, 1000).glowing());
    }
    @Test void duplicateDeathPacketCannotProduceSecondCandidate() {
        var a = new Attribution(); a.damage("a", Z, 2, 42, 42, 100);
        assertNotNull(a.death("a", Z, 2, 101, 1000));
        a.damage("a", Z, 2, 42, 42, 102); assertNull(a.death("a", Z, 2, 103, 1100));
    }
    @Test void environmentalFinishNeedsRecentPlayerHit() {
        var a = new Attribution(); a.damage("a", Z, 1, 42, 42, 100); a.damage("a", Z, 1, -1, 42, 150);
        assertNotNull(a.death("a", Z, 1, 151, 2000));
    }
    @Test void oldDamageOrRespawnCannotAttributeDeath() {
        var a = new Attribution(); a.damage("a", Z, 1, 42, 42, 100);
        assertNull(a.death("a", Z, 1, 201, 2000));
        a.damage("b", Z, 1, 42, 42, 300); assertNull(a.death("b", Z, 1, 5, 2000));
    }
    @Test void piglinCannotBecomeCandidateEvenWithGlowingPumpkin() {
        var a = new Attribution(); a.damage("a", "minecraft:zombified_piglin", 2, 42, 42, 100);
        assertNull(a.death("a", "minecraft:zombified_piglin", 2, 101, 2000));
    }
}
