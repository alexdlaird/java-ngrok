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
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.WINDOWS;
import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.getNgrokBin;
import static com.github.alexdlaird.util.ProcessUtils.captureRunProcess;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class NgrokTestCase extends TestCase {

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

    protected final HttpClient retryHttpClient = new DefaultHttpClient.Builder()
        .withTimeout(10000)
        .withRetryCount(3)
        .build();

    protected final NgrokInstaller ngrokInstaller = new NgrokInstaller(retryHttpClient);

    protected NgrokProcess ngrokProcessV2;

    protected NgrokProcess ngrokProcessV2_2;

    protected NgrokProcess ngrokProcessV3;

    protected NgrokProcess ngrokProcessV3_2;

    private final Map<String, String> mockedSystemProperties = new HashMap<>();

    private final Random random = new Random();

    protected final Gson gson = new Gson();

    protected final String testResourceDescription = "Created by java-ngrok testcase";

    @BeforeEach
    public void setUp() {
        ngrokProcessV2 = new NgrokProcess(javaNgrokConfigV2, ngrokInstaller);
        ngrokProcessV3 = new NgrokProcess(javaNgrokConfigV3, ngrokInstaller);
    }

    @AfterEach
    public void tearDown()
        throws IOException {
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

    protected Map<String, String> reserveNgrokDomain(final JavaNgrokConfig javaNgrokConfig,
                                                     final String domain)
        throws IOException, InterruptedException {
        final List<String> args = List.of("--config", javaNgrokConfig.getConfigPath().toString(),
            "api", "reserved-domains", "create",
            "--domain", domain,
            "--description", testResourceDescription);

        final String result = captureRunProcess(javaNgrokConfig.getNgrokPath(), args);
        return gson.fromJson(result.substring(result.indexOf("{")), Map.class);
    }

    protected Map<String, String> reserveNgrokAddr(final JavaNgrokConfig javaNgrokConfig)
        throws IOException, InterruptedException {
        final List<String> args = List.of("--config", javaNgrokConfig.getConfigPath().toString(),
            "api", "reserved-addrs", "create",
            "--description", testResourceDescription);

        final String result = captureRunProcess(javaNgrokConfig.getNgrokPath(), args);
        return gson.fromJson(result.substring(result.indexOf("{")), Map.class);
    }

    protected Map<String, String> createNgrokEdge(final JavaNgrokConfig javaNgrokConfig,
                                                  final String proto,
                                                  final String domain,
                                                  final int port)
        throws IOException, InterruptedException {
        final List<String> args = List.of("--config", javaNgrokConfig.getConfigPath().toString(),
            "api", "edges", proto, "create",
            "--hostports", String.format("%s:%s", domain, port),
            "--description", testResourceDescription);

        final String result = captureRunProcess(javaNgrokConfig.getNgrokPath(), args);
        return gson.fromJson(result.substring(result.indexOf("{")), Map.class);
    }

    protected String generateNameForSubdomain() {
        return String.format("java-ngrok-%s", random.nextInt(2000000000 - (1000000001)) + 1000000000);
    }

    protected void mockSystemProperty(final String key, final String value) {
        mockedSystemProperties.put(key, System.getProperty(key));

        System.setProperty(key, value);
    }
}
