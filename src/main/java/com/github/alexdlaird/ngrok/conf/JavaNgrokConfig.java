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

package com.github.alexdlaird.ngrok.conf;

import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.process.NgrokLog;
import com.github.alexdlaird.ngrok.protocol.Region;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import static java.util.Objects.isNull;

/**
 * An object for managing <code>java-ngrok</code>'s configuration to interact the <code>ngrok</code> binary.
 */
public class JavaNgrokConfig {
    private final Path ngrokPath;
    private final Path configPath;
    private final String authToken;
    private final Region region;
    private final boolean keepMonitoring;
    private final int maxLogs;
    private final Function<NgrokLog, Void> logEventCallback;
    private final int startupTimeout;

    private JavaNgrokConfig(final Builder builder) {
        ngrokPath = builder.ngrokPath;
        configPath = builder.configPath;
        authToken = builder.authToken;
        region = builder.region;
        keepMonitoring = builder.keepMonitoring;
        maxLogs = builder.maxLogs;
        logEventCallback = builder.logEventCallback;
        startupTimeout = builder.startupTimeout;
    }

    /**
     * Get the path to the <code>ngrok</code> binary.
     */
    public Path getNgrokPath() {
        return ngrokPath;
    }

    /**
     * Get the path to the <code>ngrok</code> config file.
     */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Get the authtoken that will be passed to commands.
     */
    public String getAuthToken() {
        return authToken;
    }

    /**
     * Get the region in which <code>ngrok</code> will start.
     */
    public Region getRegion() {
        return region;
    }

    /**
     * Get whether the <code>ngrok</code> process will continue to be monitored after it finishes starting up.
     */
    public boolean isKeepMonitoring() {
        return keepMonitoring;
    }

    /**
     * Get the maximum number of <code>ngrok</code> logs to retain in the monitoring thread.
     */
    public int getMaxLogs() {
        return maxLogs;
    }

    /**
     * Get the log event callback that will be invoked each time <code>ngrok</code> emits a log.
     */
    public Function<NgrokLog, Void> getLogEventCallback() {
        return logEventCallback;
    }

    /**
     * Get the startup time before <code>ngrok</code> times out on boot.
     */
    public int getStartupTime() {
        return startupTimeout;
    }

    /**
     * Builder for a {@link JavaNgrokConfig}.
     */
    public static class Builder {

        private Path ngrokPath;
        private Path configPath;
        private String authToken;
        private Region region;
        private boolean keepMonitoring;
        private int maxLogs = 100;
        private Function<NgrokLog, Void> logEventCallback;
        private int startupTimeout = 15;
        // TODO: implement reconnectSessionRetries config var

        /**
         * The path to the <code>ngrok</code> binary, defaults to ~/.ngrok2/ngrok.
         */
        public Builder withNgrokPath(final Path ngrokPath) {
            this.ngrokPath = ngrokPath;
            return this;
        }

        /**
         * The path to the <code>ngrok</code> config file, defaults to ~/.ngrok2/config.yml.
         */
        public Builder withConfigPath(final Path configPath) {
            this.configPath = configPath;
            return this;
        }

        /**
         * An authtoken to pass to commands (overrides what is in the config).
         */
        public Builder withAuthToken(final String authToken) {
            this.authToken = authToken;
            return this;
        }

        /**
         * The region in which <code>ngrok</code> should start
         */
        public Builder withRegion(final Region region) {
            this.region = region;
            return this;
        }

        /**
         * Don't keep monitoring <code>ngrok</code> (for logs, etc.) after startup is complete.
         */
        public Builder withoutMonitoring() {
            this.keepMonitoring = false;
            return this;
        }

        /**
         * The maximum number of <code>ngrok</code> logs to retain in the monitoring thread.
         */
        public Builder withMaxLogs(final int maxLogs) {
            if (maxLogs < 1) {
                throw new IllegalArgumentException("\"maxLogs\" must be greater than 0.");
            }

            this.maxLogs = maxLogs;
            return this;
        }

        /**
         * A callback that will be invoked each time <code>ngrok</code> emits a log. {@link #keepMonitoring} must be
         * set to <code>true</code> or the function will stop being called after <code>ngrok</code> finishes starting.
         */
        public Builder withLogEventCallback(final Function<NgrokLog, Void> logEventCallback) {
            this.logEventCallback = logEventCallback;
            return this;
        }

        /**
         * The max number of seconds to wait for <code>ngrok</code> to start before timing out.
         */
        public Builder withStartupTimeout(final int startupTimeout) {
            if (startupTimeout < 1) {
                throw new IllegalArgumentException("\"startupTimeout\" must be greater than 0.");
            }

            this.startupTimeout = startupTimeout;
            return this;
        }

        public JavaNgrokConfig build() {
            if (isNull(ngrokPath)) {
                ngrokPath = Paths.get(System.getProperty("user.home"), ".ngrok2", NgrokInstaller.getNgrokBin());
            }
            if (isNull(configPath)) {
                configPath = Paths.get(System.getProperty("user.home"), ".ngrok2", "ngrok.yml");
            }

            return new JavaNgrokConfig(this);
        }
    }
}
