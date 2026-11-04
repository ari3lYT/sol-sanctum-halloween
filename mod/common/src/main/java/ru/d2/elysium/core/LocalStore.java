package ru.d2.elysium.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LocalStore {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public static final class Database {
        public int schema = 1;
        public Map<String, Counter> profiles = new LinkedHashMap<>();
    }
    private final Path directory;
    public Config config;
    public Database database;
    public String error = "";
    public LocalStore(Path directory) {
        this.directory = directory;
        config = read("config.json", Config.class, new Config());
        database = read("statistics.json", Database.class, new Database());
        if (database.schema != 1 || database.profiles == null) {
            error = "Неизвестный формат статистики. Сохранение отключено.";
            database = new Database();
            readOnly = true;
        }
        config.validate();
    }
    private boolean readOnly;

    private <T> T read(String name, Class<T> type, T fallback) {
        Path file = directory.resolve(name);
        if (!Files.exists(file)) return fallback;
        try {
            T value = JSON.fromJson(Files.readString(file), type);
            if (value == null) throw new IOException("Empty JSON");
            if (value instanceof Database db && !valid(db)) throw new IOException("Invalid statistics");
            return value;
        } catch (IOException | RuntimeException ex) {
            try {
                Files.copy(file, directory.resolve(name + ".damaged-" + System.currentTimeMillis()));
                Path backup = directory.resolve(name + ".bak");
                if (Files.exists(backup)) {
                    T value = JSON.fromJson(Files.readString(backup), type);
                    if (value != null && (!(value instanceof Database db) || valid(db))) {
                        error = "Данные восстановлены из резервной копии";
                        return value;
                    }
                }
                error = "Повреждённый файл сохранён с суффиксом .damaged";
            } catch (IOException | RuntimeException recovery) {
                error = "Ошибка чтения данных. Сохранение отключено.";
                readOnly = true;
            }
            return fallback;
        }
    }
    private static boolean valid(Database db) {
        if (db.profiles == null) return false;
        return db.profiles.values().stream().allMatch(c -> c != null && c.mobKills != null
                && c.baseline != null && c.ordinary >= 0 && c.unique >= 0 && c.pending != null
                && c.pending.stream().allMatch(k -> k != null && k.uuid() != null && Counter.relevant(k.entityType()))
                && c.mobKills.values().stream().allMatch(n -> n != null && n >= 0)
                && c.baseline.entrySet().stream().allMatch(e -> e.getValue() != null && e.getValue() >= 0));
    }
    public Counter profile(String identity) {
        return database.profiles.computeIfAbsent(profileKey(identity), ignored -> new Counter());
    }
    public static String profileKey(String identity) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    public boolean save() {
        if (readOnly) return false;
        try {
            Files.createDirectories(directory);
            atomicWrite("config.json", JSON.toJson(config));
            atomicWrite("statistics.json", JSON.toJson(database));
            if (error.startsWith("Ошибка сохранения")) error = "";
            return true;
        } catch (IOException | RuntimeException ex) {
            error = "Ошибка сохранения. Проверь доступ к папке config.";
            return false;
        }
    }
    private void atomicWrite(String name, String value) throws IOException {
        Path target = directory.resolve(name);
        Path temporary = Files.createTempFile(directory, name, ".tmp");
        try {
            Files.writeString(temporary, value, StandardCharsets.UTF_8);
            if (Files.exists(target)) Files.copy(target, directory.resolve(name + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
