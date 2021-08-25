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

package com.github.alexdlaird.ngrok.process;

import com.github.alexdlaird.exception.JavaNgrokSecurityException;
import com.github.alexdlaird.exception.NgrokException;
import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.http.Response;
import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.protocol.Tunnels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.alexdlaird.util.StringUtils.isBlank;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.logging.Level.SEVERE;

/**
 * An object containing information about the <code>ngrok</code> process.
 * Can be configured with {@link JavaNgrokConfig}.
 */
public class NgrokProcess {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(NgrokProcess.class));

    private final JavaNgrokConfig javaNgrokConfig;
    private final NgrokInstaller ngrokInstaller;
    private Process process;
    private ProcessMonitor processMonitor;

    /**
     * If <code>ngrok</code> is not already installed at {@link JavaNgrokConfig#getNgrokPath()}, the given
     * {@link NgrokInstaller} will install it. This will also provision a default <code>ngrok</code> config
     * at {@link JavaNgrokConfig#getConfigPath()}, if none exists.
     *
     * @param javaNgrokConfig The <code>java-ngrok</code> to use when interacting with the <code>ngrok</code> binary.
     * @param ngrokInstaller  The class used to download and install <code>ngrok</code>.
     */
    public NgrokProcess(final JavaNgrokConfig javaNgrokConfig,
                        final NgrokInstaller ngrokInstaller) {
        this.javaNgrokConfig = javaNgrokConfig;
        this.ngrokInstaller = ngrokInstaller;

        if (!Files.exists(javaNgrokConfig.getNgrokPath())) {
            ngrokInstaller.installNgrok(javaNgrokConfig.getNgrokPath());
        }
        if (!Files.exists(javaNgrokConfig.getConfigPath())) {
            ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Collections.emptyMap());
        }
    }

    /**
     * If not already running, start a <code>ngrok</code> process with no tunnels. This will start the
     * <code>ngrok</code> web interface, against which HTTP requests can be made to create, interact with, and
     * destroy tunnels.
     */
    public void start() {
        start(0);
    }

    private void start(final int retries) {
        if (isRunning()) {
            return;
        }

        if (!Files.exists(javaNgrokConfig.getNgrokPath())) {
            throw new NgrokException(String.format("ngrok binary was not found. " +
                    "Be sure to call \"NgrokInstaller.installNgrok()\" first for \"ngrokPath\": %s", javaNgrokConfig.getNgrokPath()));
        }
        ngrokInstaller.validateConfig(javaNgrokConfig.getConfigPath());

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);

        final List<String> command = new ArrayList<>();
        command.add(javaNgrokConfig.getNgrokPath().toString());
        command.add("start");
        command.add("--none");
        command.add("--log=stdout");

        if (nonNull(javaNgrokConfig.getConfigPath())) {
            LOGGER.info(String.format("Starting ngrok with config file: %s", javaNgrokConfig.getConfigPath()));
            command.add(String.format("--config=%s", javaNgrokConfig.getConfigPath().toString()));
        }
        if (nonNull(javaNgrokConfig.getAuthToken())) {
            LOGGER.info("Overriding default auth token");
            command.add(String.format("--authtoken=%s", javaNgrokConfig.getAuthToken()));
        }
        if (nonNull(javaNgrokConfig.getRegion())) {
            LOGGER.info(String.format("Starting ngrok in region: %s", javaNgrokConfig.getRegion()));
            command.add(String.format("--region=%s", javaNgrokConfig.getRegion()));
        }

        processBuilder.command(command);
        try {
            process = processBuilder.start();
            LOGGER.fine(String.format("ngrok process starting with PID: %s", process.pid()));

            processMonitor = new ProcessMonitor(process, javaNgrokConfig);
            new Thread(processMonitor).start();

            final Calendar timeout = Calendar.getInstance();
            timeout.add(Calendar.SECOND, javaNgrokConfig.getStartupTime());
            while (Calendar.getInstance().before(timeout)) {
                if (processMonitor.isHealthy()) {
                    LOGGER.info(String.format("ngrok process has started with API URL: %s", processMonitor.apiUrl));

                    processMonitor.startupError = null;

                    break;
                } else if (!process.isAlive()) {
                    break;
                }
            }

            if (!processMonitor.isHealthy()) {
                // If the process did not come up in a healthy state, clean up the state
                stop();

                if (nonNull(processMonitor.startupError)) {
                    throw new NgrokException(String.format("The ngrok process errored on start: %s.", processMonitor.startupError),
                            processMonitor.logs,
                            processMonitor.startupError);
                } else {
                    throw new NgrokException("The ngrok process was unable to start.", processMonitor.logs);
                }
            }
        } catch (IOException e) {
            throw new NgrokException("An error occurred while starting ngrok.", e);
        }
    }

    /**
     * Check if this object is currently managing a running <code>ngrok</code> process.
     */
    public boolean isRunning() {
        return nonNull(process) && process.isAlive();
    }

    /**
     * Terminate the <code>ngrok</code> processes, if running. This method will not block, it will
     * just issue a kill request.
     */
    public void stop() {
        if (!isRunning()) {
            LOGGER.info(String.format("\"ngrokPath\" %s is not running a process", javaNgrokConfig.getNgrokPath()));

            return;
        }

        LOGGER.info(String.format("Killing ngrok process: %s", process.pid()));

        processMonitor.stop();
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();

        process = null;
    }

    /**
     * Set the <code>ngrok</code> auth token in the config file, enabling authenticated features (for instance,
     * more concurrent tunnels, custom subdomains, etc.).
     *
     * <pre>
     * // Setting an auth token allows us to do things like open multiple tunnels at the same time
     * final NgrokClient ngrokClient = new NgrokClient.Builder().build();
     * ngrokClient.setAuthToken("<NGROK_AUTH_TOKEN>")
     *
     * // &lt;NgrokTunnel: "http://&lt;public_sub1&gt;.ngrok.io" -&gt; "http://localhost:80"&gt;
     * final Tunnel ngrokTunnel1 = ngrokClient.connect();
     * // &lt;NgrokTunnel: "http://&lt;public_sub2&gt;.ngrok.io" -&gt; "http://localhost:8000"&gt;
     * final CreateTunnel sshCreateTunnel = new CreateTunnel.Builder()
     *         .withAddr(8000)
     *         .build();
     * final Tunnel ngrokTunnel2 = ngrokClient.connect(createTunnel);
     * </pre>
     * <p>
     * The auth token can also be set in the {@link JavaNgrokConfig} that is passed to the {@link NgrokClient.Builder}.
     *
     * @param authToken The auth token.
     */
    public void setAuthToken(final String authToken) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);

        final List<String> command = new ArrayList<>();
        command.add(javaNgrokConfig.getNgrokPath().toString());
        command.add("authtoken");
        command.add(authToken);
        command.add("--log=stdout");

        if (nonNull(javaNgrokConfig.getConfigPath())) {
            command.add(String.format("--config=%s", javaNgrokConfig.getConfigPath().toString()));
        }

        LOGGER.info(String.format("Updating authtoken for \"configPath\": %s", javaNgrokConfig.getConfigPath()));

        processBuilder.command(command);
        try {
            final Process process = processBuilder.start();
            process.waitFor();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            final String result = captureOutput(reader);
            if (!result.contains("Authtoken saved")) {
                throw new NgrokException(String.format("An error occurred while setting the auth token: %s", result));
            }
        } catch (IOException | InterruptedException e) {
            throw new NgrokException("An error occurred while setting the auth token for ngrok.", e);
        }
    }

    private String captureOutput(final BufferedReader reader) throws IOException {
        final StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }

        return builder.toString().trim();
    }

    /**
     * Update <code>ngrok</code>, if an update is available.
     */
    public void update() {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);

        final List<String> command = List.of(javaNgrokConfig.getNgrokPath().toString(), "update", "--log=stdout");

        processBuilder.command(command);
        try {
            final Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new NgrokException("An error occurred while trying to update ngrok.", e);
        }
    }

    /**
     * Get the <code>ngrok</code> version.
     *
     * @return The version.
     */
    public String getVersion() {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);

        final List<String> command = List.of(javaNgrokConfig.getNgrokPath().toString(), "--version");

        processBuilder.command(command);
        try {
            final Process process = processBuilder.start();
            process.waitFor();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            final String result = captureOutput(reader);
            return result.split("version ")[1];
        } catch (IOException | InterruptedException | ArrayIndexOutOfBoundsException e) {
            throw new NgrokException("An error occurred while trying to update ngrok.", e);
        }
    }

    /**
     * Get the API URL for the <code>ngrok</code> web interface.
     */
    public String getApiUrl() {
        if (!isRunning() || !processMonitor.isHealthy()) {
            return null;
        }

        return processMonitor.apiUrl;
    }

    /**
     * Get the class used to download and install <code>ngrok</code>.
     */
    public NgrokInstaller getNgrokInstaller() {
        return ngrokInstaller;
    }

    /**
     * Get the Runnable that is monitoring the <code>ngrok</code> thread.
     */
    public ProcessMonitor getProcessMonitor() {
        return processMonitor;
    }

    public static class ProcessMonitor implements Runnable {
        private final Process process;
        private final JavaNgrokConfig javaNgrokConfig;
        private final HttpClient httpClient;
        private String apiUrl;
        private boolean tunnelStarted;
        private boolean clientConnected;
        private String startupError;

        private final List<NgrokLog> logs = new ArrayList<>();
        private boolean alive = true;

        public ProcessMonitor(final Process process,
                              final JavaNgrokConfig javaNgrokConfig) {
            this(process, javaNgrokConfig, new DefaultHttpClient.Builder().build());
        }

        protected ProcessMonitor(final Process process,
                                 final JavaNgrokConfig javaNgrokConfig,
                                 final HttpClient httpClient) {
            this.process = process;
            this.javaNgrokConfig = javaNgrokConfig;
            this.httpClient = httpClient;
        }

        @Override
        public void run() {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    logStartupLine(line);

                    if (isHealthy()) {
                        break;
                    } else if (nonNull(startupError)) {
                        alive = false;
                        break;
                    }
                }

                while (alive && process.isAlive() && javaNgrokConfig.isKeepMonitoring() && (line = reader.readLine()) != null) {
                    logLine(line);
                }

                alive = false;
            } catch (IOException e) {
                throw new NgrokException("An error occurred in the ngrok process.", e);
            }
        }

        /**
         * Get the <code>ngrok</code> logs.
         */
        public List<NgrokLog> getLogs() {
            return List.of(logs.toArray(new NgrokLog[]{}));
        }

        /**
         * Get whether the thread is continuing to monitor <code>ngrok</code> logs.
         */
        public boolean isMonitoring() {
            return alive;
        }

        private void stop() {
            this.alive = false;
        }

        private boolean isHealthy() {
            if (isNull(apiUrl) || !tunnelStarted || !clientConnected) {
                return false;
            }

            if (!apiUrl.toLowerCase().startsWith("http")) {
                throw new JavaNgrokSecurityException(String.format("URL must start with \"http\": %s", apiUrl));
            }

            final Response<Tunnels> tunnelsResponse = httpClient.get(String.format("%s/api/tunnels", apiUrl), Tunnels.class);
            if (tunnelsResponse.getStatusCode() != HTTP_OK) {
                return false;
            }

            return process.isAlive();
        }

        private void logStartupLine(final String line) {
            final NgrokLog ngrokLog = logLine(line);

            if (isNull(ngrokLog)) {
                return;
            }

            if (nonNull(ngrokLog.getLvl()) && ngrokLog.getLvl().equals(SEVERE.getName())) {
                this.startupError = ngrokLog.getErr();
            } else if (nonNull(ngrokLog.getMsg())) {
                // Log ngrok startup states as they come in
                if (ngrokLog.getMsg().contains("starting web service") && nonNull(ngrokLog.getAddr())) {
                    this.apiUrl = String.format("http://%s", ngrokLog.getAddr());
                } else if (ngrokLog.getMsg().contains("tunnel session started")) {
                    this.tunnelStarted = true;
                } else if (ngrokLog.getMsg().contains("client session established")) {
                    this.clientConnected = true;
                }
            }
        }

        private NgrokLog logLine(final String line) {
            final NgrokLog ngrokLog = new NgrokLog(line);

            if (isBlank(ngrokLog.getLine())) {
                return null;
            }

            LOGGER.log(Level.parse(ngrokLog.getLvl()), ngrokLog.getMsg());
            logs.add(ngrokLog);
            if (logs.size() > javaNgrokConfig.getMaxLogs()) {
                logs.remove(0);
            }

            if (nonNull(javaNgrokConfig.getLogEventCallback())) {
                javaNgrokConfig.getLogEventCallback().apply(ngrokLog);
            }

            return ngrokLog;
        }
    }
}
