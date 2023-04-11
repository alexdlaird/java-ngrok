/*
 * Copyright (c) 2023 Alex Laird
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
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.WINDOWS;
import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.getNgrokBin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class NgrokInstallerTest extends NgrokTestCase {

    @Test
    public void testInstallNgrokV2() throws IOException, InterruptedException {
        // GIVEN
        givenNgrokNotInstalled(javaNgrokConfigV2);

        // WHEN
        ngrokInstaller.installNgrok(javaNgrokConfigV2.getNgrokPath(), javaNgrokConfigV2.getNgrokVersion());
        ngrokProcessV2 = new NgrokProcess(javaNgrokConfigV2, ngrokInstaller);

        // THEN
        assertTrue(Files.exists(javaNgrokConfigV2.getNgrokPath()));
        assertTrue(ngrokProcessV2.getVersion().startsWith("2"));
    }

    @Test
    public void testInstallNgrokV3() throws IOException, InterruptedException {
        // GIVEN
        givenNgrokNotInstalled(javaNgrokConfigV3);

        // WHEN
        ngrokInstaller.installNgrok(javaNgrokConfigV3.getNgrokPath(), javaNgrokConfigV3.getNgrokVersion());
        ngrokProcessV3 = new NgrokProcess(javaNgrokConfigV3, ngrokInstaller);

        // THEN
        assertTrue(Files.exists(javaNgrokConfigV3.getNgrokPath()));
        assertTrue(ngrokProcessV3.getVersion().startsWith("3"));
    }

    @Test
    public void testInstallNgrokDefault() throws IOException, InterruptedException {
        // GIVEN
        JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
                .withConfigPath(Paths.get("build", ".ngrok2", "config_default.yml").toAbsolutePath())
                .withNgrokPath(Paths.get("build", "bin", "default", getNgrokBin()))
                .build();
        givenNgrokNotInstalled(javaNgrokConfig);

        // WHEN
        ngrokInstaller.installNgrok(javaNgrokConfig.getNgrokPath(), javaNgrokConfig.getNgrokVersion());
        ngrokProcessV3 = new NgrokProcess(javaNgrokConfig, ngrokInstaller);

        // THEN
        assertTrue(Files.exists(javaNgrokConfig.getNgrokPath()));
        assertTrue(ngrokProcessV3.getVersion().startsWith("3"));
    }

    @Test
    public void testInstallDefaultConfig() throws IOException {
        // GIVEN
        if (Files.exists(javaNgrokConfigV2.getConfigPath())) {
            Files.delete(javaNgrokConfigV2.getConfigPath());
        }
        assertFalse(Files.exists(javaNgrokConfigV2.getConfigPath()));

        // WHEN
        ngrokInstaller.installDefaultConfig(javaNgrokConfigV2.getConfigPath(), Collections.emptyMap());

        // THEN
        assertTrue(Files.exists(javaNgrokConfigV2.getConfigPath()));
    }

    @Test
    public void testGetDefaultNgrokConfig() {
        // GIVEN
        final JavaNgrokConfig javaNgrokConfigV2Tmp = new JavaNgrokConfig.Builder()
                .withConfigPath(Paths.get("build", ".ngrok2", "config_v2_tmp.yml").toAbsolutePath())
                .withNgrokPath(Paths.get("build", "bin", "v2", getNgrokBin()))
                .withNgrokVersion(NgrokVersion.V2)
                .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfigV2Tmp.getConfigPath(), Collections.emptyMap(), javaNgrokConfigV2Tmp.getNgrokVersion());

        // WHEN
        final Map<String, Object> ngrokConfig = ngrokInstaller.getNgrokConfig(javaNgrokConfigV2Tmp.getConfigPath(), true, javaNgrokConfigV2Tmp.getNgrokVersion());

        // THEN
        assertNotNull(ngrokConfig);
        assertEquals(0, ngrokConfig.size());
        assertTrue(Files.exists(javaNgrokConfigV2Tmp.getConfigPath()));
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
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(javaNgrokConfigV2.getConfigPath(), Map.of("web_addr", "false")));
    }

    @Test
    public void testLogFormatJsonNotAllowed() {
        // WHEN
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(javaNgrokConfigV2.getConfigPath(), Map.of("log_format", "json")));
    }

    @Test
    public void testLogLevelWarnNotAllowed() {
        // WHEN
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(javaNgrokConfigV2.getConfigPath(), Map.of("log_level", "warn")));
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
        assertEquals(NgrokV2CDNUrl.WINDOWS_i386, ngrokCDNUrl);
    }

    @Test
    public void testGetNgrokCDNUrlLinuxARM() {
        // GIVEN
        mockSystemProperty("os.name", "Linux");
        mockSystemProperty("os.arch", "arm x86_64");

        // WHEN
        final NgrokCDNUrl ngrokCDNUrl = ngrokInstaller.getNgrokCDNUrl();

        // THEN
        assertEquals(NgrokV2CDNUrl.LINUX_x86_64_arm, ngrokCDNUrl);
    }
}
