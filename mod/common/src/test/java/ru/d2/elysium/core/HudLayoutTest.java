package ru.d2.elysium.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudLayoutTest {
    @Test void startsWithMargin() {
        var b = HudLayout.bounds(new Config(), 427, 240, 180, 77); assertEquals(8, b.x()); assertEquals(8, b.y());
    }
    @Test void dragClampsToScreenEdges() {
        var c = new Config(); HudLayout.move(c, 427, 240, 180, 77, -500, 9999);
        var b = HudLayout.bounds(c, 427, 240, 180, 77); assertEquals(8, b.x()); assertEquals(232, b.y() + b.height());
    }
    @Test void positionSurvivesWindowResize() {
        var c = new Config(); HudLayout.move(c, 427, 240, 180, 77, 9999, 9999);
        var b = HudLayout.bounds(c, 854, 480, 180, 77); assertEquals(846, b.x() + b.width()); assertEquals(472, b.y() + b.height());
    }
    @Test void tinyViewportCannotProduceInvalidCoordinates() {
        var c = new Config(); HudLayout.move(c, 80, 60, 180, 77, 500, 500);
        var b = HudLayout.bounds(c, 80, 60, 180, 77); assertTrue(b.x() >= 0 && b.y() >= 0); assertTrue(b.x() + b.width() <= 80);
    }
    @Test void corruptPositionIsSanitized() {
        var c = new Config(); c.hudX = Double.NaN; c.hudY = Double.POSITIVE_INFINITY; c.validate(); assertEquals(0, c.hudX); assertEquals(0, c.hudY);
    }
}
