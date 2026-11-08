package de.lpchat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import java.util.jar.JarFile;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;

/** Downloads only stable, verified LPChat releases; never replaces a running plugin. */
final class GitHubUpdater implements AutoCloseable {
    static final URI LATEST = URI.create("https://api.github.com/repos/Tobias30yt/LPChat/releases/latest");
    private static final String DOWNLOAD_ROOT = "https://github.com/Tobias30yt/LPChat/releases/download/";
    static final int MAX_JAR_BYTES = 32 * 1024 * 1024;
    private final Transport transport;
    private volatile boolean closed;

    interface Transport extends AutoCloseable {
        byte[] get(URI uri, int maxBytes) throws IOException, InterruptedException;
        @Override default void close() {}
    }

    GitHubUpdater() { this(new GitHubTransport()); }
    GitHubUpdater(Transport transport) { this.transport = transport; }

    record Version(int major, int minor, int patch) implements Comparable<Version> {
        static Version parse(String value) {
            if (value == null || !value.matches("v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)")) {
                throw new IllegalArgumentException("Keine stabile Versionsnummer: " + value);
            }
            String[] parts = value.replaceFirst("^v", "").split("\\.");
            return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        @Override public int compareTo(Version other) {
            int result = Integer.compare(major, other.major);
            if (result == 0) result = Integer.compare(minor, other.minor);
            if (result == 0) result = Integer.compare(patch, other.patch);
            return result;
        }
        @Override public String toString() { return major + "." + minor + "." + patch; }
    }

    String checkAndStage(String current, String serverVersion, Path updateDirectory, String installedName)
            throws IOException, InterruptedException {
        if (!installedName.matches("[^/\\\\]+\\.jar") || installedName.equals("..jar")) {
            throw new IOException("Ungueltiger Plugin-Dateiname");
        }
        Version local = Version.parse(current);
        if (closed) return "Update-Pruefung abgebrochen.";
        JsonObject release = JsonParser.parseString(new String(transport.get(LATEST, 1024 * 1024), StandardCharsets.UTF_8))
                .getAsJsonObject();
        if (release.get("draft").getAsBoolean() || release.get("prerelease").getAsBoolean()) {
            return "Vorabversion ignoriert.";
        }
        String tag = release.get("tag_name").getAsString();
        Version remote = Version.parse(tag);
        if (remote.compareTo(local) <= 0) return "LPChat ist aktuell (" + current + ").";
        String assetName = "LPChat-" + remote + ".jar";
        JsonObject asset = null;
        for (var element : release.getAsJsonArray("assets")) {
            JsonObject candidate = element.getAsJsonObject();
            if (assetName.equals(candidate.get("name").getAsString())) {
                if (asset != null) throw new IOException("Mehrdeutiges Release-Asset");
                asset = candidate;
            }
        }
        if (asset == null) throw new IOException("Release enthaelt keine " + assetName);
        if (!"uploaded".equals(asset.get("state").getAsString())) throw new IOException("Release-Asset ist nicht fertig");
        long size = asset.get("size").getAsLong();
        if (size < 1 || size > MAX_JAR_BYTES) throw new IOException("Ungueltige Downloadgroesse");
        String digest = asset.has("digest") && !asset.get("digest").isJsonNull() ? asset.get("digest").getAsString() : "";
        if (!digest.matches("sha256:[0-9a-fA-F]{64}")) throw new IOException("GitHub-SHA256-Pruefsumme fehlt");
        String expectedUrl = DOWNLOAD_ROOT + tag + "/" + assetName;
        if (!expectedUrl.equals(asset.get("browser_download_url").getAsString())) {
            throw new IOException("Unerwartete Downloadquelle");
        }
        if (closed) return "Update-Pruefung abgebrochen.";
        byte[] data = transport.get(URI.create(expectedUrl), (int) size);
        if (data.length != size || !sha256(data).equalsIgnoreCase(digest.substring(7))) {
            throw new IOException("Downloadgroesse oder SHA256-Pruefsumme stimmt nicht");
        }
        if (closed) return "Update-Pruefung abgebrochen.";
        Files.createDirectories(updateDirectory);
        Path destination = updateDirectory.resolve(installedName);
        // Paper inspects every file at depth 1, even without a .jar extension. Keep candidates
        // in a subdirectory so an interrupted validation can never be installed on restart.
        Path stagingDirectory = updateDirectory.resolve(".lpchat-downloads");
        Files.createDirectories(stagingDirectory);
        Path temporary = Files.createTempFile(stagingDirectory, ".lpchat-", ".download");
        try {
            Files.write(temporary, data);
            verifyJar(temporary, remote, serverVersion);
            synchronized (this) {
                if (closed) return "Update-Pruefung abgebrochen.";
                if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                    if (!Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Updateziel ist keine normale Datei");
                    PluginDescriptionFile pending = description(destination);
                    if (!"LPChat".equals(pending.getName())) throw new IOException("Updateziel gehoert zu einem anderen Plugin");
                    if (Version.parse(pending.getVersion()).compareTo(remote) >= 0) return "Gleiches oder neueres LPChat-Update liegt bereits bereit.";
                }
                try {
                    Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    // Do not risk leaving a half-written .jar that Paper would load on the next start.
                    throw new IOException("Updateordner unterstuetzt kein atomares Ersetzen", exception);
                }
            }
            return "LPChat " + remote + " heruntergeladen und geprueft. Aktiv ab dem naechsten Serverneustart.";
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private static PluginDescriptionFile description(Path file) throws IOException {
        try (JarFile jar = new JarFile(file.toFile())) {
            var entry = jar.getJarEntry("plugin.yml");
            if (entry == null) throw new IOException("plugin.yml fehlt");
            try (InputStream stream = jar.getInputStream(entry)) {
                byte[] metadata = stream.readNBytes(65537);
                if (metadata.length > 65536) throw new IOException("plugin.yml ist zu gross");
                return new PluginDescriptionFile(new ByteArrayInputStream(metadata));
            } catch (InvalidDescriptionException exception) { throw new IOException("Ungueltige plugin.yml", exception); }
        }
    }

    static void verifyJar(Path file, Version release, String serverVersion) throws IOException {
        PluginDescriptionFile metadata = description(file);
        if (!"LPChat".equals(metadata.getName()) || !"de.lpchat.LPChatPlugin".equals(metadata.getMain())
                || !release.toString().equals(metadata.getVersion())) throw new IOException("JAR passt nicht zum LPChat-Release");
        if (!compatibleApi(metadata.getAPIVersion(), serverVersion)) throw new IOException("Update benoetigt eine neuere oder unbekannte Minecraft-Version");
        try (JarFile jar = new JarFile(file.toFile())) {
            var main = jar.getJarEntry("de/lpchat/LPChatPlugin.class");
            if (main == null) throw new IOException("Plugin-Hauptklasse fehlt");
            try (InputStream stream = jar.getInputStream(main)) {
                byte[] header = stream.readNBytes(8);
                if (header.length != 8 || header[0] != (byte) 0xca || header[1] != (byte) 0xfe
                        || header[2] != (byte) 0xba || header[3] != (byte) 0xbe) throw new IOException("Ungueltige Hauptklasse");
                int javaVersion = ((header[6] & 255) << 8) | (header[7] & 255);
                if (javaVersion > Runtime.version().feature() + 44) throw new IOException("Update benoetigt eine neuere Java-Version");
            }
        }
    }

    static boolean compatibleApi(String required, String current) {
        if (required == null || current == null || !required.matches("[0-9]+(\\.[0-9]+){1,2}")
                || !current.matches("[0-9]+(\\.[0-9]+){1,2}")) return false;
        String[] target = required.split("\\.");
        String[] running = current.split("\\.");
        for (int i = 0; i < Math.max(target.length, running.length); i++) {
            int comparison = Integer.compare(i < target.length ? Integer.parseInt(target[i]) : 0,
                    i < running.length ? Integer.parseInt(running[i]) : 0);
            if (comparison != 0) return comparison < 0;
        }
        return true;
    }

    @Override public synchronized void close() {
        closed = true;
        transport.close();
    }

    private static final class GitHubTransport implements Transport {
        private static final Set<String> HOSTS = Set.of("api.github.com", "github.com", "release-assets.githubusercontent.com",
                "objects.githubusercontent.com", "github-releases.githubusercontent.com");
        private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();

        @Override public byte[] get(URI uri, int maxBytes) throws IOException, InterruptedException {
            for (int redirects = 0; redirects < 5; redirects++) {
                if (!"https".equals(uri.getScheme()) || !HOSTS.contains(uri.getHost()) || uri.getUserInfo() != null
                        || (uri.getPort() != -1 && uri.getPort() != 443)) throw new IOException("Nicht erlaubte Download-Adresse");
                var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(45))
                        .header("User-Agent", "LPChat-Updater (Tobias30yt/LPChat)")
                        .header("Accept", uri.getHost().equals("api.github.com") ? "application/vnd.github+json" : "application/octet-stream")
                        .GET().build();
                var response = client.send(request, HttpResponse.BodyHandlers.limiting(HttpResponse.BodyHandlers.ofByteArray(), maxBytes));
                if (response.statusCode() == 200) return response.body();
                if (Set.of(301, 302, 303, 307, 308).contains(response.statusCode())) {
                    uri = uri.resolve(response.headers().firstValue("Location").orElseThrow(() -> new IOException("Weiterleitungsziel fehlt")));
                    continue;
                }
                throw new IOException("GitHub antwortet mit HTTP " + response.statusCode());
            }
            throw new IOException("Zu viele Download-Weiterleitungen");
        }

        @Override public void close() { client.shutdownNow(); }
    }
}
