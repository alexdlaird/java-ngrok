package com.github.alexdlaird.ngrok.installer;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.exception.JavaNgrokInstallerException;
import com.github.alexdlaird.ngrok.NgrokTestCase;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.WINDOWS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NgrokInstallerTest extends NgrokTestCase {

    @Test
    public void testInstallNgrok() throws IOException, InterruptedException {
        // GIVEN
        if (Files.exists(javaNgrokConfig.getNgrokPath())) {
            // Due to Windows file locking behavior, wait a beat
            if (NgrokInstaller.getSystem().equals(WINDOWS)) {
                Thread.sleep(1000);
            }
            Files.delete(javaNgrokConfig.getNgrokPath());
        }
        assertFalse(Files.exists(javaNgrokConfig.getNgrokPath()));

        // WHEN
        ngrokInstaller.installNgrok(javaNgrokConfig.getNgrokPath());

        // THEN
        assertTrue(Files.exists(javaNgrokConfig.getNgrokPath()));
    }

    @Test
    public void testInstallDefaultConfig() throws IOException {
        // GIVEN
        if (Files.exists(javaNgrokConfig.getConfigPath())) {
            Files.delete(javaNgrokConfig.getConfigPath());
        }
        assertFalse(Files.exists(javaNgrokConfig.getConfigPath()));

        // WHEN
        ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Collections.emptyMap());

        // THEN
        assertTrue(Files.exists(javaNgrokConfig.getConfigPath()));
    }

    @Test
    public void testInstallToDirectoryFailsPermissions() {
        assertThrows(JavaNgrokInstallerException.class, () -> ngrokInstaller.installNgrok(Paths.get("/no-perms")));
    }

    @Test
    public void testWebAddrFalseNotAllowed() {
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Map.of("web_addr", "false")));
    }

    @Test
    public void testLogFormatJsonNotAllowed() {
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Map.of("log_format", "json")));
    }

    @Test
    public void testLogLevelWarnNotAllowed() {
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Map.of("log_level", "warn")));
    }
}
