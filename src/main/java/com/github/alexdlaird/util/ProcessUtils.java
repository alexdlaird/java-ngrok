package com.github.alexdlaird.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Objects.nonNull;

/**
 * Convenience methods for handling short-lived processes.
 */
public class ProcessUtils {
    /**
     * Execute the given process command and capture its output.
     *
     * @param command The command to execute.
     * @return The output of the executed command.
     * @throws InterruptedException The thread was interrupted during execution.
     * @throws IOException          An I/O exception occurred.
     */
    public static String captureRunProcess(List<String> command)
        throws InterruptedException, IOException {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);

        processBuilder.command(command);

        final Process process = processBuilder.start();
        process.waitFor();

        final BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        final String result = captureOutput(reader);
        reader.close();
        if (result.contains("ERROR:")) {
            throw new IOException("Process execution failed: " + result);
        }
        return result;
    }

    private static String captureOutput(final BufferedReader reader)
        throws IOException {
        final StringBuilder builder = new StringBuilder();

        String line;
        while (nonNull(line = reader.readLine())) {
            builder.append(line).append("\n");
        }

        return builder.toString().trim();
    }
}
