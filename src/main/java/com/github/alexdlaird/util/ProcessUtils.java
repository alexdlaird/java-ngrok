package com.github.alexdlaird.util;

import com.github.alexdlaird.exception.NgrokException;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.nonNull;

/**
 * Convenience methods for executing processes and capturing their output.
 */
public class ProcessUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessUtils.class);

    /**
     * Start a blocking <code>ngrok</code> process with the binary at the given path and the passed args. When the
     * process returns, so will this method, and the captured output from the process along with it.
     *
     * <p>This method is meant for invoking <code>ngrok</code> directly (for instance, for API requests) and is not
     * necessarily compatible with non-blocking API methods or interacting with active tunnels. For that, use
     * {@link NgrokProcess}.
     *
     * @param args The args to pass to <code>ngrok</code>.
     * @return The output from the process.
     * @throws NgrokException The <code>ngrok</code> process exited with an error.
     * @throws InterruptedException The thread was interrupted during execution.
     * @throws IOException          An I/O exception occurred.
     */
    public static String captureRunProcess(final Path ngrokPath, final List<String> args)
        throws InterruptedException, IOException {
        final List<String> command = new ArrayList<>();
        command.add(ngrokPath.toString());
        command.addAll(args);

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

        if (process.exitValue() != 0) {
            throw new NgrokException(String.format("The ngrok process exited with code %s: %s",
                process.exitValue(), result));
        }

        return result;
    }

    /**
     * Read and discard the rest of a process's output on a daemon thread, until the output ends or the reader is
     * closed. A process writing to a pipe that nothing reads blocks once the pipe's buffer is full.
     *
     * @param reader The reader for the process's output.
     * @return The thread doing the reading.
     */
    public static Thread drainInBackground(final BufferedReader reader) {
        final Thread thread = new Thread(() -> {
            try {
                reader.transferTo(Writer.nullWriter());
            } catch (final IOException e) {
                LOGGER.trace("Stopped draining process output, the reader was closed", e);
            }
        }, "java-ngrok-output-drain");
        thread.setDaemon(true);
        thread.start();

        return thread;
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
