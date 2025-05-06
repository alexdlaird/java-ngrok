/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.process;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.exception.JavaNgrokSecurityException;
import com.github.alexdlaird.exception.NgrokException;
import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.http.Response;
import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.alexdlaird.util.ProcessUtils.captureRunProcess;
import static com.github.alexdlaird.util.StringUtils.isBlank;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.logging.Level.SEVERE;

/**
 * An object containing information about the <code>ngrok</code> process. Can be configured with
 * {@link JavaNgrokConfig}.
 *
 * <h2>Basic Usage</h2>
 * Opening a tunnel will start the <code>ngrok</code> process. This process will remain alive, and the tunnels open,
 * until {@link NgrokProcess#stop()} is invoked, or until the Java process terminates.
 *
 * <h3>Event Logs</h3>
 * When <code>ngrok</code> emits logs, <code>java-ngrok</code> can surface them to a callback function. To register this
 * callback, use {@link JavaNgrokConfig.Builder#withLogEventCallback}.
 *
 * <p>If these events aren't necessary for our use case, some resources can be freed up by turning them off.
 * {@link JavaNgrokConfig.Builder#withoutMonitoring} will disable logging, or we can call
 * {@link NgrokProcess.ProcessMonitor#stop()} to stop monitoring on a running process.
 */
public class NgrokProcess {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(NgrokProcess.class));

    private final JavaNgrokConfig javaNgrokConfig;
    private final NgrokInstaller ngrokInstaller;

    private Process process;
    private ProcessMonitor processMonitor;

    /**
     * If <code>ngrok</code> is not already installed at {@link JavaNgrokConfig#getNgrokPath()}, the given
     * {@link NgrokInstaller} will install it. This will also provision a default <code>ngrok</code> config at
     * {@link JavaNgrokConfig#getConfigPath()}, if none exists.
     *
     * @param javaNgrokConfig The config to use when interacting with the <code>ngrok</code> binary.
     * @param ngrokInstaller  The class used to download and install <code>ngrok</code>.
     */
    public NgrokProcess(final JavaNgrokConfig javaNgrokConfig,
                        final NgrokInstaller ngrokInstaller) {
        this.javaNgrokConfig = javaNgrokConfig;
        this.ngrokInstaller = ngrokInstaller;

        if (!Files.exists(javaNgrokConfig.getNgrokPath())) {
            ngrokInstaller.installNgrok(javaNgrokConfig.getNgrokPath(), javaNgrokConfig.getNgrokVersion());
        }
        if (!Files.exists(javaNgrokConfig.getConfigPath())) {
            ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath(), Map.of(),
                javaNgrokConfig.getNgrokVersion());
        }
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

    /**
     * If not already running, start a <code>ngrok</code> process with no tunnels. This will start the
     * <code>ngrok</code> web interface, against which HTTP requests can be made to create, interact with, and
     * destroy tunnels.
     *
     * @throws NgrokException             <code>ngrok</code> could not start.
     * @throws JavaNgrokSecurityException The URL was not supported.
     */
    public void start() {
        if (isRunning()) {
            return;
        }

        if (!Files.exists(javaNgrokConfig.getNgrokPath())) {
            throw new NgrokException(String.format("ngrok binary was not found. "
                                                   + "Be sure to call \"NgrokInstaller.installNgrok()\" first for "
                                                   + "\"ngrokPath\": %s",
                javaNgrokConfig.getNgrokPath()));
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
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            LOGGER.fine(String.format("ngrok process starting with PID: %s", process.pid()));

            processMonitor = new ProcessMonitor(process, javaNgrokConfig);
            new Thread(processMonitor).start();

            final Calendar timeout = Calendar.getInstance();
            timeout.add(Calendar.SECOND, javaNgrokConfig.getStartupTimeout());
            while (Calendar.getInstance().before(timeout)) {
                if (processMonitor.isHealthy()) {
                    LOGGER.info(String.format("ngrok process has started with API URL: %s", processMonitor.apiUrl));

                    processMonitor.startupError = null;

                    break;
                } else if (!isRunning()) {
                    break;
                }
            }

            if (!processMonitor.isHealthy()) {
                // If the process did not come up in a healthy state, clean up the state
                stop();

                if (nonNull(processMonitor.startupError)) {
                    throw new NgrokException(String.format("The ngrok process errored on start: %s.",
                        processMonitor.startupError), processMonitor.logs, processMonitor.startupError);
                } else {
                    throw new NgrokException("The ngrok process was unable to start.", processMonitor.logs);
                }
            }
        } catch (final IOException e) {
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
     * Terminate the <code>ngrok</code> processes, if running. This method will not block, it will just issue a kill
     * request.
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
        try {
            if (nonNull(processMonitor.reader)) {
                processMonitor.reader.close();
            }
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "An error occurred when closing \"ProcessMonitor.reader\"", e);
        }
    }

    /**
     * Set the <code>ngrok</code> auth token in the config file, enabling authenticated features (for instance, opening
     * multiple concurrent tunnels, custom domains, etc.).
     *
     * <pre>
     * // Setting an auth token allows us to do things like open multiple tunnels at the same time
     * final NgrokClient ngrokClient = new NgrokClient.Builder().build();
     * ngrokClient.setAuthToken("&lt;NGROK_AUTHTOKEN&gt;")
     *
     * // &lt;NgrokTunnel: "http://&lt;public_sub1&gt;.ngrok.io" -&gt; "http://localhost:80"&gt;
     * final Tunnel ngrokTunnel1 = ngrokClient.connect();
     * // &lt;NgrokTunnel: "http://&lt;public_sub2&gt;.ngrok.io" -&gt; "http://localhost:8000"&gt;
     * final CreateTunnel sshCreateTunnel = new CreateTunnel.Builder()
     *         .withAddr(8000)
     *         .build();
     * final Tunnel ngrokTunnel2 = ngrokClient.connect(createTunnel);
     * </pre>
     *
     * <p>The auth token can also be set in the {@link JavaNgrokConfig} that is passed to the
     * {@link NgrokClient.Builder}.
     *
     * @param authToken The auth token.
     * @throws NgrokException <code>ngrok</code> could not start.
     */
    public void setAuthToken(final String authToken) {
        final List<String> args = new ArrayList<>();
        if (javaNgrokConfig.getNgrokVersion() == NgrokVersion.V2) {
            args.add("authtoken");
            args.add(authToken);
        } else {
            args.add("config");
            args.add("add-authtoken");
            args.add(authToken);
        }
        args.add("--log=stdout");

        if (nonNull(javaNgrokConfig.getConfigPath())) {
            args.add(String.format("--config=%s", javaNgrokConfig.getConfigPath().toString()));
        }

        LOGGER.info(String.format("Updating authtoken for \"configPath\": %s", javaNgrokConfig.getConfigPath()));

        try {
            final String result = captureRunProcess(javaNgrokConfig.getNgrokPath(), args);
            if (!result.contains("Authtoken saved")) {
                throw new NgrokException(String.format("An error occurred while setting the auth token: %s", result));
            }
        } catch (final IOException | InterruptedException e) {
            throw new NgrokException("An error occurred while setting the auth token for ngrok.", e);
        }
    }

    /**
     * Set the <code>ngrok</code> API key in the config file, enabling more features (for instance, labeled tunnels).
     *
     * <p>The API key can also be set in the {@link JavaNgrokConfig} that is passed to the
     * {@link NgrokClient.Builder}.
     *
     * @param apiKey The API key.
     * @throws NgrokException <code>ngrok</code> could not start.
     */
    public void setApiKey(final String apiKey) {
        final List<String> args = new ArrayList<>();
        if (javaNgrokConfig.getNgrokVersion() == NgrokVersion.V3) {
            args.add("config");
            args.add("add-api-key");
            args.add(apiKey);
        } else {
            throw new JavaNgrokException(String.format("ngrok %s does not have this command.",
                javaNgrokConfig.getNgrokVersion()));
        }
        args.add("--log=stdout");

        if (nonNull(javaNgrokConfig.getConfigPath())) {
            args.add(String.format("--config=%s", javaNgrokConfig.getConfigPath().toString()));
        }

        LOGGER.info(String.format("Updating API key for \"configPath\": %s", javaNgrokConfig.getConfigPath()));

        try {
            final String result = captureRunProcess(javaNgrokConfig.getNgrokPath(), args);
            if (!result.contains("API key saved")) {
                throw new NgrokException(String.format("An error occurred while setting the API key: %s",
                    result));
            }
        } catch (final IOException | InterruptedException e) {
            throw new NgrokException("An error occurred while setting the API key for ngrok.", e);
        }
    }

    /**
     * Update <code>ngrok</code>, if an update is available.
     *
     * @throws NgrokException <code>ngrok</code> could not start.
     */
    public void update() {
        try {
            captureRunProcess(javaNgrokConfig.getNgrokPath(),
                List.of(javaNgrokConfig.getNgrokPath().toString(), "update", "--log=stdout"));
        } catch (final IOException | InterruptedException e) {
            throw new NgrokException("An error occurred while trying to update ngrok.", e);
        }
    }

    /**
     * Get the <code>ngrok</code> version.
     *
     * @return The version.
     * @throws NgrokException <code>ngrok</code> could not start.
     */
    public String getVersion() {
        try {
            return captureRunProcess(javaNgrokConfig.getNgrokPath(), List.of("--version")).split("version ")[1];
        } catch (final IOException | InterruptedException | ArrayIndexOutOfBoundsException e) {
            throw new NgrokException("An error occurred while getting the version for ngrok.", e);
        }
    }

    /**
     * Get the API URL for the <code>ngrok</code> web interface.
     *
     * @throws JavaNgrokSecurityException The URL was not supported.
     */
    public String getApiUrl() {
        if (!isRunning() || !processMonitor.isHealthy()) {
            return null;
        }

        return processMonitor.apiUrl;
    }

    /**
     * A Runnable that monitors the <code>ngrok</code> thread.
     */
    public static class ProcessMonitor implements Runnable {

        private final List<NgrokLog> logs = new ArrayList<>();

        private final Process process;
        private final JavaNgrokConfig javaNgrokConfig;
        private final HttpClient httpClient;

        private boolean alive = true;

        private String apiUrl;
        private boolean tunnelStarted;
        private boolean clientConnected;
        private String startupError;
        private BufferedReader reader;

        /**
         * Construct to monitor a {link @Process} monitor.
         *
         * @param process         The Process to monitor.
         * @param javaNgrokConfig The config to use when monitoring the Process.
         */
        public ProcessMonitor(final Process process,
                              final JavaNgrokConfig javaNgrokConfig) {
            this(process, javaNgrokConfig, new DefaultHttpClient.Builder().build());
        }

        /**
         * Construct to monitor a {@link Process} monitor with a custom {@link HttpClient}.
         *
         * @param process         The Process to monitor.
         * @param javaNgrokConfig The config to use when monitoring the Process.
         * @param httpClient      The custom HTTP client.
         */
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
                reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

                String line;
                while (nonNull(line = reader.readLine())) {
                    logStartupLine(line);

                    if (isHealthy()) {
                        break;
                    } else if (nonNull(startupError)) {
                        alive = false;
                        break;
                    }
                }

                while (alive && process.isAlive()
                       && javaNgrokConfig.isKeepMonitoring()
                       && nonNull(line = reader.readLine())) {
                    logLine(line);
                }

                alive = false;
            } catch (final IOException e) {
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

        /**
         * Set the monitor thread to stop monitoring the ngrok process after the next log event. This will not
         * necessarily terminate the process immediately, as the process may currently be idle, rather it sets a flag on
         * the thread telling it to terminate the next time it wakes up.
         *
         * <p>This has no impact on the ngrok process itself, only <code>java-ngrok</code>'s monitor of the process and
         * its logs.
         */
        public void stop() {
            this.alive = false;
        }

        private boolean isHealthy() {
            if (isNull(apiUrl) || !tunnelStarted || !clientConnected) {
                return false;
            }

            if (!apiUrl.toLowerCase().startsWith("http")) {
                throw new JavaNgrokSecurityException(String.format("URL must start with \"http\": %s", apiUrl));
            }

            final Response<Tunnels> tunnelsResponse = httpClient.get(String.format("%s/api/tunnels", apiUrl),
                Tunnels.class);
            if (tunnelsResponse.getStatusCode() != HTTP_OK) {
                return false;
            }

            return nonNull(process) && process.isAlive();
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

            LOGGER.log(Level.parse(ngrokLog.getLvl()), ngrokLog.getLine());
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
