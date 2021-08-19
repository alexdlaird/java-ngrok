package com.github.alexdlaird.ngrok.process;

import com.github.alexdlaird.exception.NgrokException;
import com.github.alexdlaird.ngrok.NgrokTestCase;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.WINDOWS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NgrokProcessTest extends NgrokTestCase {
    // TODO: testStart()

    // TODO: testStop()

    @Test
    public void testStartPortInUse() {
        // GIVEN
        assertFalse(ngrokProcess.isRunning());
        ngrokProcess.start();
        assertTrue(ngrokProcess.isRunning());
        final Path ngrokPath2 = Paths.get(javaNgrokConfig.getNgrokPath().getParent().toString(), "2", NgrokInstaller.getNgrokBin());
        final Path configPath2 = Paths.get(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder()
                .withNgrokPath(ngrokPath2)
                .withConfigPath(configPath2)
                .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(), Map.of("web_addr", ngrokProcess.getApiUrl().substring(7)));

        // WHEN
        final NgrokProcess ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
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

    // TODO: testExternalKill()

    // TODO: testMultipleProcessesDifferentBinaries()

    // TODO: testMultipleProcessesSameBinaryFails()

    // TODO: testProcessLogs()

    // TODO: testLogEventCallbackAndMaxLogs()

    // TODO: testNoMonitorThread()

    // TODO: testStartNoBinary()
}
