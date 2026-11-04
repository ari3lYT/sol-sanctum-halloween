package ru.d2.elysium.core;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CounterTest {
    private static final String Z = "minecraft:zombie", S = "minecraft:skeleton", P = "minecraft:zombified_piglin";
    private Counter ready() { var c = new Counter(); c.apply(Map.of(Z, 50), true, false, 1000); return c; }
    @Test void startsAfterLifetimeBaseline() {
        var c = ready(); assertEquals(0, c.totalKills()); assertEquals(0, c.ordinaryCandies());
        c.apply(Map.of(Z, 51), false, false, 2000); assertEquals(1, c.totalKills());
    }
    @Test void killsWithoutPumpkinsEarnNoCandy() {
        var c = ready(); c.apply(Map.of(Z, 59), false, false, 2000);
        assertEquals(9, c.totalKills()); assertEquals(0, c.points(new Config()));
    }
    @Test void bothCandiesRequireObservedDeathAndStatConfirmation() {
        var c = ready(); c.observe("one", Z, false, 1100); c.observe("two", Z, true, 1200);
        assertEquals(0, c.points(new Config()));
        c.apply(Map.of(Z, 52), false, false, 2000);
        assertEquals(1, c.ordinaryCandies()); assertEquals(1, c.uniqueCandies()); assertEquals(4, c.points(new Config()));
    }
    @Test void duplicatePacketsCannotDuplicateCandy() {
        var c = ready(); c.observe("one", Z, true, 1100);
        c.apply(Map.of(Z, 51), false, false, 2000); c.apply(Map.of(Z, 51), false, false, 2100);
        assertEquals(1, c.uniqueCandies()); assertEquals(1, c.totalKills());
    }
    @Test void duplicatePendingDeathsAreIgnored() {
        var c = ready(); assertTrue(c.observe("one", Z, false, 1100)); assertFalse(c.observe("one", Z, false, 1101));
    }
    @Test void piglinAndPlayerNeverCount() {
        var c = ready(); assertFalse(c.observe("p", P, true, 1100));
        c.apply(Map.of(P, 100, "minecraft:player", 30), false, false, 2000);
        assertEquals(0, c.totalKills()); assertEquals(0, c.uniqueCandies());
    }
    @Test void partialPacketDoesNotResetUnmentionedBaseline() {
        var c = ready(); c.apply(Map.of(S, 1), false, false, 2000); c.apply(Map.of(Z, 51), false, false, 2100);
        assertEquals(2, c.totalKills()); assertEquals(50, c.baseline.get(Z) - 1);
    }
    @Test void creditsCannotCrossMobTypes() {
        var c = ready(); c.observe("z", Z, true, 1100); c.apply(Map.of(S, 1), false, false, 2000);
        assertEquals(0, c.uniqueCandies());
    }
    @Test void unconfirmedDeathDoesNotEarnCandy() {
        var c = ready(); c.observe("one", Z, true, 1100); c.apply(Map.of(Z, 50), false, false, 2000);
        assertEquals(0, c.uniqueCandies());
    }
    @Test void oneStatIncrementCannotConfirmMultipleDeaths() {
        var c = ready(); c.observe("one", Z, false, 1100); c.observe("two", Z, true, 1200);
        c.apply(Map.of(Z, 51), false, false, 2000); assertEquals(1, c.ordinaryCandies() + c.uniqueCandies());
    }
    @Test void expiredCandidateDoesNotUseUnrelatedFutureKill() {
        var c = ready(); c.observe("one", Z, true, 1100); c.apply(Map.of(Z, 51), false, false, 200000);
        assertEquals(0, c.uniqueCandies());
    }
    @Test void reconnectCatchesUpAndDoesNotResetTotals() {
        var c = ready(); c.observe("one", Z, true, 1100); c.apply(Map.of(Z, 51), true, false, 2000);
        c.apply(Map.of(Z, 51), true, false, 3000); assertEquals(1, c.uniqueCandies()); assertEquals(1, c.totalKills());
    }
    @Test void serverRollbackRebasesWithoutNegativeOrRepeatedLifetimeTotals() {
        var c = ready(); c.apply(Map.of(Z, 51), false, false, 2000);
        c.observe("one", Z, true, 2100); c.apply(Map.of(Z, 0), true, false, 3000);
        c.apply(Map.of(Z, 1), false, false, 4000);
        assertEquals(2, c.totalKills()); assertEquals(0, c.uniqueCandies());
    }
    @Test void resetWaitsForReplyAndPreservesAbsoluteBaseline() {
        var c = ready(); c.observe("one", Z, true, 1100); c.apply(Map.of(Z, 51), false, true, 2000);
        assertEquals(0, c.totalKills()); assertEquals(0, c.uniqueCandies());
        c.observe("two", Z, false, 2100); c.apply(Map.of(Z, 52), false, false, 3000);
        assertEquals(1, c.ordinaryCandies()); assertEquals(1, c.totalKills());
    }
    @Test void negativeServerValuesCannotCreateRewards() {
        var c = ready(); c.apply(Map.of(Z, -1), false, false, 2000); assertEquals(50, c.baseline.get(Z));
    }
    @Test void pointsUseLongArithmetic() {
        var c = ready(); c.ordinary = 3_000_000_000L; c.unique = 2_000_000_000L;
        assertEquals(9_000_000_000L, c.points(new Config()));
    }
}
