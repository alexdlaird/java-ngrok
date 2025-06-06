/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.conf;

import com.github.alexdlaird.ngrok.TestCase;
import com.github.alexdlaird.ngrok.installer.ConfigVersion;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.process.NgrokLog;
import com.github.alexdlaird.ngrok.protocol.Region;
import java.nio.file.Path;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SetEnvironmentVariable(key = "NGROK_AUTHTOKEN", value = "some-auth-token")
@SetEnvironmentVariable(key = "NGROK_API_KEY", value = "some-api-key")
public class JavaNgrokConfigTest extends TestCase {
    @Test
    public void testJavaNgrokConfig() {
        // GIVEN
        final Path ngrokPath = Path.of("custom-ngrok");
        final Path configPath = Path.of("custom-config");
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
            .withConfigVersion(ConfigVersion.V2)
            .build();

        // THEN
        assertEquals(ngrokPath, javaNgrokConfig.getNgrokPath());
        assertEquals(configPath, javaNgrokConfig.getConfigPath());
        assertEquals("auth-token", javaNgrokConfig.getAuthToken());
        assertEquals(Region.EU, javaNgrokConfig.getRegion());
        assertFalse(javaNgrokConfig.isKeepMonitoring());
        assertEquals(50, javaNgrokConfig.getMaxLogs());
        assertEquals(logEventCallback, javaNgrokConfig.getLogEventCallback());
        assertEquals(5, javaNgrokConfig.getStartupTimeout());
        assertEquals(NgrokVersion.V2, javaNgrokConfig.getNgrokVersion());
        assertEquals("api-key", javaNgrokConfig.getApiKey());
        assertEquals(ConfigVersion.V2, javaNgrokConfig.getConfigVersion());
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
        // WHEN
        final JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
            .build();

        // THEN
        assertEquals("some-auth-token", javaNgrokConfig.getAuthToken());
    }

    @Test
    public void testApiKeySetFromEnv() {
        // WHEN
        final JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
            .build();

        // THEN
        assertEquals("some-api-key", javaNgrokConfig.getApiKey());
    }
}
