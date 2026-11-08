package de.lpchat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GitHubUpdaterTest {
    @TempDir Path folder;

    private byte[] jar(String name, String version, String api, int javaVersion) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var jar = new JarOutputStream(bytes)) {
            jar.putNextEntry(new JarEntry("plugin.yml"));
            jar.write(("name: " + name + "\nversion: " + version + "\nmain: de.lpchat.LPChatPlugin\napi-version: '" + api + "'\n").getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("de/lpchat/LPChatPlugin.class"));
            jar.write(new byte[]{(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 0, 0, (byte) (javaVersion >> 8), (byte) javaVersion});
            jar.closeEntry();
        }
        return bytes.toByteArray();
    }

    private JsonObject release(byte[] jar) {
        var release = new JsonObject();
        release.addProperty("tag_name", "v1.3.0");
        release.addProperty("draft", false);
        release.addProperty("prerelease", false);
        var asset = new JsonObject();
        asset.addProperty("name", "LPChat-1.3.0.jar");
        asset.addProperty("state", "uploaded");
        asset.addProperty("size", jar.length);
        asset.addProperty("digest", "sha256:" + GitHubUpdater.sha256(jar));
        asset.addProperty("browser_download_url", "https://github.com/Tobias30yt/LPChat/releases/download/v1.3.0/LPChat-1.3.0.jar");
        var assets = new JsonArray();
        assets.add(asset);
        release.add("assets", assets);
        return release;
    }

    private GitHubUpdater updater(JsonObject release, byte[] jar, AtomicInteger downloads) {
        return new GitHubUpdater((uri, max) -> {
            if (uri.equals(GitHubUpdater.LATEST)) return release.toString().getBytes(StandardCharsets.UTF_8);
            downloads.incrementAndGet();
            return jar;
        });
    }

    @Test void stagesVerifiedReleaseUnderInstalledFilenameWithoutTouchingLiveJar() throws Exception {
        byte[] payload = jar("LPChat", "1.3.0", "26.2", 69);
        Path live = folder.resolve("custom-name.jar");
        Files.writeString(live, "running-version");
        Path update = folder.resolve("update");
        try (var updater = updater(release(payload), payload, new AtomicInteger())) {
            assertTrue(updater.checkAndStage("1.2.0", "26.2", update, live.getFileName().toString()).contains("1.3.0"));
        }
        assertEquals("running-version", Files.readString(live));
        assertArrayEquals(payload, Files.readAllBytes(update.resolve("custom-name.jar")));
        try (var files = Files.list(update.resolve(".lpchat-downloads"))) { assertEquals(0, files.count()); }
    }

    @Test void equalOlderDraftAndPrereleaseNeverDownload() throws Exception {
        byte[] payload = jar("LPChat", "1.3.0", "26.2", 69);
        for (String current : new String[]{"1.3.0", "1.4.0", "2.0.0"}) {
            AtomicInteger downloads = new AtomicInteger();
            try (var updater = updater(release(payload), payload, downloads)) { updater.checkAndStage(current, "26.2", folder, "LPChat.jar"); }
            assertEquals(0, downloads.get());
        }
        for (String field : new String[]{"draft", "prerelease"}) {
            var metadata = release(payload);
            metadata.addProperty(field, true);
            AtomicInteger downloads = new AtomicInteger();
            try (var updater = updater(metadata, payload, downloads)) { updater.checkAndStage("1.2.0", "26.2", folder, "LPChat.jar"); }
            assertEquals(0, downloads.get());
        }
    }

    @Test void checksumFailureLeavesExistingPendingUpdateUntouched() throws Exception {
        byte[] payload = jar("LPChat", "1.3.0", "26.2", 69);
        Files.writeString(folder.resolve("LPChat.jar"), "existing");
        var metadata = release(payload);
        metadata.getAsJsonArray("assets").get(0).getAsJsonObject().addProperty("digest", "sha256:" + "0".repeat(64));
        try (var updater = updater(metadata, payload, new AtomicInteger())) {
            assertThrows(IOException.class, () -> updater.checkAndStage("1.2.0", "26.2", folder, "LPChat.jar"));
        }
        assertEquals("existing", Files.readString(folder.resolve("LPChat.jar")));
    }

    @Test void rejectsWrongPluginVersionMinecraftJavaAndNonJarPayloads() throws Exception {
        for (byte[] payload : new byte[][]{
                jar("OtherPlugin", "1.3.0", "26.2", 69), jar("LPChat", "1.2.0", "26.2", 69),
                jar("LPChat", "1.3.0", "26.3", 69), jar("LPChat", "1.3.0", "26.2", 120), "not a jar".getBytes(StandardCharsets.UTF_8)}) {
            try (var updater = updater(release(payload), payload, new AtomicInteger())) {
                assertThrows(IOException.class, () -> updater.checkAndStage("1.2.0", "26.2", folder, "LPChat.jar"));
            }
            assertFalse(Files.exists(folder.resolve("LPChat.jar")));
            try (var files = Files.list(folder.resolve(".lpchat-downloads"))) { assertEquals(0, files.count()); }
        }
    }

    @Test void missingDigestUnexpectedSourceAndOversizeAssetAreRejectedBeforeDownload() throws Exception {
        byte[] payload = jar("LPChat", "1.3.0", "26.2", 69);
        for (String badField : new String[]{"digest", "browser_download_url", "size"}) {
            var metadata = release(payload);
            var asset = metadata.getAsJsonArray("assets").get(0).getAsJsonObject();
            if (badField.equals("size")) asset.addProperty(badField, GitHubUpdater.MAX_JAR_BYTES + 1);
            else asset.addProperty(badField, "invalid");
            AtomicInteger downloads = new AtomicInteger();
            try (var updater = updater(metadata, payload, downloads)) {
                assertThrows(IOException.class, () -> updater.checkAndStage("1.2.0", "26.2", folder, "LPChat.jar"));
            }
            assertEquals(0, downloads.get());
        }
    }

    @Test void doesNotDowngradePendingReleaseAndClosePreventsDownloads() throws Exception {
        byte[] payload = jar("LPChat", "1.3.0", "26.2", 69);
        byte[] pending = jar("LPChat", "1.4.0", "26.2", 69);
        Files.write(folder.resolve("LPChat.jar"), pending);
        AtomicInteger downloads = new AtomicInteger();
        var updater = updater(release(payload), payload, downloads);
        assertTrue(updater.checkAndStage("1.2.0", "26.2", folder, "LPChat.jar").contains("bereits"));
        assertArrayEquals(pending, Files.readAllBytes(folder.resolve("LPChat.jar")));
        updater.close();
        updater.checkAndStage("1.2.0", "26.2", folder, "LPChat.jar");
        assertEquals(1, downloads.get());
    }

    @Test void networkFailuresAndPathTraversalDoNotCreateAnUpdate() throws Exception {
        try (var updater = new GitHubUpdater((uri, max) -> { throw new IOException("HTTP 403"); })) {
            assertThrows(IOException.class, () -> updater.checkAndStage("1.2.0", "26.2", folder, "LPChat.jar"));
            assertThrows(IOException.class, () -> updater.checkAndStage("1.2.0", "26.2", folder, "../LPChat.jar"));
        }
        try (var files = Files.list(folder)) { assertEquals(0, files.count()); }
    }

    @Test void comparesNumericVersionsAndCompatibility() {
        assertTrue(GitHubUpdater.Version.parse("v1.10.0").compareTo(GitHubUpdater.Version.parse("1.9.9")) > 0);
        assertThrows(IllegalArgumentException.class, () -> GitHubUpdater.Version.parse("1.3.0-beta"));
        assertTrue(GitHubUpdater.compatibleApi("26.2", "26.2.1"));
        assertFalse(GitHubUpdater.compatibleApi("26.3", "26.2"));
        assertFalse(GitHubUpdater.compatibleApi(null, "26.2"));
    }
}
