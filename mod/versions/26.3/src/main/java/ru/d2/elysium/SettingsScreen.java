package ru.d2.elysium;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class SettingsScreen extends Screen {
    private final ElysiumClient mod;
    private final Screen parent;
    private Boolean capture;
    private Button openKey, hudKey;
    public SettingsScreen(ElysiumClient mod, Screen parent) { super(Component.literal("Настройки")); this.mod = mod; this.parent = parent; }
    private String label(boolean statistics) { return (statistics ? "Статистика: " : "Скрыть HUD: ") + Platform.binding(statistics).getTranslatedKeyMessage().getString(); }
    private void refreshKeys() {
        openKey.setMessage(Component.literal(Boolean.TRUE.equals(capture) ? "Нажми клавишу…" : label(true))
            .withStyle(Platform.conflict(true) ? ChatFormatting.RED : ChatFormatting.WHITE));
        hudKey.setMessage(Component.literal(Boolean.FALSE.equals(capture) ? "Нажми клавишу…" : label(false))
            .withStyle(Platform.conflict(false) ? ChatFormatting.RED : ChatFormatting.WHITE));
    }
    @Override protected void init() {
        int w = Math.min(width - 16, 320), x = (width - w) / 2, y = (height - 220) / 2, half = (w - 32) / 2;
        addRenderableWidget(Button.builder(Component.literal("HUD: " + (mod.store.config.showHud ? "вкл" : "выкл")), b -> {
            mod.toggleHud(); b.setMessage(Component.literal("HUD: " + (mod.store.config.showHud ? "вкл" : "выкл")));
        }).bounds(x + 12, y + 40, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Tab: " + (mod.store.config.showInTab ? "вкл" : "выкл")), b -> {
            mod.toggleTab(); b.setMessage(Component.literal("Tab: " + (mod.store.config.showInTab ? "вкл" : "выкл")));
        }).bounds(x + w - half - 12, y + 40, half, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Положение HUD"), b ->
            Platform.show(Minecraft.getInstance(), new HudPositionScreen(mod, this))).bounds(x + 12, y + 66, w - 24, 20).build());
        openKey = addRenderableWidget(Button.builder(Component.empty(), b -> { capture = true; refreshKeys(); })
            .bounds(x + 12, y + 111, w - 24, 20).build());
        hudKey = addRenderableWidget(Button.builder(Component.empty(), b -> { capture = false; refreshKeys(); })
            .bounds(x + 12, y + 137, w - 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose()).bounds(x + 12, y + 188, w - 24, 20).build());
        refreshKeys();
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (capture == null) return super.keyPressed(event);
        if (event.key() != InputConstants.KEY_ESCAPE) {
            Platform.bind(capture, event.key() == InputConstants.KEY_DELETE || event.key() == InputConstants.KEY_BACKSPACE
                ? InputConstants.UNKNOWN : InputConstants.getKey(event));
        }
        capture = null; refreshKeys(); return true;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (capture == null) return super.mouseClicked(event, doubleClick);
        Platform.bind(capture, InputConstants.Type.MOUSE.getOrCreate(event.button()));
        capture = null; refreshKeys(); return true;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Canvas g = Platform.canvas(graphics); g.fill(0, 0, width, height, 0xB00B0910);
        int w = Math.min(width - 16, 320), x = (width - w) / 2, y = (height - 220) / 2;
        Overlay.panel(g, x, y, w, 220);
        g.text("Настройки", x + 14, y + 14, Overlay.ORANGE);
        g.text("Клавиши", x + 14, y + 96, Overlay.WHITE);
        if (capture != null) g.fitText("Esc — отмена · Delete — убрать", x + 14, y + 168, w - 28, Overlay.MUTED);
        else if (Platform.conflict(true) || Platform.conflict(false)) g.text("Красным отмечен конфликт клавиш", x + 14, y + 168, Overlay.MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
    @Override public void onClose() { capture = null; mod.settingsChanged(); Platform.show(Minecraft.getInstance(), parent); }
    @Override public boolean isPauseScreen() { return false; }
}
