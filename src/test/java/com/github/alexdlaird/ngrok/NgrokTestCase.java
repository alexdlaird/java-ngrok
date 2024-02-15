/*
 * Copyright (c) 2021 Alex Laird
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
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Random;
import java.util.UUID;

public class NgrokTestCase {

    protected JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
            .withConfigPath(Paths.get("build", ".ngrok2", "config.yml").toAbsolutePath())
            .build();

    protected NgrokInstaller ngrokInstaller = new NgrokInstaller();

    protected NgrokProcess ngrokProcess;

    protected NgrokProcess ngrokProcess2;

    @BeforeEach
    public void setUp() {
        ngrokProcess = new NgrokProcess(javaNgrokConfig, ngrokInstaller);
    }

    @AfterEach
    public void tearDown() throws IOException, InterruptedException {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("killall", "9", "ngrok");
        processBuilder.start().waitFor();

        Files.walk(javaNgrokConfig.getConfigPath().getParent())
                .sorted(Comparator.reverseOrder())
                .forEach((path) -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new JavaNgrokException(String.format("An error occurred cleaning up file %s when testing.", path));
                    }
                });
    }

    protected String createUniqueSubdomain() {
        Random random = new Random();
        return String.format("java-ngrok-%s-%s-%s-tcp", random.longs(1000000000000000L, 9999999999999999L).findFirst().getAsLong(), System.getProperty("java.version").replaceAll("\\.", ""), NgrokInstaller.getSystem().toLowerCase());
    }
}
