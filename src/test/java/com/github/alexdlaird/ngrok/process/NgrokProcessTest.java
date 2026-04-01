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
        assertFalse(ngrokProcess.isRunning());

        // WHEN
        ngrokProcess.start();

        // THEN
        assertTrue(ngrokProcess.isRunning());
    }

    @Test
    public void testStop()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokProcess.start();

        // WHEN
        ngrokProcess.stop();
        Thread.sleep(1000);

        // THEN
        assertFalse(ngrokProcess.isRunning());
    }

    @Test
    public void testStartPortInUse()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        assertFalse(ngrokProcess.isRunning());
        ngrokProcess.start();
        assertTrue(ngrokProcess.isRunning());
        final Path ngrokPath2 = Path.of(javaNgrokConfig.getNgrokPath().getParent().toString(),
            "2",
            NgrokInstaller.getNgrokBin());
        final Path configPath2 = Path.of(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
            .withNgrokPath(ngrokPath2)
            .withConfigPath(configPath2)
            .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(),
            Map.of("web_addr", ngrokProcess.getApiUrl().replace("http://", "")), javaNgrokConfig.getNgrokVersion());
        Thread.sleep(3000);

        // WHEN

        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokException exception = assertThrows(NgrokException.class, ngrokProcess2::start);

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
        assertFalse(ngrokProcess2.isRunning());
    }

    @Test
    public void testExternalKill()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokProcess.start();
        assertTrue(ngrokProcess.isRunning());
        final ProcessHandle processHandle = ProcessHandle.allProcesses()
                                                         .filter(p -> p.info()
                                                                       .command()
                                                                       .orElse("")
                                                                       .contains(
                                                                           javaNgrokConfig.getNgrokPath().toString()))
                                                         .findFirst().orElse(null);
        assertNotNull(processHandle);

        // THEN Kill the process by external means, java-ngrok will clean up the state
        processHandle.destroy();
        long timeoutTime = System.currentTimeMillis() + 10 * 1000;
        while (processHandle.isAlive() && ngrokProcess.isRunning() && System.currentTimeMillis() < timeoutTime) {
            Thread.sleep(50);
        }
        assertFalse(processHandle.isAlive());
        assertFalse(ngrokProcess.isRunning());

        // THEN test we can successfully restart the process
        ngrokProcess.start();
        assertTrue(ngrokProcess.isRunning());
    }

    @Test
    public void testMultipleProcessesDifferentBinaries() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Map.of("web_addr", "localhost:4040"),
            javaNgrokConfig.getNgrokVersion());
        final Path ngrokPath2 = Path.of(javaNgrokConfig.getNgrokPath().getParent().toString(),
            "2",
            NgrokInstaller.getNgrokBin());
        final Path configPath2 = Path.of(javaNgrokConfig.getConfigPath().getParent().toString(),
            "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
            .withNgrokPath(ngrokPath2)
            .withConfigPath(configPath2)
            .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(),
            Map.of("web_addr", "localhost:4041"), javaNgrokConfig.getNgrokVersion());
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);

        // WHEN
        ngrokProcess.start();
        ngrokProcess2.start();

        // THEN
        assertTrue(ngrokProcess.isRunning());
        assertEquals("http://localhost:4040", ngrokProcess.getApiUrl());
        assertTrue(ngrokProcess2.isRunning());
        assertEquals("http://localhost:4041", ngrokProcess2.getApiUrl());
    }

    @Test
    public void testProcessLogs() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // WHEN
        ngrokProcess.start();

        // THEN
        int i = 0;
        for (final NgrokLog log : ngrokProcess.getLogs()) {
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
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
            .withLogEventCallback(logEventCallbackMock)
            .withMaxLogs(5)
            .build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);

        // WHEN
        ngrokProcess2.start();
        Thread.sleep(1000);

        // THEN
        assertThat(Mockito.mockingDetails(logEventCallbackMock).getInvocations().size(),
            greaterThan(ngrokProcess2.getLogs().size()));
        assertEquals(5, ngrokProcess2.getLogs().size());
    }

    @Test
    public void testNoMonitorThread()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
            .withoutMonitoring()
            .build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);

        // WHEN
        ngrokProcess2.start();
        Thread.sleep(1000);

        // THEN
        assertTrue(ngrokProcess2.isRunning());
        assertFalse(ngrokProcess2.getProcessMonitor().isMonitoring());
    }

    @Test
    public void testStartProcessNoBinary()
        throws IOException, InterruptedException {
        // Due to Windows file locking behavior, wait a beat
        if (NgrokInstaller.getSystem().equals(WINDOWS)) {
            Thread.sleep(1000);
        }

        // GIVEN
        if (Files.exists(javaNgrokConfig.getNgrokPath())) {
            Files.delete(javaNgrokConfig.getNgrokPath());
        }

        // WHEN
        final NgrokException exception = assertThrows(NgrokException.class, ngrokProcess::start);

        // THEN
        assertThat(exception.getMessage(), containsString("ngrok binary was not found"));
        assertFalse(ngrokProcess.isRunning());
    }
}
