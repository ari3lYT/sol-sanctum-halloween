package ru.d2.elysium;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import com.mojang.blaze3d.platform.InputConstants;

public final class EventGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            context.waitFor(client -> ElysiumClient.instance().counter != null && ElysiumClient.instance().counter.initialized);
            kill(context, world, EntityType.ZOMBIE, Items.CARVED_PUMPKIN);
            kill(context, world, EntityType.SKELETON, Items.JACK_O_LANTERN);
            kill(context, world, EntityType.ZOMBIFIED_PIGLIN, Items.JACK_O_LANTERN);
            kill(context, world, EntityType.ZOMBIE, Items.AIR);
            context.waitFor(client -> ElysiumClient.instance().counter.totalKills() == 3);
            context.runOnClient(client -> {
                var mod = ElysiumClient.instance();
                if (mod.counter.ordinaryCandies() != 1 || mod.counter.uniqueCandies() != 1 || mod.counter.points(mod.store.config) != 4)
                    throw new AssertionError("Expected 1 ordinary, 1 unique, 4 points, got " + mod.summary());
            });
            context.takeScreenshot("elysium-hud");
            context.getInput().pressKey(InputConstants.KEY_K);
            context.waitForScreen(EventScreen.class);
            context.clickScreenButton("Настройки");
            context.waitForScreen(SettingsScreen.class);
            context.clickScreenButton("HUD: вкл");
            context.runOnClient(client -> { if (ElysiumClient.instance().store.config.showHud) throw new AssertionError("HUD toggle"); });
            context.clickScreenButton("HUD: выкл");
            context.clickScreenButton("Tab: вкл");
            context.runOnClient(client -> { if (ElysiumClient.instance().store.config.showInTab) throw new AssertionError("Tab toggle"); });
            context.clickScreenButton("Tab: выкл");
            context.clickScreenButton("Положение HUD");
            context.waitForScreen(HudPositionScreen.class);
            int scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
            context.getInput().setCursorPos(30 * scale, 30 * scale);
            context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTick();
            context.getInput().setCursorPos(170 * scale, 110 * scale);
            context.waitTicks(2);
            context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.runOnClient(client -> {
                if (ElysiumClient.instance().store.config.hudX <= 0 || ElysiumClient.instance().store.config.hudY <= 0)
                    throw new AssertionError("Dragging did not move HUD");
            });
            context.takeScreenshot("elysium-hud-position");
            context.clickScreenButton("Готово");
            context.waitForScreen(SettingsScreen.class);
            context.clickScreenButton("Статистика: K");
            context.getInput().pressKey(InputConstants.KEY_J);
            context.runOnClient(client -> {
                if (!Platform.binding(true).getTranslatedKeyMessage().getString().equalsIgnoreCase("J")) throw new AssertionError("Key rebind failed");
            });
            context.clickScreenButton("Статистика: J");
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitForScreen(SettingsScreen.class);
            context.clickScreenButton("Скрыть HUD: H");
            context.getInput().pressKey(InputConstants.KEY_G);
            context.takeScreenshot("elysium-settings");
            context.clickScreenButton("Готово");
            context.waitForScreen(EventScreen.class);
            context.clickScreenButton("Готово");
            context.getInput().pressKey(InputConstants.KEY_G);
            context.runOnClient(client -> { if (ElysiumClient.instance().store.config.showHud) throw new AssertionError("New HUD key did not work"); });
            context.getInput().pressKey(InputConstants.KEY_G);
            context.getInput().holdKey(options -> options.keyPlayerList);
            context.waitTick();
            context.takeScreenshot("elysium-tab");
            context.getInput().releaseKey(options -> options.keyPlayerList);
            context.getInput().pressKey(InputConstants.KEY_J);
            context.waitForScreen(EventScreen.class);
            context.takeScreenshot("elysium-statistics");
            context.clickScreenButton("Сбросить");
            context.runOnClient(client -> { if (ElysiumClient.instance().counter.points(ElysiumClient.instance().store.config) != 4) throw new AssertionError("Premature reset"); });
            context.clickScreenButton("Подтвердить сброс");
            world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); p.getStats().sendStats(p); });
            context.waitFor(client -> ElysiumClient.instance().counter.points(ElysiumClient.instance().store.config) == 0);
            context.clickScreenButton("Готово");
        }
    }
    private void kill(ClientGameTestContext context, TestSingleplayerContext world, EntityType<? extends Mob> type, Item head) {
        int id = world.getServer().computeOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            var level = player.level();
            Mob mob = type.create(level, EntitySpawnReason.COMMAND);
            if (mob == null) throw new AssertionError("Failed to create test mob");
            mob.setNoAi(true);
            mob.setPos(player.getX() + 2, player.getY(), player.getZ());
            mob.setItemSlot(EquipmentSlot.HEAD, head.getDefaultInstance());
            mob.setHealth(1);
            level.addFreshEntity(mob);
            return mob.getId();
        });
        context.waitFor(client -> client.level != null && client.level.getEntity(id) instanceof Mob mob
                && mob.getItemBySlot(EquipmentSlot.HEAD).is(head));
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            var mob = (Mob) p.level().getEntity(id);
            mob.hurtServer(p.level(), p.damageSources().playerAttack(p), 10);
            p.getStats().sendStats(p);
        });
        context.waitTicks(5);
        context.waitTicks(2);
    }
}
