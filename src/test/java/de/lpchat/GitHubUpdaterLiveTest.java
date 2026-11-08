package de.lpchat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real GitHub download, confined to a temporary directory; never installs a plugin. */
@EnabledIfSystemProperty(named = "lpchat.liveUpdateTest", matches = "true")
class GitHubUpdaterLiveTest {
    @TempDir Path directory;

    @Test void downloadsAndVerifiesPublishedReleaseFromGitHub() throws Exception {
        try (var updater = new GitHubUpdater()) {
            String result = updater.checkAndStage("0.0.0", "26.2", directory, "LPChat.jar");
            assertTrue(result.contains("heruntergeladen und geprueft"), result);
            assertTrue(Files.size(directory.resolve("LPChat.jar")) > 1000);
        }
    }
}
