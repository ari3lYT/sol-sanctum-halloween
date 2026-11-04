package ru.d2.elysium;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import ru.d2.elysium.core.HudLayout;

public final class HudPositionScreen extends Screen {
    private final ElysiumClient mod;
    private final Screen parent;
    private boolean dragging;
    private double offsetX, offsetY;
    public HudPositionScreen(ElysiumClient mod, Screen parent) { super(Component.literal("Положение HUD")); this.mod = mod; this.parent = parent; }
    @Override protected void init() {
        int x = width / 2 - 154;
        addRenderableWidget(Button.builder(Component.literal("По умолчанию"), b -> {
            mod.store.config.hudX = 0; mod.store.config.hudY = 0; mod.settingsChanged();
        }).bounds(x, height - 28, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose()).bounds(x + 158, height - 28, 150, 20).build());
    }
    private HudLayout.Bounds bounds() {
        int panelWidth = Math.max(180, Minecraft.getInstance().font.width("Очки: " + mod.counter.points(mod.store.config)
            + "  Мобы: " + mod.counter.totalKills()) + 20);
        panelWidth = Math.max(panelWidth, Minecraft.getInstance().font.width("Уникальные: " + mod.counter.uniqueCandies()) + 44);
        return HudLayout.bounds(mod.store.config, width, height, panelWidth, Overlay.hudHeight(mod));
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        var b = bounds();
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && b.contains(event.x(), event.y())) {
            dragging = true; offsetX = event.x() - b.x(); offsetY = event.y() - b.y(); return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) return super.mouseDragged(event, dx, dy);
        var b = bounds();
        HudLayout.move(mod.store.config, width, height, b.width(), b.height(), event.x() - offsetX, event.y() - offsetY);
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) { dragging = false; mod.settingsChanged(); return true; }
        return super.mouseReleased(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Canvas g = Platform.canvas(graphics); g.fill(0, 0, width, height, 0x550B0910);
        Overlay.drawHud(mod, g);
        var b = bounds();
        g.fill(b.x() - 1, b.y() - 1, b.x() + b.width() + 1, b.y(), Overlay.PURPLE);
        g.fill(b.x() - 1, b.y() + b.height(), b.x() + b.width() + 1, b.y() + b.height() + 1, Overlay.PURPLE);
        g.fill(b.x() - 1, b.y(), b.x(), b.y() + b.height(), Overlay.PURPLE);
        g.fill(b.x() + b.width(), b.y(), b.x() + b.width() + 1, b.y() + b.height(), Overlay.PURPLE);
        String hint = "Перетащи панель мышью";
        g.text(hint, (width - g.textWidth(hint)) / 2, height - 44, Overlay.WHITE);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
    @Override public void onClose() { dragging = false; mod.settingsChanged(); Platform.show(Minecraft.getInstance(), parent); }
    @Override public boolean isPauseScreen() { return false; }
}
