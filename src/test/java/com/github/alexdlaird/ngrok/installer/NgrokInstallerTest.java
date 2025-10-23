/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.installer;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.exception.JavaNgrokInstallerException;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.ngrok.NgrokTestCase;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.WINDOWS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class NgrokInstallerTest extends NgrokTestCase {

    @Test
    public void testInstallNgrokV2()
        throws IOException, InterruptedException {
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
    public void testInstallNgrokV3()
        throws IOException, InterruptedException {
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
    public void testInstallNgrokDefault()
        throws IOException, InterruptedException {
        // GIVEN
        JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
            .withConfigPath(Path.of("build", ".ngrok", "config_default.yml").toAbsolutePath())
            .withNgrokPath(Path.of("build", "bin", "default", NgrokInstaller.getNgrokBin()))
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
    public void testInstallDefaultConfig()
        throws IOException {
        // GIVEN
        if (Files.exists(javaNgrokConfigV3.getConfigPath())) {
            Files.delete(javaNgrokConfigV3.getConfigPath());
        }
        boolean defaultExists = Files.exists(NgrokInstaller.DEFAULT_CONFIG_PATH);
        assertFalse(Files.exists(javaNgrokConfigV3.getConfigPath()));

        // WHEN
        ngrokInstaller.installDefaultConfig(javaNgrokConfigV3.getConfigPath(), Map.of());

        // THEN
        assertTrue(Files.exists(javaNgrokConfigV3.getConfigPath()));
        // Asserting this way ensures CI validates a lack of a default config path, while running
        // locally one a dev's machine (where the default may exist) will not fail the test
        assertEquals(defaultExists, Files.exists(NgrokInstaller.DEFAULT_CONFIG_PATH));
    }

    @Test
    public void testGetDefaultNgrokV2Config() {
        // GIVEN
        final JavaNgrokConfig javaNgrokConfigV3Tmp = new JavaNgrokConfig.Builder()
            .withConfigPath(Path.of("build", ".ngrok", "config_v3_tmp.yml").toAbsolutePath())
            .withNgrokPath(Path.of("build", "bin", "v3", NgrokInstaller.getNgrokBin()))
            .withNgrokVersion(NgrokVersion.V3)
            .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfigV3Tmp.getConfigPath(),
            Map.of(), javaNgrokConfigV3Tmp.getNgrokVersion(), ConfigVersion.V2);

        // WHEN
        final Map<String, Object> ngrokConfig = ngrokInstaller.getNgrokConfig(javaNgrokConfigV3Tmp.getConfigPath());

        // THEN
        assertNotNull(ngrokConfig);
        assertEquals(2, ngrokConfig.size());
        assertEquals("2", ngrokConfig.get("version"));
        assertEquals("us", ngrokConfig.get("region"));
        assertTrue(Files.exists(javaNgrokConfigV3Tmp.getConfigPath()));
    }

    @Test
    public void testGetDefaultNgrokV3Config() {
        // GIVEN
        final JavaNgrokConfig javaNgrokConfigV3Tmp = new JavaNgrokConfig.Builder()
            .withConfigPath(Path.of("build", ".ngrok", "config_v3_tmp.yml").toAbsolutePath())
            .withNgrokPath(Path.of("build", "bin", "v3", NgrokInstaller.getNgrokBin()))
            .withNgrokVersion(NgrokVersion.V3)
            .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfigV3Tmp.getConfigPath(),
            Map.of(), javaNgrokConfigV3Tmp.getNgrokVersion(), ConfigVersion.V3);

        // WHEN
        final Map<String, Object> ngrokConfig = ngrokInstaller.getNgrokConfig(javaNgrokConfigV3Tmp.getConfigPath());

        // THEN
        assertNotNull(ngrokConfig);
        assertEquals(1, ngrokConfig.size());
        assertEquals("3", ngrokConfig.get("version"));
        assertTrue(Files.exists(javaNgrokConfigV3Tmp.getConfigPath()));
    }

    @Test
    public void testInstallToDirectoryFailsPermissions() {
        assumeFalse(NgrokInstaller.getSystem().equals(WINDOWS));

        // WHEN
        assertThrows(JavaNgrokInstallerException.class, () -> ngrokInstaller.installNgrok(Path.of("/no-perms")));
    }

    @Test
    public void testWebAddrFalseNotAllowed() {
        // WHEN
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(
            javaNgrokConfigV3.getConfigPath(), Map.of("web_addr", "false")));
    }

    @Test
    public void testLogFormatJsonNotAllowed() {
        // WHEN
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(
            javaNgrokConfigV3.getConfigPath(), Map.of("log_format", "json")));
    }

    @Test
    public void testLogLevelWarnNotAllowed() {
        // WHEN
        assertThrows(JavaNgrokException.class, () -> ngrokInstaller.installDefaultConfig(
            javaNgrokConfigV3.getConfigPath(), Map.of("log_level", "warn")));
    }

    @Test
    public void testGetDefaultNgrokDirMac() {
        // GIVEN
        mockSystemProperty("os.name", "macOS");
        mockSystemProperty("user.home", Path.of("my", "home").toString());

        // WHEN
        final Path defaultConfigPath = NgrokInstaller.getDefaultNgrokDir();

        // THEN
        assertEquals(Path.of("my", "home", "Library", "Application Support", "ngrok"), defaultConfigPath);
    }

    @Test
    public void testGetDefaultNgrokDirWindows() {
        // GIVEN
        mockSystemProperty("os.name", "Windows 10");
        mockSystemProperty("user.home", Path.of("my", "home").toString());

        // WHEN
        final Path defaultConfigPath = NgrokInstaller.getDefaultNgrokDir();

        // THEN
        assertEquals(Path.of("my", "home", "AppData", "Local", "ngrok"), defaultConfigPath);
    }

    @Test
	@ClearEnvironmentVariable(key = "XDG_CONFIG_HOME")
    public void testGetDefaultNgrokDirUnix() {
        // GIVEN
        mockSystemProperty("os.name", "Linux");
        mockSystemProperty("user.home", Path.of("my", "home").toString());

        // WHEN
        final Path defaultConfigPath = NgrokInstaller.getDefaultNgrokDir();

        // THEN
        assertEquals(Path.of("my", "home", ".config", "ngrok"), defaultConfigPath);
    }

    @Test
    public void testGetNgrokBinaryMac() {
        // GIVEN
        mockSystemProperty("os.name", "macOS");

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
    public void testGetNgrokBinaryLinux() {
        // GIVEN
        mockSystemProperty("os.name", "Linux");

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
    public void testGetNgrokBinaryCygwinIsWindows() {
        // GIVEN
        mockSystemProperty("os.name", "Cygwin NT");

        // WHEN
        final String ngrokBin = NgrokInstaller.getNgrokBin();

        // THEN
        assertEquals("ngrok.exe", ngrokBin);
    }

    @Test
    public void testGetNgrokBinaryMinGWIsWindows() {
        // GIVEN
        mockSystemProperty("os.name", "MinGW NT");

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
    public void testGetNgrokCDNUrlMacARM64() {
        // GIVEN
        mockSystemProperty("os.name", "macOS");
        mockSystemProperty("os.arch", "aarch64");

        // WHEN
        final NgrokCDNUrl ngrokCDNUrl = ngrokInstaller.getNgrokCDNUrl();

        // THEN
        assertEquals(NgrokV3CDNUrl.DARWIN_x86_64_arm, ngrokCDNUrl);
    }

    @Test
    public void testGetNgrokCDNUrlWindows32() {
        // GIVEN
        mockSystemProperty("os.name", "Windows 10");
        mockSystemProperty("os.arch", "i386");

        // WHEN
        final NgrokCDNUrl ngrokCDNUrl = ngrokInstaller.getNgrokCDNUrl();

        // THEN
        assertEquals(NgrokV3CDNUrl.WINDOWS_i386, ngrokCDNUrl);
    }

    @Test
    public void testGetNgrokCDNUrlLinuxARM64() {
        // GIVEN
        mockSystemProperty("os.name", "Linux");
        mockSystemProperty("os.arch", "arm x86_64");

        // WHEN
        final NgrokCDNUrl ngrokCDNUrl = ngrokInstaller.getNgrokCDNUrl();

        // THEN
        assertEquals(NgrokV3CDNUrl.LINUX_x86_64_arm, ngrokCDNUrl);
    }

    @Test
    public void testGetNgrokCDNUrlFreeBSD64() {
        // GIVEN
        mockSystemProperty("os.name", "FreeBSD");
        mockSystemProperty("os.arch", "x86_64");

        // WHEN
        final NgrokCDNUrl ngrokCDNUrl = ngrokInstaller.getNgrokCDNUrl();

        // THEN
        assertEquals(NgrokV3CDNUrl.FREEBSD_x86_64, ngrokCDNUrl);
    }

    @Test
    public void testGetNgrokCDNUrlCygwinIsWindows() {
        // GIVEN
        mockSystemProperty("os.name", "Cygwin NT");
        mockSystemProperty("os.arch", "aarch64");

        // WHEN
        final NgrokCDNUrl ngrokCDNUrl = ngrokInstaller.getNgrokCDNUrl();

        // THEN
        assertEquals(NgrokV3CDNUrl.WINDOWS_x86_64_arm, ngrokCDNUrl);
    }

    @Test
    public void testGetNgrokCDNUrlMinGWIsWindows() {
        // GIVEN
        mockSystemProperty("os.name", "MinGW NT");
        mockSystemProperty("os.arch", "i386");

        // WHEN
        final NgrokCDNUrl ngrokCDNUrl = ngrokInstaller.getNgrokCDNUrl();

        // THEN
        assertEquals(NgrokV3CDNUrl.WINDOWS_i386, ngrokCDNUrl);
    }

    @Test
    public void testDownloadFails()
        throws IOException, InterruptedException {
        // GIVEN
        givenNgrokNotInstalled(javaNgrokConfigV3);
        final HttpClient mockHttpClient = mock(HttpClient.class);
        final NgrokInstaller ngrokInstaller_2 = new NgrokInstaller(mockHttpClient);
        doAnswer(invocation -> {
            throw new SocketTimeoutException("Download failed");
        }).when(mockHttpClient).get(any(), any(), any(), any(Path.class));

        // WHEN
        assertThrows(JavaNgrokInstallerException.class, () -> ngrokInstaller_2.installNgrok(
            javaNgrokConfigV3.getNgrokPath(), javaNgrokConfigV3.getNgrokVersion()));

        // THEN
        assertFalse(Files.exists(javaNgrokConfigV3.getNgrokPath()));
    }

    @Test
    public void testEnsureInstallerUrlsExistV2()
        throws IOException {
        for (final NgrokCDNUrl ngrokCdnUrl : NgrokV2CDNUrl.values()) {
            final URL url = new URL(ngrokCdnUrl.getUrl());
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");

            final int responseCode = connection.getResponseCode();
            assertEquals(200, responseCode, String.format("Installer URL returned 404: %s", url));
        }
    }

    @Test
    public void testEnsureInstallerUrlsExistV3()
        throws IOException {
        for (final NgrokCDNUrl ngrokCdnUrl : NgrokV3CDNUrl.values()) {
            final URL url = new URL(ngrokCdnUrl.getUrl());
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");

            final int responseCode = connection.getResponseCode();
            assertEquals(200, responseCode, String.format("Installer URL returned 404: %s", url));
        }
    }
}