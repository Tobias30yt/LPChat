package de.lpchat.orbital;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StrikePlanTest {
    @Test void nukeHasSymmetricConcentricRingsAndPredictablePayload() {
        var points = StrikePattern.NUKE.points(5, 0);
        assertEquals(181, points.size());
        assertEquals(0, points.stream().mapToDouble(StrikePattern.Point::x).sum(), 1e-9);
        assertEquals(0, points.stream().mapToDouble(StrikePattern.Point::z).sum(), 1e-9);
        for (var p : points) assertEquals(p.band() * 6, Math.hypot(p.x(), p.z()), 1e-9);
    }
    @Test void freeFlightReachesIntendedRingAndTargetHeightAtFuse() {
        var plan = StrikePlan.create(StrikePattern.NUKE, 5, StrikePlan.Order.SIMULTANEOUS, 23, 64, -64, 320, 768);
        var points = StrikePattern.NUKE.points(5, 23);
        for (int i = 0; i < plan.size(); i++) {
            var p = plan.get(i);
            double x = p.x(), y = p.y(), z = p.z(), vx = p.vx(), vy = p.vy(), vz = p.vz();
            for (int tick = 0; tick < p.fuse(); tick++) { vy -= .04; x += vx; y += vy; z += vz; vx *= .98; vy *= .98; vz *= .98; }
            assertEquals(points.get(i).x(), x, 1e-8);
            assertEquals(points.get(i).z(), z, 1e-8);
            assertEquals(65, y, 1e-8);
        }
    }
    @Test void explosionOrdersReverseBandTiming() {
        var inward = StrikePlan.create(StrikePattern.NUKE, 3, StrikePlan.Order.INWARD, 0, 64, -64, 320, 768);
        var outward = StrikePlan.create(StrikePattern.NUKE, 3, StrikePlan.Order.OUTWARD, 0, 64, -64, 320, 768);
        assertTrue(inward.getFirst().fuse() > inward.getLast().fuse());
        assertTrue(outward.getFirst().fuse() < outward.getLast().fuse());
    }
    @Test void compressedNukeDropsPreformedRings() {
        var plan = StrikePlan.create(StrikePattern.COMPRESSED_NUKE, 4, StrikePlan.Order.SIMULTANEOUS, 0, 64, -64, 320, 768);
        assertTrue(plan.stream().allMatch(p -> p.vx() == 0 && p.vz() == 0));
        assertEquals(24, Math.hypot(plan.getLast().x(), plan.getLast().z()), 1e-9);
    }
    @Test void stabIsVerticalAndFiresFromTopToBottomWithinHeight() {
        var plan = StrikePlan.create(StrikePattern.STAB, 1, StrikePlan.Order.SIMULTANEOUS, 0, 64, -64, 320, 768);
        assertTrue(plan.size() > 190);
        for (int i = 1; i < plan.size(); i++) assertEquals(1, plan.get(i-1).y() - plan.get(i).y(), 1e-9);
        assertTrue(plan.stream().allMatch(p -> p.x() == 0 && p.z() == 0 && !p.gravity() && p.y() >= -64 && p.y() < 320));
        assertTrue(plan.getFirst().y() > plan.getLast().y());
        assertTrue(plan.stream().allMatch(p -> p.fuse() == 1));
    }
    @Test void everyDefaultPatternIsFiniteWithinExpandedTestBudget() {
        for (StrikePattern pattern : StrikePattern.values()) {
            var plan = assertDoesNotThrow(() -> StrikePlan.create(pattern, pattern.defaultSize(), StrikePlan.Order.SIMULTANEOUS, 90, 64, -64, 320, 20000), pattern.id());
            assertFalse(plan.isEmpty(), pattern.id());
            assertTrue(plan.stream().allMatch(p -> Double.isFinite(p.x()) && Double.isFinite(p.y()) && Double.isFinite(p.z())), pattern.id());
        }
    }
    @Test void invalidSizesCoordinatesAndOversizedPlansAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> StrikePattern.NUKE.points(9, 0));
        assertThrows(IllegalArgumentException.class, () -> StrikePattern.RING.points(0, 0));
        assertThrows(IllegalArgumentException.class, () -> StrikePlan.create(StrikePattern.NUKE, 3, StrikePlan.Order.INWARD, 0, Double.NaN, -64, 320, 768));
        assertThrows(IllegalArgumentException.class, () -> StrikePlan.create(StrikePattern.NUKE, 3, StrikePlan.Order.INWARD, 0, 319, -64, 320, 768));
        assertThrows(IllegalArgumentException.class, () -> StrikePlan.create(StrikePattern.CIRCLE_STAB, 8, StrikePlan.Order.INWARD, 0, 64, -64, 320, 768));
    }
}
