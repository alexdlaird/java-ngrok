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
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.alexdlaird.StringUtils.isBlank;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * An object containing information about the <code>ngrok</code> process.
 */
public class NgrokProcess {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(NgrokProcess.class));

    private final JavaNgrokConfig javaNgrokConfig;

    private final NgrokInstaller ngrokInstaller;

    private Process process;

    private ProcessMonitor processMonitor;

    public NgrokProcess(final JavaNgrokConfig javaNgrokConfig,
                        final NgrokInstaller ngrokInstaller) {
        this.javaNgrokConfig = javaNgrokConfig;
        this.ngrokInstaller = ngrokInstaller;

        if (!Files.exists(javaNgrokConfig.getNgrokPath())) {
            this.ngrokInstaller.installNgrok(javaNgrokConfig.getNgrokPath());
        }
        if (!Files.exists(javaNgrokConfig.getConfigPath())) {
            this.ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath());
        }
    }

    /**
     * Start a <code>ngrok</code> process with no tunnels. This will start the <code>ngrok</code> web interface,
     * against which HTTP requests can be made to create, interact with, and destroy tunnels.
     */
    public void start() {
        if (isRunning()) {
            return;
        }

        // TODO: parse the ngrok config, then validated it before startup
        // ngrokInstaller.validateConfig(data);

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);

        final List<String> command = new ArrayList<>();
        command.add(javaNgrokConfig.getNgrokPath().toString());
        command.add("start");
        command.add("--none");
        command.add("--log=stdout");

        if (nonNull(javaNgrokConfig.getConfigPath())) {
            command.add(String.format("--config=%s", javaNgrokConfig.getConfigPath().toString()));
        }
        if (nonNull(javaNgrokConfig.getAuthToken())) {
            command.add(String.format("--authtoken=%s", javaNgrokConfig.getAuthToken()));
        }
        if (nonNull(javaNgrokConfig.getRegion())) {
            command.add(String.format("--region=%s", javaNgrokConfig.getRegion()));
        }

        processBuilder.command(command);
        try {
            process = processBuilder.start();
            processMonitor = new ProcessMonitor(process);
            new Thread(processMonitor).start();

            final Calendar timeout = Calendar.getInstance();
            timeout.add(Calendar.SECOND, javaNgrokConfig.getStartupTime());
            while (Calendar.getInstance().before(timeout)) {
                if (processMonitor.isHealthy()) {
                    break;
                }
            }

            if (!processMonitor.isHealthy()) {
                stop();

                // TODO: implement recursive retries if this is a common ngrok connection issue

                // TODO: put the log output in the exception
                throw new NgrokException("The ngrok process was unable to start.");
            }
        } catch (IOException e) {
            throw new NgrokException("An error occurred while starting ngrok.", e);
        }
    }

    public boolean isRunning() {
        return nonNull(processMonitor);
    }

    /**
     * Terminate the <code>ngrok</code> processes, if running. This method will not block, it will
     * just issue a kill request.
     */
    public void stop() {
        if (!isRunning()) {
            return;
        }

        processMonitor.stop();
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();

        process = null;
        processMonitor = null;
    }

    /**
     * Set the <code>ngrok</code> auth token in the config file, enabling authenticated features (for instance,
     * more concurrent tunnels, custom subdomains, etc.).
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

        processBuilder.command(command);
        try {
            final Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new NgrokException("An error occurred while setting the auth token for ngrok.", e);
        }
    }

    /**
     * Update <code>ngrok</code>, if an update is available.
     */
    public void update() {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);

        final List<String> command = new ArrayList<>();
        command.add(javaNgrokConfig.getNgrokPath().toString());
        command.add("update");
        command.add("--log=stdout");

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

        final List<String> command = new ArrayList<>();
        command.add(javaNgrokConfig.getNgrokPath().toString());
        command.add("--version");

        processBuilder.command(command);
        try {
            final Process process = processBuilder.start();
            process.waitFor();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String version = "unknown";
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("version ")) {
                    version = line.split("version ")[1];
                    break;
                }
            }

            return version;
        } catch (IOException | InterruptedException e) {
            throw new NgrokException("An error occurred while trying to update ngrok.", e);
        }
    }

    public NgrokInstaller getNgrokInstaller() {
        return ngrokInstaller;
    }

    public String getApiUrl() {
        if (!isRunning() || !processMonitor.isHealthy()) {
            return null;
        }

        return processMonitor.apiUrl;
    }

    private static class ProcessMonitor implements Runnable {
        private final Process process;
        private boolean alive = true;
        private String apiUrl;
        private boolean tunnelStarted;
        private boolean clientConnected;
        private String startupError;

        public ProcessMonitor(final Process process) {
            this.process = process;
        }

        public void stop() {
            this.alive = false;
        }

        public void run() {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while (alive && (line = reader.readLine()) != null) {
                    logStartupLine(line);

                    if (isHealthy()) {
                        break;
                    } else if (!isBlank(startupError)) {
                        break;
                    }
                }

                // TODO: check if we've errored out and the process needs to be shut down

                // TODO: add support to keep monitoring logs after startup
            } catch (IOException e) {
                throw new NgrokException("An error occurred in the ngrok process.", e);
            }
        }

        public boolean isHealthy() {
            if (isNull(apiUrl) || !tunnelStarted || !clientConnected) {
                return false;
            }

            if (!apiUrl.toLowerCase(Locale.ROOT).startsWith("http")) {
                throw new JavaNgrokSecurityException(String.format("URL must start with \"http\": %s", apiUrl));
            }

            // TODO: Ensure the process is available for requests before registering it as healthy

            return process.isAlive() && isNull(startupError);
        }

        private void logStartupLine(final String line) {
            final NgrokLog ngrokLog = logLine(line);

            if (isNull(ngrokLog)) {
                return;
            }

            if (ngrokLog.getLvl().equals("ERROR") || ngrokLog.getLine().equals("CRITICAL")) {
                this.startupError = ngrokLog.getErr();
            } else {
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

            return ngrokLog;
        }
    }
}
