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

package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.WINDOWS;
import static com.github.alexdlaird.ngrok.installer.NgrokInstaller.getNgrokBin;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class NgrokTestCase {

    protected final JavaNgrokConfig javaNgrokConfigV2 = new JavaNgrokConfig.Builder()
            .withConfigPath(Paths.get("build", ".ngrok2", "config_v2.yml").toAbsolutePath())
            .withNgrokPath(Paths.get("build", "bin", "v2", getNgrokBin()))
            .withNgrokVersion(NgrokVersion.V2)
            .build();

    protected final JavaNgrokConfig javaNgrokConfigV3 = new JavaNgrokConfig.Builder()
            .withConfigPath(Paths.get("build", ".ngrok2", "config_v3.yml").toAbsolutePath())
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
                    } catch (IOException e) {
                        throw new JavaNgrokException(String.format("An error occurred cleaning up file %s when testing.", path));
                    }
                });

        for (final Map.Entry<String, String> entry : mockedSystemProperties.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
        mockedSystemProperties.clear();
    }

    protected void givenNgrokNotInstalled(final JavaNgrokConfig javaNgrokConfig) throws InterruptedException, IOException {
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
        Random random = new Random();
        return String.format("java-ngrok-%s-%s-%s-tcp", random.longs(1000000000000000L, 9999999999999999L).findFirst().getAsLong(), System.getProperty("java.version").replaceAll("\\.", ""), NgrokInstaller.getSystem().toLowerCase());
    }

    protected void mockSystemProperty(final String key, final String value) {
        mockedSystemProperties.put(key, System.getProperty(key));

        System.setProperty(key, value);
    }
}
