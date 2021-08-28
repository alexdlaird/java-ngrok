/*
 * Copyright (c) 2021 Alex Laird
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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
        assumeFalse(NgrokInstaller.getSystem().equals(WINDOWS));

        // WHEN
        assertThrows(JavaNgrokInstallerException.class, () -> ngrokInstaller.installNgrok(Paths.get("/no-perms")));
    }

    @Test
    public void testWebAddrFalseNotAllowed() {
        // WHEN
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Map.of("web_addr", "false")));
    }

    @Test
    public void testLogFormatJsonNotAllowed() {
        // WHEN
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Map.of("log_format", "json")));
    }

    @Test
    public void testLogLevelWarnNotAllowed() {
        // WHEN
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Map.of("log_level", "warn")));
    }

    @Test
    public void testGetNgrokBinaryMac() {
        // GIVEN
        mockSystemProperty("os.name", "Mac OS X");

        // WHEN
        final String ngrokBin = NgrokInstaller.getNgrokBin();

        // THEN
        assertEquals("ngrok", ngrokBin);
    }

    @Test
    public void testGetNgrokBinaryFreeBSD() {
        // GIVEN
        mockSystemProperty("os.name", "FreeBSD");

        // WHEN
        final String ngrokBin = NgrokInstaller.getNgrokBin();

        // THEN
        assertEquals("ngrok", ngrokBin);
    }

    @Test
    public void testGetNgrokBinaryWindows() {
        // GIVEN
        mockSystemProperty("os.name", "Windows 10");

        // WHEN
        final String ngrokBin = NgrokInstaller.getNgrokBin();

        // THEN
        assertEquals("ngrok.exe", ngrokBin);
    }

    @Test
    public void testGetNgrokBinaryCygwin() {
        // GIVEN
        mockSystemProperty("os.name", "Cygwin NT");

        // WHEN
        final String ngrokBin = NgrokInstaller.getNgrokBin();

        // THEN
        assertEquals("ngrok.exe", ngrokBin);
    }

    @Test
    public void testGetNgrokBinaryUnsupported() {
        // GIVEN
        mockSystemProperty("os.name", "Solaris");

        // WHEN
        assertThrows(JavaNgrokInstallerException.class, NgrokInstaller::getNgrokBin);
    }

    @Test
    public void testGetNgrokCDNUrlWindowsi386() {
        // GIVEN
        mockSystemProperty("os.name", "Windows 10");
        mockSystemProperty("os.arch", "i386");

        // WHEN
        final NgrokCDNUrl ngrokCDNUrl = ngrokInstaller.getNgrokCDNUrl();

        // THEN
        assertEquals(NgrokCDNUrl.WINDOWS_i386, ngrokCDNUrl);
    }

    @Test
    public void testGetNgrokCDNUrlLinuxARM() {
        // GIVEN
        mockSystemProperty("os.name", "Linux");
        mockSystemProperty("os.arch", "arm x86_64");

        // WHEN
        final NgrokCDNUrl ngrokCDNUrl = ngrokInstaller.getNgrokCDNUrl();

        // THEN
        assertEquals(NgrokCDNUrl.LINUX_x86_64_arm, ngrokCDNUrl);
    }
}
