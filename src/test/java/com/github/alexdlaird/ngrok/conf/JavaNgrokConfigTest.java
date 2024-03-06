/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.conf;

import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.process.NgrokLog;
import com.github.alexdlaird.ngrok.protocol.Region;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static com.github.alexdlaird.util.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class JavaNgrokConfigTest {
    @Test
    public void testJavaNgrokConfig() {
        // GIVEN
        final Path ngrokPath = Paths.get("custom-ngrok");
        final Path configPath = Paths.get("custom-config");
        final Function<NgrokLog, Void> logEventCallback = ngrokLog -> null;

        // WHEN
        final JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
                .withNgrokPath(ngrokPath)
                .withConfigPath(configPath)
                .withAuthToken("auth-token")
                .withRegion(Region.EU)
                .withoutMonitoring()
                .withMaxLogs(50)
                .withLogEventCallback(logEventCallback)
                .withStartupTimeout(5)
                .withNgrokVersion(NgrokVersion.V2)
                .withApiKey("api-key")
                .build();

        // THEN
        assertEquals(ngrokPath, javaNgrokConfig.getNgrokPath());
        assertEquals(configPath, javaNgrokConfig.getConfigPath());
        assertEquals("auth-token", javaNgrokConfig.getAuthToken());
        assertEquals(Region.EU, javaNgrokConfig.getRegion());
        assertFalse(javaNgrokConfig.isKeepMonitoring());
        assertEquals(50, javaNgrokConfig.getMaxLogs());
        assertEquals(logEventCallback, javaNgrokConfig.getLogEventCallback());
        assertEquals(5, javaNgrokConfig.getStartupTime());
        assertEquals(NgrokVersion.V2, javaNgrokConfig.getNgrokVersion());
        assertEquals("api-key", javaNgrokConfig.getApiKey());
    }

    @Test
    public void testJavaNgrokConfigWithInvalidMaxLogs() {
        // WHEN
        assertThrows(IllegalArgumentException.class, () -> new JavaNgrokConfig.Builder().withMaxLogs(0));
    }

    @Test
    public void testJavaNgrokConfigWithInvalidStartupTimeout() {
        // WHEN
        assertThrows(IllegalArgumentException.class, () -> new JavaNgrokConfig.Builder().withStartupTimeout(0));
    }

    @Test
    public void testAuthTokenSetFromEnv() {
        // GIVEN
        final String ngrokAuthToken = System.getenv("NGROK_AUTHTOKEN");
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // WHEN
        final JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
                .build();

        // THEN
        assertEquals(ngrokAuthToken, javaNgrokConfig.getAuthToken());
    }
}
