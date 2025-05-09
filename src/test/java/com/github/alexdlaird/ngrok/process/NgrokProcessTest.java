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
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
    public void testStartPortInUseV2()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        assertFalse(ngrokProcessV2.isRunning());
        ngrokProcessV2.start();
        assertTrue(ngrokProcessV2.isRunning());
        final Path ngrokPath2 = Paths.get(javaNgrokConfigV2.getNgrokPath().getParent().toString(),
            "2",
            NgrokInstaller.getNgrokBin());
        final Path configPath2 = Paths.get(javaNgrokConfigV2.getConfigPath().getParent().toString(),
            "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV2)
            .withNgrokPath(ngrokPath2)
            .withConfigPath(configPath2)
            .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(), Map.of("web_addr",
            ngrokProcessV2.getApiUrl().substring(7)), javaNgrokConfig2.getNgrokVersion());

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
    public void testStartPortInUseV3()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        assertFalse(ngrokProcessV3.isRunning());
        ngrokProcessV3.start();
        assertTrue(ngrokProcessV3.isRunning());
        final Path ngrokPath2 = Paths.get(javaNgrokConfigV3.getNgrokPath().getParent().toString(),
            "2",
            NgrokInstaller.getNgrokBin());
        final Path configPath2 = Paths.get(javaNgrokConfigV3.getConfigPath().getParent().toString(), "config2.yml");
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfigV3)
            .withNgrokPath(ngrokPath2)
            .withConfigPath(configPath2)
            .build();
        ngrokInstaller.installDefaultConfig(javaNgrokConfig2.getConfigPath(),
            Map.of("web_addr", ngrokProcessV3.getApiUrl().substring(7)), javaNgrokConfigV3.getNgrokVersion());

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
}
