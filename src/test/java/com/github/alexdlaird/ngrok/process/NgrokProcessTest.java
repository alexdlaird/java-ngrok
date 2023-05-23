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

package com.github.alexdlaird.ngrok.process;

import com.github.alexdlaird.exception.NgrokException;
import com.github.alexdlaird.ngrok.NgrokTestCase;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.WINDOWS;
import static com.github.alexdlaird.util.StringUtils.isNotBlank;
import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

public class NgrokProcessTest extends NgrokTestCase {

    @Test
    public void testStart() {
        // GIVEN
        assertFalse(ngrokProcessV3.isRunning());

        // WHEN
        ngrokProcessV3.start();

        // THEN
        assertTrue(ngrokProcessV3.isRunning());
    }

    @Test
    public void testStop() {
        // GIVEN
        ngrokProcessV3.start();

        // WHEN
        ngrokProcessV3.stop();

        // THEN
        assertFalse(ngrokProcessV3.isRunning());
    }

    @Test
    public void testStartPortInUseV2() throws InterruptedException {
        // GIVEN
        assertFalse(ngrokProcessV2.isRunning());
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        ngrokProcessV2.setAuthToken(ngrokAuthToken);
        ngrokProcessV2.start();
        assertTrue(ngrokProcessV2.isRunning());
        final Path ngrokPath2 = Paths.get(javaNgrokConfigV2.getNgrokPath().getParent().toString(), "2", NgrokInstaller.getNgrokBin());
        final Path configPath2 = Paths.get(javaNgrokConfigV2.getConfigPath().getParent().toString(), "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
                .withNgrokPath(ngrokPath2)
                .withConfigPath(configPath2)
                .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(), Map.of("web_addr", ngrokProcessV2.getApiUrl().substring(7)), javaNgrokConfig2.getNgrokVersion());

        // WHEN
        NgrokException exception = null;
        String error = null;
        for (int i = 0; isNull(error) && i < 10; ++i) {
            Thread.sleep(1000);

            ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
            exception = assertThrows(NgrokException.class, ngrokProcessV2_2::start);
            error = exception.getNgrokError();
        }

        // THEN
        assertNotNull(exception);
        assertNotNull(error);
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
    public void testStartPortInUseV3() throws InterruptedException {
        // GIVEN
        assertFalse(ngrokProcessV3.isRunning());
        ngrokProcessV3.start();
        assertTrue(ngrokProcessV3.isRunning());
        final Path ngrokPath2 = Paths.get(javaNgrokConfigV3.getNgrokPath().getParent().toString(), "2", NgrokInstaller.getNgrokBin());
        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
                .withNgrokPath(ngrokPath2)
                .withConfigPath(configPath2)
                .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(), Map.of("web_addr", ngrokProcessV3.getApiUrl().substring(7)), javaNgrokConfigV3.getNgrokVersion());

        // WHEN
        NgrokException exception = null;
        String error = null;
        for (int i = 0; isNull(error) && i < 10; ++i) {
            Thread.sleep(1000);

            ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
            exception = assertThrows(NgrokException.class, ngrokProcessV3_2::start);
            error = exception.getNgrokError();
        }

        // THEN
        assertNotNull(exception);
        assertNotNull(error);
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
    public void testExternalKill() throws InterruptedException {
        // GIVEN
        ngrokProcessV3.start();
        assertTrue(ngrokProcessV3.isRunning());

        // WHEN
        final ProcessHandle processHandle = ProcessHandle.allProcesses()
                .filter(p -> p.info().command().orElse("").contains(javaNgrokConfigV3.getNgrokPath().toString()))
                .findFirst().orElse(null);

        // THEN
        assertNotNull(processHandle);
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
        // GIVEN
        ngrokInstaller.installDefaultConfig(javaNgrokConfigV2.getConfigPath(), Map.of("web_addr", "localhost:4040"), javaNgrokConfigV2.getNgrokVersion());
        final Path ngrokPathV2_2 = Paths.get(javaNgrokConfigV2.getNgrokPath().getParent().toString(), "2", NgrokInstaller.getNgrokBin());
        final Path configPathV2_2 = Paths.get(javaNgrokConfigV2.getConfigPath().getParent().toString(), "configV2_2.yml");
        final JavaNgrokConfig javaNgrokConfigV2_2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
                .withNgrokPath(ngrokPathV2_2)
                .withConfigPath(configPathV2_2)
                .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfigV2_2.getConfigPath(), Map.of("web_addr", "localhost:4041"), javaNgrokConfigV2.getNgrokVersion());
        ngrokProcessV2_2 = new NgrokProcess(javaNgrokConfigV2_2, ngrokInstaller);

        ngrokInstaller.installDefaultConfig(javaNgrokConfigV3.getConfigPath(), Map.of("web_addr", "localhost:4042"), javaNgrokConfigV3.getNgrokVersion());
        final Path ngrokPath2 = Paths.get(javaNgrokConfigV3.getNgrokPath().getParent().toString(), "2", NgrokInstaller.getNgrokBin());
        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "configV3_2.yml");
        final JavaNgrokConfig javaNgrokConfigV3_2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
                .withNgrokPath(ngrokPath2)
                .withConfigPath(configPath2)
                .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfigV3_2.getConfigPath(), Map.of("web_addr", "localhost:4043"), javaNgrokConfigV3.getNgrokVersion());
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfigV3_2, ngrokInstaller);

        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");
        ngrokProcessV2.setAuthToken(ngrokAuthToken);
        ngrokProcessV2_2.setAuthToken(ngrokAuthToken);

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
        // WHEN
        ngrokProcessV3.start();

        // THEN
        int i = 0;
        for (final NgrokLog log : ngrokProcessV3.getProcessMonitor().getLogs()) {
            assertNotNull(log.getT());
            assertNotNull(log.getLvl());
            assertNotNull(log.getMsg());
            ++i;
        }
        assertTrue(i > 0);
    }

    @Test
    public void testLogEventCallbackAndMaxLogs() throws InterruptedException {
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
        assertThat(Mockito.mockingDetails(logEventCallbackMock).getInvocations().size(), greaterThan(ngrokProcessV3_2.getProcessMonitor().getLogs().size()));
        assertEquals(5, ngrokProcessV3_2.getProcessMonitor().getLogs().size());
    }

    @Test
    public void testNoMonitorThread() {
        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
                .withoutMonitoring()
                .build();
        ngrokProcessV3_2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);

        // WHEN
        ngrokProcessV3_2.start();

        // THEN
        assertTrue(ngrokProcessV3_2.isRunning());
        assertFalse(ngrokProcessV3_2.getProcessMonitor().isMonitoring());
    }

    @Test
    public void testStartNoBinary() throws IOException, InterruptedException {
        // Due to Windows file locking behavior, wait a beat
        if (NgrokInstaller.getSystem().equals(WINDOWS)) {
            Thread.sleep(1000);
        }

        // GIVEN
        if (Files.exists(javaNgrokConfigV2.getNgrokPath())) {
            Files.delete(javaNgrokConfigV2.getNgrokPath());
        }

        // WHEN
        final NgrokException exception = assertThrows(NgrokException.class, ngrokProcessV2::start);

        // THEN
        assertThat(exception.getMessage(), containsString("ngrok binary was not found"));
        assertFalse(ngrokProcessV2.isRunning());
    }
}
