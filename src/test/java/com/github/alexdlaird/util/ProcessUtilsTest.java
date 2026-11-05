/*
 * Copyright (c) 2021-2026 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcessUtilsTest {
    @Test
    public void testDrainInBackgroundLetsProcessWritePastPipeBuffer(@TempDir final Path tempDir)
        throws IOException, InterruptedException {
        // GIVEN
        final Process process = startProcessWritingHalfAMegabyte(tempDir);
        final BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        // WHEN
        final Thread thread = ProcessUtils.drainInBackground(reader);

        // THEN
        assertTrue(thread.isDaemon(), "Draining must not keep the JVM alive");
        assertTrue(process.waitFor(60, TimeUnit.SECONDS),
            "The process blocked writing to a pipe that nothing was reading");
    }

    @Test
    public void testUndrainedProcessBlocksOnFullPipe(@TempDir final Path tempDir)
        throws IOException, InterruptedException {
        // GIVEN
        final Process process = startProcessWritingHalfAMegabyte(tempDir);

        try {
            // WHEN
            final boolean exited = process.waitFor(10, TimeUnit.SECONDS);

            // THEN
            assertFalse(exited, "Without draining, a process blocks once its output pipe is full");
        } finally {
            process.destroyForcibly();
        }
    }

    private static Process startProcessWritingHalfAMegabyte(final Path tempDir)
        throws IOException {
        final Path source = tempDir.resolve("Writer.java");
        Files.writeString(source, "public class Writer { public static void main(String[] args) { "
                                  + "String line = \"x\".repeat(127); "
                                  + "for (int i = 0; i < 4096; i++) { System.out.println(line); } } }");
        final String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();

        return new ProcessBuilder(java, source.toString()).redirectErrorStream(true).start();
    }
}
