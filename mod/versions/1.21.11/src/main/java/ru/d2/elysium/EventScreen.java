package ru.d2.elysium;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.d2.elysium.core.Counter;

public final class EventScreen extends Screen {
    private final ElysiumClient mod;
    private long resetArmed;
    private long copiedAt;
    private Button resetButton;
    public EventScreen(ElysiumClient mod) { super(Component.literal("Sol Sanctum | Makoto · Хэллоуин")); this.mod = mod; }
    @Override protected void init() {
        int w = Math.min(width - 16, 400), x = (width - w) / 2, y = (height - 240) / 2;
        int half = (w - 32) / 2;
        addRenderableWidget(Button.builder(Component.literal("Настройки"), b ->
            Platform.show(Minecraft.getInstance(), new SettingsScreen(mod, this))).bounds(x + 12, y + 152, w - 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Копировать итог"), b -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(mod.summary()); copiedAt = System.currentTimeMillis();
        }).bounds(x + 12, y + 176, half, 20).build());
        resetButton = addRenderableWidget(Button.builder(Component.literal("Сбросить"), b -> {
            long now = System.currentTimeMillis();
            if (resetArmed > now) { mod.reset(); resetArmed = 0; b.setMessage(Component.literal("Сбросить")); }
            else { resetArmed = now + 5000; b.setMessage(Component.literal("Подтвердить сброс")); }
        }).bounds(x + w - half - 12, y + 176, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose()).bounds(x + 12, y + 208, w - 24, 20).build());
    }
    @Override public void tick() {
        if (resetArmed != 0 && resetArmed < System.currentTimeMillis()) {
            resetArmed = 0; resetButton.setMessage(Component.literal("Сбросить"));
        }
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Canvas g = Platform.canvas(graphics);
        g.fill(0, 0, width, height, 0xB00B0910);
        int w = Math.min(width - 16, 400), x = (width - w) / 2, y = (height - 240) / 2;
        Overlay.panel(g, x, y, w, 240);
        g.fitText("Sol Sanctum | Makoto · Хэллоуин", x + 14, y + 12, w - 28, Overlay.ORANGE);
        Counter c = mod.counter;
        if (c != null) {
            Overlay.candy(g, x + 14, y + 38, Overlay.ORANGE);
            g.text("Обычные конфеты: " + c.ordinaryCandies() + "  × " + mod.store.config.ordinaryPoints, x + 40, y + 39, Overlay.WHITE);
            Overlay.candy(g, x + 14, y + 57, Overlay.PURPLE);
            g.text("Уникальные конфеты: " + c.uniqueCandies() + "  × " + mod.store.config.uniquePoints, x + 40, y + 58, Overlay.WHITE);
            g.text("Очки: " + c.points(mod.store.config), x + 14, y + 81, Overlay.ORANGE);
            g.text("Убийства мобов: " + c.totalKills(), x + 14, y + 98, Overlay.WHITE);
        }
        String status = System.currentTimeMillis() - copiedAt < 2000 ? "Итог скопирован в буфер обмена" : mod.status();
        g.fitText(status, x + 14, y + 133, w - 28, Overlay.MUTED);
        super.render(graphics, mouseX, mouseY, delta);
    }
    @Override public boolean isPauseScreen() { return false; }
}
