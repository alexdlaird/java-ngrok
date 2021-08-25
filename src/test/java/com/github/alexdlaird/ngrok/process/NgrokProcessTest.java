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
import java.util.Collections;
import java.util.function.Function;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.WINDOWS;
import static java.util.Objects.isNull;
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
        // GIVEN
        assertFalse(ngrokProcess.isRunning());

        // WHEN
        ngrokProcess.start();

        // THEN
        assertTrue(ngrokProcess.isRunning());
    }

    @Test
    public void testStartPortInUse() throws InterruptedException {
        // GIVEN
        assertFalse(ngrokProcess.isRunning());
        ngrokProcess.start();
        assertTrue(ngrokProcess.isRunning());
        final Path ngrokPath2 = Paths.get(javaNgrokConfig.getNgrokPath().getParent().toString(), "2", NgrokInstaller.getNgrokBin());
        final Path configPath2 = Paths.get(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
                .withNgrokPath(ngrokPath2)
                .withConfigPath(configPath2)
                .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(), Collections.singletonMap("web_addr", ngrokProcess.getApiUrl().substring(7)));

        // WHEN
        NgrokException exception = null;
        String error = null;
        for (int i = 0; isNull(error) && i < 10; ++i) {
            Thread.sleep(1000);

            ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
            exception = assertThrows(NgrokException.class, ngrokProcess2::start);
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
        assertFalse(ngrokProcess2.isRunning());
    }

    @Test
    public void testMultipleProcessesDifferentBinaries() {
        // GIVEN
        ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Collections.singletonMap("web_addr", "localhost:4040"));
        final Path ngrokPath2 = Paths.get(javaNgrokConfig.getNgrokPath().getParent().toString(), "2", NgrokInstaller.getNgrokBin());
        final Path configPath2 = Paths.get(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
                .withNgrokPath(ngrokPath2)
                .withConfigPath(configPath2)
                .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(), Collections.singletonMap("web_addr", "localhost:4041"));
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
        // WHEN
        ngrokProcess.start();

        // THEN
        for (final NgrokLog log : ngrokProcess.getProcessMonitor().getLogs()) {
            assertNotNull(log.getT());
            assertNotNull(log.getLvl());
            assertNotNull(log.getMsg());
        }
    }

    @Test
    public void testLogEventCallbackAndMaxLogs() throws InterruptedException {
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
        assertThat(Mockito.mockingDetails(logEventCallbackMock).getInvocations().size(), greaterThan(ngrokProcess2.getProcessMonitor().getLogs().size()));
        assertEquals(5, ngrokProcess2.getProcessMonitor().getLogs().size());
    }

    @Test
    public void testNoMonitorThread() {
        // GIVEN
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
                .withoutMonitoring()
                .build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);

        // WHEN
        ngrokProcess2.start();

        // THEN
        assertTrue(ngrokProcess2.isRunning());
        assertFalse(ngrokProcess2.getProcessMonitor().isMonitoring());
    }

    @Test
    public void testStartNoBinary() throws IOException, InterruptedException {
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
