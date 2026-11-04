package ru.d2.elysium.core;

public final class HudLayout {

    public record Bounds(int x, int y, int width, int height) {
        public boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
    public static Bounds bounds(Config config, int width, int height, int panelWidth, int panelHeight) {
        int w = Math.max(1, Math.min(panelWidth, width - 16));
        int h = Math.max(1, Math.min(panelHeight, height - 16));
        int availableX = Math.max(0, width - w - 16), availableY = Math.max(0, height - h - 16);
        return new Bounds(8 + (int) Math.round(config.hudX * availableX), 8 + (int) Math.round(config.hudY * availableY), w, h);
    }
    public static void move(Config config, int width, int height, int panelWidth, int panelHeight, double x, double y) {
        int availableX = Math.max(0, width - Math.min(panelWidth, width - 16) - 16);
        int availableY = Math.max(0, height - Math.min(panelHeight, height - 16) - 16);
        config.hudX = availableX == 0 ? 0 : Math.clamp((x - 8) / availableX, 0.0, 1.0);
        config.hudY = availableY == 0 ? 0 : Math.clamp((y - 8) / availableY, 0.0, 1.0);
    }
}
