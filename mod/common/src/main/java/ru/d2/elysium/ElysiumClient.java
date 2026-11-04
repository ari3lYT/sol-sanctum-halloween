package ru.d2.elysium;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;

import net.minecraft.world.level.storage.LevelResource;
import ru.d2.elysium.core.Counter;
import ru.d2.elysium.core.LocalStore;
import ru.d2.elysium.core.SiteSync;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ElysiumClient implements ClientModInitializer {
    private static ElysiumClient INSTANCE;
    public LocalStore store;
    public Counter counter;
    private final KillObserver observer = new KillObserver();
    private ClientPacketListener connection;
    private SiteSync siteSync;
    private String activeProfileId;
    private boolean firstPacket = true;
    private boolean resetPending;
    private boolean dirty;
    private long lastRequest;
    private long lastResponse;
    private long lastSave;

    public static ElysiumClient instance() { return INSTANCE; }

    @Override public void onInitializeClient() {
        INSTANCE = this;
        store = new LocalStore(FabricLoader.getInstance().getConfigDir().resolve("elysium-halloween"));
        if (store.config.syncWebsite) {
            try { siteSync = new SiteSync(FabricLoader.getInstance().getConfigDir().resolve("elysium-halloween")); }
            catch (java.io.IOException ignored) { /* Tracking remains available if the config folder is unwritable. */ }
        }
        Platform.register(this);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> { store.save(); if (siteSync != null) siteSync.close(); });
    }
    private boolean connect(Minecraft client) {
        ClientPacketListener next = client.getConnection();
        if (next == null || client.player == null || client.level == null) return false;
        if (next == connection) return true;
        String origin;
        if (client.getSingleplayerServer() != null) {
            origin = "world:" + client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        } else if (client.getCurrentServer() != null) {
            origin = "server:" + normalizeAddress(client.getCurrentServer().ip);
        } else return false;
        if (dirty) store.save();
        String identity = origin + "|" + client.player.getUUID();
        counter = store.profile(identity);
        activeProfileId = LocalStore.profileKey(identity);
        connection = next;
        firstPacket = true;
        resetPending = false;
        observer.clear();
        lastResponse = 0;
        lastRequest = 0;
        dirty = true;
        return true;
    }
    public static String normalizeAddress(String address) {
        String text = address.trim().toLowerCase(Locale.ROOT);
        return text.endsWith(":25565") ? text.substring(0, text.length() - 6) : text;
    }
    private void tick(Minecraft client) {
        if (!connect(client)) {
            if (connection != null) {
                store.save();
                connection = null;
                counter = null;
                resetPending = false;
            }
            return;
        }
        Platform.tickKeys(this, client);
        observer.expire(client);
        long now = System.currentTimeMillis();
        if (now - lastRequest >= store.config.pollSeconds * 1000L) requestStatistics();
        if (dirty && now - lastSave >= 1000) {
            dirty = !store.save();
            lastSave = now;
        }
    }
    public void requestStatistics() {
        if (connection == null) return;
        long now = System.currentTimeMillis();
        if (now - lastRequest < 1000) return;
        lastRequest = now;
        // Request ordinary Minecraft statistics. Event snapshots are sent separately by SiteSync.
        connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
    }
    public void receive(ClientPacketListener source, ClientboundAwardStatsPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (!connect(client) || source != connection) return;
        Map<String, Integer> snapshot = new LinkedHashMap<>();
        packet.stats().forEach((stat, value) -> {
            if (stat.getType() == Stats.ENTITY_KILLED && stat.getValue() instanceof EntityType<?> entity) {
                String key = BuiltInRegistries.ENTITY_TYPE.getKey(entity).toString();
                if (Counter.relevant(key)) snapshot.put(key, value);
            }
        });
        long now = System.currentTimeMillis();
        dirty |= counter.apply(snapshot, firstPacket, resetPending, now);
        lastResponse = now;
        firstPacket = false;
        resetPending = false;
        if (siteSync != null && store.config.syncWebsite && client.getSingleplayerServer() == null && client.getCurrentServer() != null) {
            siteSync.offer(SiteSync.snapshot(activeProfileId, client.player.getUUID(), client.player.getName().getString(), FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString(), counter));
        }
    }
    public void damage(ClientPacketListener source, net.minecraft.network.protocol.game.ClientboundDamageEventPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (connect(client) && source == connection && !firstPacket && !resetPending) observer.damage(client, packet);
    }
    public void death(ClientPacketListener source, net.minecraft.network.protocol.game.ClientboundEntityEventPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (connect(client) && source == connection && !firstPacket && !resetPending && observer.death(client, packet, counter)) {
            dirty = true;
            // The normal polling interval confirms the kill with the server's vanilla statistics.
        }
    }
    public void settingsChanged() {
        dirty = true;
        store.save();
    }
    public void toggleHud() {
        store.config.showHud = !store.config.showHud;
        dirty = true;
    }
    public void toggleTab() {
        store.config.showInTab = !store.config.showInTab;
        dirty = true;
    }
    public void reset() {
        observer.clear();
        resetPending = true;
        requestStatistics();
    }
    public String status() {
        if (!store.error.isEmpty()) return store.error;
        if (resetPending) return "Сброс после ответа сервера…";
        if (lastResponse == 0) return "Загрузка статистики…";
        if (System.currentTimeMillis() - lastResponse > Math.max(15000L, store.config.pollSeconds * 3000L))
            return "Сервер не отвечает на запрос статистики";
        return "";
    }
    public String summary() {
        if (counter == null) return "Элизиум: нет активного мира";
        return "Элизиум · Хэллоуин\nОбычные конфеты: " + counter.ordinaryCandies()
            + "\nУникальные конфеты: " + counter.uniqueCandies()
            + "\nОчки: " + counter.points(store.config)
            + "\nМобы: " + counter.totalKills();
    }
}
