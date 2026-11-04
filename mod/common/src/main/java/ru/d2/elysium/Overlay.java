package ru.d2.elysium;

import net.minecraft.client.Minecraft;
import ru.d2.elysium.core.Counter;
import ru.d2.elysium.core.HudLayout;

public final class Overlay {
    public static final int WHITE = 0xFFF4EEE6, MUTED = 0xFFBFB5C8, ORANGE = 0xFFFFAD54, PURPLE = 0xFFD6ABFF;

    public static void panel(Canvas g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xE8161320);
        g.fill(x, y, x + 2, y + h, ORANGE);
        g.fill(x + 2, y, x + w, y + 1, 0xFF46364E);
    }
    public static void candy(Canvas g, int x, int y, int color) {
        g.fill(x, y + 2, x + 3, y + 8, color);
        g.fill(x + 3, y + 3, x + 5, y + 7, 0xFF6A4652);
        g.fill(x + 5, y, x + 13, y + 10, color);
        g.fill(x + 7, y + 1, x + 9, y + 8, 0x55FFFFFF);
        g.fill(x + 13, y + 3, x + 15, y + 7, 0xFF6A4652);
        g.fill(x + 15, y + 2, x + 18, y + 8, color);
    }
    public static int hudWidth(ElysiumClient mod, Canvas g) {
        if (mod.counter == null) return 180;
        Counter c = mod.counter;
        return Math.max(180, Math.max(g.textWidth("Уникальные: " + c.uniqueCandies()) + 44,
                g.textWidth("Очки: " + c.points(mod.store.config) + "  Мобы: " + c.totalKills()) + 20));
    }
    public static int hudHeight(ElysiumClient mod) { return mod.status().isEmpty() ? 77 : 91; }
    public static HudLayout.Bounds bounds(ElysiumClient mod, Canvas g) {
        return HudLayout.bounds(mod.store.config, g.width(), g.height(), hudWidth(mod, g), hudHeight(mod));
    }
    public static void drawHud(ElysiumClient mod, Canvas g) {
        Counter c = mod.counter;
        if (c == null) return;
        var b = bounds(mod, g); int x = b.x(), y = b.y();
        panel(g, x, y, b.width(), b.height());
        g.text("Хэллоуин", x + 10, y + 9, ORANGE);
        candy(g, x + 10, y + 26, ORANGE);
        g.fitText("Обычные: " + c.ordinaryCandies(), x + 34, y + 27, b.width() - 44, WHITE);
        candy(g, x + 10, y + 42, PURPLE);
        g.fitText("Уникальные: " + c.uniqueCandies(), x + 34, y + 43, b.width() - 44, WHITE);
        g.fitText("Очки: " + c.points(mod.store.config) + "  Мобы: " + c.totalKills(), x + 10, y + 62, b.width() - 20, WHITE);
        if (!mod.status().isEmpty()) g.fitText(mod.status(), x + 10, y + 78, b.width() - 20, MUTED);
    }
    public static void render(ElysiumClient mod, Canvas g) {
        Minecraft mc = Minecraft.getInstance();
        Counter c = mod.counter;
        if (c == null || mc.level == null || Platform.hudHidden(mc) || Platform.screen(mc) != null) return;
        if (mod.store.config.showHud) drawHud(mod, g);
        if (mod.store.config.showInTab && mc.options.keyPlayerList.isDown()) {
            int w = Math.min(300, g.width() - 16), x = 8, y = Math.max(8, g.height() - 49);
            panel(g, x, y, w, 40);
            g.text("Очки: " + c.points(mod.store.config), x + 10, y + 7, ORANGE);
            g.fitText("Обычные: " + c.ordinaryCandies() + " · Уникальные: " + c.uniqueCandies()
                    + " · Мобы: " + c.totalKills(), x + 10, y + 23, w - 20, WHITE);
        }
    }
}
