package ru.d2.elysium.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Sends event snapshots only. Files, logs, server addresses and game/session tokens are never read. */
public final class SiteSync implements AutoCloseable {
    private static final Gson JSON = new Gson();
    public static final URI ENDPOINT = URI.create(System.getProperty("solsanctum.endpoint", "http://127.0.0.1:18126/halloween/api/ingest"));
    private static final class Identity {
        int schema = 1;
        String installationId;
        String uploadKey;
        long sequence;
    }
    private record Snapshot(String body, String fingerprint) {}
    private final URI endpoint;
    private final Path identityFile;
    private final Identity identity;
    private final HttpClient http;
    private final ScheduledExecutorService worker;
    private final AtomicReference<Snapshot> latest = new AtomicReference<>();
    private volatile String acknowledged = "";
    private long nextAttempt;
    private int failures;


    public SiteSync(Path directory) throws IOException { this(directory, ENDPOINT); }
    public SiteSync(Path directory, URI endpoint) throws IOException {
        boolean https = "https".equals(endpoint.getScheme());
        boolean testLoopback = "http".equals(endpoint.getScheme()) && ("127.0.0.1".equals(endpoint.getHost()) || "localhost".equals(endpoint.getHost()));
        if ((!https && !testLoopback) || endpoint.getUserInfo() != null || endpoint.getHost() == null || endpoint.getFragment() != null)
            throw new IllegalArgumentException("Event endpoint requires HTTPS");
        this.endpoint = endpoint;
        Files.createDirectories(directory);
        identityFile = directory.resolve("website.json");
        identity = loadIdentity();
        saveIdentity();
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NEVER).build();
        worker = Executors.newSingleThreadScheduledExecutor(task -> { Thread thread = new Thread(task, "Sol Sanctum website"); thread.setDaemon(true); return thread; });
        worker.scheduleWithFixedDelay(this::flush, 1, 1, TimeUnit.SECONDS);
    }
    private Identity loadIdentity() throws IOException {
        if (Files.exists(identityFile)) {
            try {
                Identity loaded = JSON.fromJson(Files.readString(identityFile), Identity.class);
                if (loaded == null || loaded.schema != 1 || loaded.installationId == null || loaded.uploadKey == null
                        || !loaded.uploadKey.matches("[A-Za-z0-9_-]{32,128}") || loaded.sequence < 0) throw new IllegalArgumentException();
                UUID.fromString(loaded.installationId);
                return loaded;
            } catch (RuntimeException ex) {
                Files.copy(identityFile, identityFile.resolveSibling("website.json.damaged-" + System.currentTimeMillis()));
            }
        }
        Identity created = new Identity();
        created.installationId = UUID.randomUUID().toString();
        byte[] random = new byte[32]; new SecureRandom().nextBytes(random);
        created.uploadKey = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        return created;
    }
    private void saveIdentity() throws IOException {
        Path temporary = Files.createTempFile(identityFile.getParent(), "website-", ".tmp");
        try {
            Files.writeString(temporary, JSON.toJson(identity));
            try { Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------")); }
            catch (UnsupportedOperationException ignored) { }
            try { Files.move(temporary, identityFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, identityFile, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public static String snapshot(String profileId, UUID playerUuid, String name, String minecraftVersion, Counter counter) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("schema", 1); body.put("playerUuid", playerUuid.toString()); body.put("playerName", name);
        body.put("profileId", profileId); body.put("minecraftVersion", minecraftVersion); body.put("modVersion", "1.1.3");
        Map<String, Long> mobs = new LinkedHashMap<>();
        counter.mobKills.forEach((type, amount) -> { if (Counter.relevant(type) && amount > 0) mobs.put(type, amount); });
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("mobKills", mobs); stats.put("ordinaryCandies", counter.ordinaryCandies());
        stats.put("uniqueCandies", counter.uniqueCandies()); stats.put("startedAt", counter.startedAt);
        body.put("stats", stats);
        return JSON.toJson(body);
    }
    /** Called on the client thread with an already detached, immutable JSON snapshot. */
    public void offer(String snapshot) {
        if (worker.isShutdown()) return;
        String fingerprint = LocalStore.profileKey(snapshot);
        if (fingerprint.equals(acknowledged)) return;
        latest.set(new Snapshot(snapshot, fingerprint));
        try { worker.execute(this::flush); } catch (RejectedExecutionException ignored) { }
    }
    private void flush() {
        Snapshot snapshot = latest.get();
        if (snapshot == null || System.currentTimeMillis() < nextAttempt) return;
        if (snapshot.fingerprint().equals(acknowledged)) { latest.compareAndSet(snapshot, null); return; }
        try {
            identity.sequence = Math.max(identity.sequence + 1, System.currentTimeMillis());
            saveIdentity();
            JsonObject body = JSON.fromJson(snapshot.body(), JsonObject.class);
            body.addProperty("installationId", identity.installationId); body.addProperty("sequence", identity.sequence);
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(6))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + identity.uploadKey)
                .header("User-Agent", "Sol-Sanctum-Halloween/1.1.3").POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(body))).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 4096
                    || !JSON.fromJson(response.body(), JsonObject.class).get("ok").getAsBoolean()) throw new IOException("Event upload not accepted");
            acknowledged = snapshot.fingerprint(); latest.compareAndSet(snapshot, null); failures = 0;
            nextAttempt = System.currentTimeMillis() + 5000;
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        catch (IOException | RuntimeException unavailable) {
            failures = Math.min(6, failures + 1);
            nextAttempt = System.currentTimeMillis() + Math.min(60000L, 5000L << (failures - 1));
        }
    }
    @Override public void close() {
        worker.shutdownNow(); http.shutdownNow();
        try { worker.awaitTermination(1, TimeUnit.SECONDS); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }
}
