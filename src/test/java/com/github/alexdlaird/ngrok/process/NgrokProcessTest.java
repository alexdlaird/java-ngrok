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
    public void testStop() {
        // GIVEN
        ngrokProcess.start();

        // WHEN
        ngrokProcess.stop();

        // THEN
        assertFalse(ngrokProcess.isRunning());
    }

    @Test
    public void testStartPortInUse() {
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
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(), Map.of("web_addr", ngrokProcess.getApiUrl().substring(7)));

        // WHEN
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokException exception = assertThrows(NgrokException.class, ngrokProcess2::start);

        // THEN
        if (NgrokInstaller.getSystem().equals(WINDOWS)) {
            assertTrue(exception.getMessage().contains("bind: Only one usage of each socket address"));
            assertTrue(exception.getNgrokError().contains("bind: Only one usage of each socket address"));
        } else {
            assertTrue(exception.getMessage().contains("bind: address already in use"));
            assertTrue(exception.getNgrokError().contains("bind: address already in use"));
        }
        assertTrue(exception.getNgrokLogs().size() > 0);
        assertFalse(ngrokProcess2.isRunning());
    }

    @Test
    public void testExternalKill() throws InterruptedException {
        // GIVEN
        ngrokProcess.start();
        assertTrue(ngrokProcess.isRunning());

        // WHEN
        final ProcessHandle processHandle = ProcessHandle.allProcesses()
                .filter(p -> p.info().command().orElse("").contains(javaNgrokConfig.getNgrokPath().toString()))
                .findFirst().orElse(null);

        // THEN
        assertNotNull(processHandle);
        processHandle.destroy();
        long timeoutTime = System.currentTimeMillis() + 10 * 1000;
        while (processHandle.isAlive() && System.currentTimeMillis() < timeoutTime) {
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
        // GIVEN
        ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Map.of("web_addr", "localhost:4040"));
        final Path ngrokPath2 = Paths.get(javaNgrokConfig.getNgrokPath().getParent().toString(), "2", NgrokInstaller.getNgrokBin());
        final Path configPath2 = Paths.get(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig)
                .withNgrokPath(ngrokPath2)
                .withConfigPath(configPath2)
                .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(), Map.of("web_addr", "localhost:4041"));
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
        assertTrue(Mockito.mockingDetails(logEventCallbackMock).getInvocations().size() > ngrokProcess2.getProcessMonitor().getLogs().size());
        assertEquals(5, ngrokProcess2.getProcessMonitor().getLogs().size());
    }

    @Test
    public void testNoMonitorThread() throws InterruptedException {
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
    public void testStartNoBinary() throws IOException {
        // GIVEN
        Files.delete(javaNgrokConfig.getNgrokPath());

        // WHEN
        final NgrokException exception = assertThrows(NgrokException.class, ngrokProcess::start);

        // THEN
        assertTrue(exception.getMessage().contains("ngrok binary was not found"));
        assertFalse(ngrokProcess.isRunning());
    }
}
