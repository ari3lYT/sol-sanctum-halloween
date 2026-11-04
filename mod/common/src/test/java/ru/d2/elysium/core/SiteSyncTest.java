package ru.d2.elysium.core;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class SiteSyncTest {
    @TempDir Path directory;
    private String snapshot(long kills) {
        Counter c = new Counter(); c.mobKills.put("minecraft:zombie", kills); c.mobKills.put("minecraft:zombified_piglin", 100L);
        c.ordinary = 1; c.unique = 1; c.startedAt = 1000;
        return SiteSync.snapshot("a".repeat(64), UUID.fromString("00000000-0000-0000-0000-000000000001"), "PlayerOne", "26.3", c);
    }
    @Test void snapshotContainsEventDataOnly() {
        String raw = snapshot(3); var body = JsonParser.parseString(raw).getAsJsonObject();
        assertEquals("PlayerOne", body.get("playerName").getAsString());
        assertFalse(raw.contains("zombified_piglin")); assertFalse(raw.contains("uploadKey")); assertFalse(raw.contains("serverAddress")); assertFalse(raw.contains("logs"));
    }
    @Test void rejectsInsecureNonLoopbackEndpoint() {
        assertThrows(IllegalArgumentException.class, () -> new SiteSync(directory, URI.create("http://example.com/ingest")));
        assertThrows(IllegalArgumentException.class, () -> new SiteSync(directory, URI.create("https://user:password@example.com/ingest")));
    }
    @Test void actualHttpUploadIsAuthenticatedAndUnchangedSnapshotsAreNotRepeated() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requests = new AtomicInteger(); AtomicReference<String> received = new AtomicReference<>(); AtomicReference<String> authorization = new AtomicReference<>(); CountDownLatch first = new CountDownLatch(1);
        server.createContext("/ingest", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)); authorization.set(exchange.getRequestHeaders().getFirst("Authorization")); requests.incrementAndGet();
            byte[] reply = "{\"ok\":true}".getBytes(); exchange.sendResponseHeaders(200, reply.length); exchange.getResponseBody().write(reply); exchange.close(); first.countDown();
        });server.start();
        try (SiteSync sync = new SiteSync(directory, URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/ingest"))) {
            sync.offer(snapshot(3)); assertTrue(first.await(8, TimeUnit.SECONDS));
            var body = JsonParser.parseString(received.get()).getAsJsonObject(); assertTrue(body.get("sequence").getAsLong() > 0); assertNotNull(body.get("installationId")); assertTrue(authorization.get().startsWith("Bearer "));
            Thread.sleep(100); sync.offer(snapshot(3)); Thread.sleep(100); assertEquals(1, requests.get());
            String state = Files.readString(directory.resolve("website.json")); assertTrue(state.contains("uploadKey")); assertFalse(received.get().contains("uploadKey"));
        } finally { server.stop(0); }
    }
    @Test void serverUnavailableRetriesWithoutBlockingCaller() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); AtomicInteger attempts = new AtomicInteger(); CountDownLatch recovered = new CountDownLatch(1);
        server.createContext("/ingest", exchange -> {
            exchange.getRequestBody().readAllBytes(); int n = attempts.incrementAndGet(); byte[] reply=(n==1 ? "{\"error\":\"busy\"}" : "{\"ok\":true}").getBytes(); exchange.sendResponseHeaders(n==1?503:200,reply.length);exchange.getResponseBody().write(reply);exchange.close();if(n>1)recovered.countDown();
        });server.start();
        try(SiteSync sync = new SiteSync(directory, URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/ingest"))) {
            long start = System.nanoTime(); sync.offer(snapshot(3)); assertTrue(System.nanoTime()-start < TimeUnit.MILLISECONDS.toNanos(500)); assertTrue(recovered.await(12, TimeUnit.SECONDS)); assertEquals(2,attempts.get());
        } finally {server.stop(0);}
    }
}
