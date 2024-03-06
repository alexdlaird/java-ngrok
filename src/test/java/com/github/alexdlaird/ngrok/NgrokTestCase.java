/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.WINDOWS;
import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.getNgrokBin;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class NgrokTestCase {

    protected final JavaNgrokConfig javaNgrokConfigV2 = new JavaNgrokConfig.Builder()
            .withConfigPath(Paths.get("build", ".ngrok", "config_v2.yml").toAbsolutePath())
            .withNgrokPath(Paths.get("build", "bin", "v2", getNgrokBin()))
            .withNgrokVersion(NgrokVersion.V2)
            .build();

    protected final JavaNgrokConfig javaNgrokConfigV3 = new JavaNgrokConfig.Builder()
            .withConfigPath(Paths.get("build", ".ngrok", "config_v3.yml").toAbsolutePath())
            .withNgrokPath(Paths.get("build", "bin", "v3", getNgrokBin()))
            .withNgrokVersion(NgrokVersion.V3)
            .build();

    protected final NgrokInstaller ngrokInstaller = new NgrokInstaller(new DefaultHttpClient.Builder()
            .withRetryCount(3)
            .build());

    protected NgrokProcess ngrokProcessV2;

    protected NgrokProcess ngrokProcessV2_2;

    protected NgrokProcess ngrokProcessV3;

    protected NgrokProcess ngrokProcessV3_2;

    private final Map<String, String> mockedSystemProperties = new HashMap<>();

    @BeforeEach
    public void setUp() {
        ngrokProcessV2 = new NgrokProcess(javaNgrokConfigV2, ngrokInstaller);
        ngrokProcessV3 = new NgrokProcess(javaNgrokConfigV3, ngrokInstaller);
    }

    @AfterEach
    public void tearDown() throws IOException {
        ngrokProcessV2.stop();
        ngrokProcessV3.stop();

        if (nonNull(ngrokProcessV2_2)) {
            ngrokProcessV2_2.stop();
        }
        if (nonNull(ngrokProcessV3_2)) {
            ngrokProcessV3_2.stop();
        }

        // This deletes all v2 and v3 configs
        Files.walk(javaNgrokConfigV2.getConfigPath().getParent())
                .sorted(Comparator.reverseOrder())
                .forEach((path) -> {
                    try {
                        Files.delete(path);
                    } catch (final IOException e) {
                        throw new JavaNgrokException(
                            String.format("An error occurred cleaning up file %s when testing.", path));
                    }
                });

        for (final Map.Entry<String, String> entry : mockedSystemProperties.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
        mockedSystemProperties.clear();
    }

    protected void givenNgrokNotInstalled(final JavaNgrokConfig javaNgrokConfig)
        throws InterruptedException, IOException {
        if (Files.exists(javaNgrokConfig.getNgrokPath())) {
            // Due to Windows file locking behavior, wait a beat
            if (NgrokInstaller.getSystem().equals(WINDOWS)) {
                Thread.sleep(1000);
            }
            Files.delete(javaNgrokConfig.getNgrokPath());
        }
        assertFalse(Files.exists(javaNgrokConfig.getNgrokPath()));
    }

    protected String createUniqueSubdomain() {
        final Random random = new Random();
        return String.format("java-ngrok-%s-%s-%s-tcp",
            random.longs(1000000000000000L, 9999999999999999L)
                .findFirst()
                .getAsLong(),
            System.getProperty("java.version").replaceAll("\\.|_", ""), NgrokInstaller.getSystem().toLowerCase());
    }

    protected void mockSystemProperty(final String key, final String value) {
        mockedSystemProperties.put(key, System.getProperty(key));

        System.setProperty(key, value);
    }
}
