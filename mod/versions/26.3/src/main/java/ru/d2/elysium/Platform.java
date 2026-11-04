package ru.d2.elysium;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/** Fabric/Minecraft adapter for 26.3. */
public final class Platform {
    private static KeyMapping open, hud;

    public static Screen screen(Minecraft client) { return client.gui.screen(); }
    public static void show(Minecraft client, Screen screen) { client.gui.setScreen(screen); }
    public static boolean hudHidden(Minecraft client) { return client.gui.hud.isHidden(); }
    public static void register(ElysiumClient mod) {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("elysium_halloween", "event"));
        open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.elysium_halloween.open", InputConstants.KEY_K, category));
        hud = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.elysium_halloween.hud", InputConstants.KEY_H, category));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("elysium_halloween", "counters"),
                (graphics, tick) -> Overlay.render(mod, canvas(graphics)));
    }
    public static void tickKeys(ElysiumClient mod, Minecraft client) {
        while (open.consumeClick()) if (screen(client) == null) {
            show(client, new EventScreen(mod)); mod.requestStatistics();
        }
        while (hud.consumeClick()) if (screen(client) == null) mod.toggleHud();
    }
    public static Canvas canvas(GuiGraphicsExtractor g) {
        return new Canvas() {
            public int width() { return g.guiWidth(); }
            public int height() { return g.guiHeight(); }
            public void fill(int x, int y, int r, int b, int color) { g.fill(x, y, r, b, color); }
            public void text(String text, int x, int y, int color) { g.text(Minecraft.getInstance().font, text, x, y, color); }
            public int textWidth(String text) { return Minecraft.getInstance().font.width(text); }
        };
    }
    public static KeyMapping binding(boolean statistics) { return statistics ? open : hud; }
    public static void bind(boolean statistics, InputConstants.Key key) {
        binding(statistics).setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }
    public static boolean conflict(boolean statistics) {
        KeyMapping current = binding(statistics);
        if (current.isUnbound()) return false;
        for (KeyMapping other : Minecraft.getInstance().options.keyMappings)
            if (other != current && current.same(other)) return true;
        return false;
    }
}
