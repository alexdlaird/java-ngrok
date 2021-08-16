package com.github.alexdlaird.ngrok.installer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NgrokInstallerTest {

    private final NgrokInstaller ngrokInstaller = new NgrokInstaller();

    @Test
    public void testInstallNgrok() throws IOException {
        // GIVEN
        final Path ngrokPath = Paths.get(System.getProperty("user.home"), ".ngrok2", NgrokInstaller.getNgrokBin());
        Files.delete(ngrokPath);
        assertFalse(Files.exists(ngrokPath));

        // WHEN
        ngrokInstaller.installNgrok(ngrokPath);

        // THEN
        assertTrue(Files.exists(ngrokPath));
    }

    @Test
    public void testInstallDefaultConfig() throws IOException {
        // GIVEN
        final Path configPath = Paths.get(System.getProperty("user.home"), ".ngrok2", "ngrok.yml");
        Files.delete(configPath);
        assertFalse(Files.exists(configPath));

        // WHEN
        ngrokInstaller.installDefaultConfig(configPath, Collections.emptyMap());

        // THEN
        assertTrue(Files.exists(configPath));
    }
}
