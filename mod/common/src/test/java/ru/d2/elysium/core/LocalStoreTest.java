package ru.d2.elysium.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LocalStoreTest {
    @TempDir Path directory;
    @Test void settingsValidateAgainstNegativeAndExcessiveValues() {
        Config c = new Config(); c.ordinaryPoints = -1; c.uniquePoints = Integer.MAX_VALUE; c.pollSeconds = 0; c.validate();
        assertEquals(0, c.ordinaryPoints); assertEquals(100000, c.uniquePoints); assertEquals(3, c.pollSeconds);
    }
    @Test void profilesAreSeparateAndDoNotPersistPlainAddresses() throws Exception {
        LocalStore s = new LocalStore(directory); s.profile("serverA|player1").ordinary = 2;
        assertEquals(0, s.profile("serverB|player1").ordinary); assertEquals(0, s.profile("serverA|player2").ordinary);
        assertTrue(s.save()); assertFalse(Files.readString(directory.resolve("statistics.json")).contains("serverA"));
    }
    @Test void persistsCandyBaselineAndPendingConfirmation() {
        LocalStore s = new LocalStore(directory); Counter c = s.profile("a"); c.apply(Map.of("minecraft:zombie", 2), true, false, 1000);
        c.observe("one", "minecraft:zombie", true, 1100); assertTrue(s.save());
        LocalStore loaded = new LocalStore(directory); loaded.profile("a").apply(Map.of("minecraft:zombie", 3), true, false, 2000);
        assertEquals(1, loaded.profile("a").uniqueCandies());
        assertTrue(loaded.save()); assertEquals(1, new LocalStore(directory).profile("a").uniqueCandies());
    }
    @Test void damagedFileIsPreservedAndBackupRecovered() throws Exception {
        LocalStore s = new LocalStore(directory); s.profile("a").ordinary = 2; s.save(); s.save();
        Files.writeString(directory.resolve("statistics.json"), "{broken");
        LocalStore loaded = new LocalStore(directory); assertEquals(2, loaded.profile("a").ordinaryCandies());
        try (var paths = Files.list(directory)) { assertTrue(paths.anyMatch(p -> p.getFileName().toString().contains(".damaged-"))); }
    }
    @Test void unknownFutureSchemaIsNotOverwritten() throws Exception {
        String json = "{\"schema\":99,\"profiles\":{}}"; Files.writeString(directory.resolve("statistics.json"), json);
        assertFalse(new LocalStore(directory).save()); assertEquals(json, Files.readString(directory.resolve("statistics.json")));
    }
}
