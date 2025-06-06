/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.process;

import com.github.alexdlaird.exception.NgrokException;
import com.github.alexdlaird.ngrok.NgrokTestCase;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.WINDOWS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class NgrokProcessTest extends NgrokTestCase {

    @Test
    public void testStart() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        assertFalse(ngrokProcessV3.isRunning());

        // WHEN
        ngrokProcessV3.start();

        // THEN
        assertTrue(ngrokProcessV3.isRunning());
    }

    @Test
    public void testStop()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokProcessV3.start();

        // WHEN
        ngrokProcessV3.stop();
        Thread.sleep(1000);

        // THEN
        assertFalse(ngrokProcessV3.isRunning());
    }

    @Test
    public void testStartPortInUseV2()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        assertFalse(ngrokProcessV2.isRunning());
        ngrokProcessV2.start();
        assertTrue(ngrokProcessV2.isRunning());
        final Path ngrokPath2 = Path.of(javaNgrokConfigV2.getNgrokPath().getParent().toString(),
            "2",
            NgrokInstaller.getNgrokBin());
        final Path configPath2 = Path.of(javaNgrokConfigV2.getConfigPath().getParent().toString(),
            "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
            .withNgrokPath(ngrokPath2)
            .withConfigPath(configPath2)
            .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(), Map.of("web_addr",
            ngrokProcessV2.getApiUrl().replace("http://", "")), javaNgrokConfig2.getNgrokVersion());
        Thread.sleep(3000);

        // WHEN
        ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokException exception = assertThrows(NgrokException.class, ngrokProcessV2_2::start);

        // THEN
        assertNotNull(exception);
        assertNotNull(exception.getNgrokError());
        if (NgrokInstaller.getSystem().equals(WINDOWS)) {
            assertThat(exception.getMessage(), containsString("bind: Only one usage of each socket address"));
            assertThat(exception.getNgrokError(), containsString("bind: Only one usage of each socket address"));
        } else {
            assertThat(exception.getMessage(), containsString("bind: address already in use"));
            assertThat(exception.getNgrokError(), containsString("bind: address already in use"));
        }
        assertThat(exception.getNgrokLogs().size(), greaterThan(0));
        assertFalse(ngrokProcessV2_2.isRunning());
    }

    @Test
    public void testStartPortInUseV3()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        assertFalse(ngrokProcessV3.isRunning());
        ngrokProcessV3.start();
        assertTrue(ngrokProcessV3.isRunning());
        final Path ngrokPath2 = Path.of(javaNgrokConfigV3.getNgrokPath().getParent().toString(),
            "2",
            NgrokInstaller.getNgrokBin());
        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withNgrokPath(ngrokPath2)
            .withConfigPath(configPath2)
            .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(),
            Map.of("web_addr", ngrokProcessV3.getApiUrl().replace("http://", "")), javaNgrokConfigV3.getNgrokVersion());
        Thread.sleep(3000);

        // WHEN

        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokException exception = assertThrows(NgrokException.class, ngrokProcessV3_2::start);

        // THEN
        assertNotNull(exception);
        assertNotNull(exception.getNgrokError());
        if (NgrokInstaller.getSystem().equals(WINDOWS)) {
            assertThat(exception.getMessage(), containsString("bind: Only one usage of each socket address"));
            assertThat(exception.getNgrokError(), containsString("bind: Only one usage of each socket address"));
        } else {
            assertThat(exception.getMessage(), containsString("bind: address already in use"));
            assertThat(exception.getNgrokError(), containsString("bind: address already in use"));
        }
        assertThat(exception.getNgrokLogs().size(), greaterThan(0));
        assertFalse(ngrokProcessV3_2.isRunning());
    }

    @Test
    public void testExternalKill()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokProcessV3.start();
        assertTrue(ngrokProcessV3.isRunning());
        final ProcessHandle processHandle = ProcessHandle.allProcesses()
                                                         .filter(p -> p.info()
                                                                       .command()
                                                                       .orElse("")
                                                                       .contains(
                                                                           javaNgrokConfigV3.getNgrokPath().toString()))
                                                         .findFirst().orElse(null);
        assertNotNull(processHandle);

        // THEN Kill the process by external means, java-ngrok will clean up the state
        processHandle.destroy();
        long timeoutTime = System.currentTimeMillis() + 10 * 1000;
        while (processHandle.isAlive() && ngrokProcessV3.isRunning() && System.currentTimeMillis() < timeoutTime) {
            Thread.sleep(50);
        }
        assertFalse(processHandle.isAlive());
        assertFalse(ngrokProcessV3.isRunning());

        // THEN test we can successfully restart the process
        ngrokProcessV3.start();
        assertTrue(ngrokProcessV3.isRunning());
    }

    @Test
    public void testMultipleProcessesDifferentBinaries() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokInstaller.installDefaultConfig(javaNgrokConfigV2.getConfigPath(),
            Map.of("web_addr", "localhost:4040"), javaNgrokConfigV2.getNgrokVersion());
        final Path ngrokPathV2_2 = Path.of(javaNgrokConfigV2.getNgrokPath().getParent().toString(),
            "2",
            NgrokInstaller.getNgrokBin());
        final Path configPathV2_2 = Path.of(javaNgrokConfigV2.getConfigPath().getParent().toString(),
            "configV2_2.yml");
        final JavaNgrokConfig javaNgrokConfigV2_2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
            .withNgrokPath(ngrokPathV2_2)
            .withConfigPath(configPathV2_2)
            .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfigV2_2.getConfigPath(), Map.of("web_addr", "localhost:4041"),
            javaNgrokConfigV2.getNgrokVersion());
        ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfigV2_2, ngrokInstaller);

        ngrokInstaller.installDefaultConfig(javaNgrokConfigV3.getConfigPath(), Map.of("web_addr", "localhost:4042"),
            javaNgrokConfigV3.getNgrokVersion());
        final Path ngrokPath2 = Path.of(javaNgrokConfigV3.getNgrokPath().getParent().toString(),
            "2",
            NgrokInstaller.getNgrokBin());
        final Path configPath2 = Path.of(javaNgrokConfigV3.getConfigPath().getParent().toString(),
            "configV3_2.yml");
        final JavaNgrokConfig javaNgrokConfigV3_2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withNgrokPath(ngrokPath2)
            .withConfigPath(configPath2)
            .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfigV3_2.getConfigPath(),
            Map.of("web_addr", "localhost:4043"), javaNgrokConfigV3.getNgrokVersion());
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfigV3_2, ngrokInstaller);

        // WHEN
        ngrokProcessV2.start();
        ngrokProcessV2_2.start();
        ngrokProcessV3.start();
        ngrokProcessV3_2.start();

        // THEN
        assertTrue(ngrokProcessV2.isRunning());
        assertEquals("http://localhost:4040", ngrokProcessV2.getApiUrl());
        assertTrue(ngrokProcessV2_2.isRunning());
        assertEquals("http://localhost:4041", ngrokProcessV2_2.getApiUrl());
        assertTrue(ngrokProcessV3.isRunning());
        assertEquals("http://localhost:4042", ngrokProcessV3.getApiUrl());
        assertTrue(ngrokProcessV3_2.isRunning());
        assertEquals("http://localhost:4043", ngrokProcessV3_2.getApiUrl());
    }

    @Test
    public void testProcessLogs() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // WHEN
        ngrokProcessV3.start();

        // THEN
        int i = 0;
        for (final NgrokLog log : ngrokProcessV3.getLogs()) {
            assertNotNull(log.getT());
            assertNotNull(log.getLvl());
            assertNotNull(log.getMsg());
            ++i;
        }
        assertTrue(i > 0);
    }

    @Test
    public void testLogEventCallbackAndMaxLogs()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final Function<NgrokLog, Void> logEventCallbackMock = mock(Function.class);
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withLogEventCallback(logEventCallbackMock)
            .withMaxLogs(5)
            .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);

        // WHEN
        ngrokProcessV3_2.start();
        Thread.sleep(1000);

        // THEN
        assertThat(Mockito.mockingDetails(logEventCallbackMock).getInvocations().size(),
            greaterThan(ngrokProcessV3_2.getLogs().size()));
        assertEquals(5, ngrokProcessV3_2.getLogs().size());
    }

    @Test
    public void testNoMonitorThread()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withoutMonitoring()
            .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);

        // WHEN
        ngrokProcessV3_2.start();
        Thread.sleep(1000);

        // THEN
        assertTrue(ngrokProcessV3_2.isRunning());
        assertFalse(ngrokProcessV3_2.getProcessMonitor().isMonitoring());
    }

    @Test
    public void testStartProcessNoBinary()
        throws IOException, InterruptedException {
        // Due to Windows file locking behavior, wait a beat
        if (NgrokInstaller.getSystem().equals(WINDOWS)) {
            Thread.sleep(1000);
        }

        // GIVEN
        if (Files.exists(javaNgrokConfigV3.getNgrokPath())) {
            Files.delete(javaNgrokConfigV3.getNgrokPath());
        }

        // WHEN
        final NgrokException exception = assertThrows(NgrokException.class, ngrokProcessV3::start);

        // THEN
        assertThat(exception.getMessage(), containsString("ngrok binary was not found"));
        assertFalse(ngrokProcessV3.isRunning());
    }
}
