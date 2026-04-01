/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.WINDOWS;
import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.getNgrokBin;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class NgrokTestCase extends TestCase {

    protected final JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
        .withConfigPath(Path.of("build", ".ngrok", "config.yml").toAbsolutePath())
        .withNgrokPath(Path.of("build", "bin", "v3", getNgrokBin()))
        .build();

    protected final HttpClient retryHttpClient = new DefaultHttpClient.Builder()
        .withTimeout(10000)
        .withRetryCount(3)
        .build();

    protected final NgrokInstaller ngrokInstaller = new NgrokInstaller(retryHttpClient);

    protected NgrokProcess ngrokProcess;

    protected NgrokProcess ngrokProcess2;

    private final Map<String, String> mockedSystemProperties = new HashMap<>();

    private final Random random = new Random();

    protected final Gson gson = new Gson();

    protected final String testResourceDescription = "Created by java-ngrok testcase";

    @BeforeEach
    public void setUp() {
        ngrokProcess = new NgrokProcess(javaNgrokConfig, ngrokInstaller);
    }

    @AfterEach
    public void tearDown()
        throws IOException {
        ngrokProcess.stop();

        if (nonNull(ngrokProcess2)) {
            ngrokProcess2.stop();
        }

        Files.walk(javaNgrokConfig.getConfigPath().getParent())
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

    protected String generateNameForSubdomain() {
        return String.format("java-ngrok-temp-%s", random.nextInt(2000000000 - (1000000001)) + 1000000000);
    }

    protected void mockSystemProperty(final String key, final String value) {
        mockedSystemProperties.put(key, System.getProperty(key));

        System.setProperty(key, value);
    }
}
