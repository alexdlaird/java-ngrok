/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.conf;

import com.github.alexdlaird.ngrok.installer.ConfigVersion;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.process.NgrokLog;
import com.github.alexdlaird.ngrok.protocol.Region;
import java.nio.file.Path;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * An object for managing <code>java-ngrok</code>'s configuration to interact the <code>ngrok</code> binary.
 *
 * <p>For usage examples, see
 * <a href="https://alexdlaird.github.io/java-ngrok/" target="_blank"><code>java-ngrok</code>'s documentation</a>.
 */
public class JavaNgrokConfig {

    private final NgrokVersion ngrokVersion;
    private final int maxLogs;
    private final int startupTimeout;
    private final boolean keepMonitoring;
    private final Path ngrokPath;
    private final Path configPath;
    private final String authToken;
    private final Region region;
    private final Function<NgrokLog, Void> logEventCallback;
    private final String apiKey;
    private final ConfigVersion configVersion;

    private JavaNgrokConfig(final Builder builder) {
        this.ngrokVersion = builder.ngrokVersion;
        this.maxLogs = builder.maxLogs;
        this.startupTimeout = builder.startupTimeout;
        this.keepMonitoring = builder.keepMonitoring;
        this.ngrokPath = builder.ngrokPath;
        this.configPath = builder.configPath;
        this.authToken = builder.authToken;
        this.region = builder.region;
        this.logEventCallback = builder.logEventCallback;
        this.apiKey = builder.apiKey;
        this.configVersion = builder.configVersion;
    }

    /**
     * Get the major <code>ngrok</code> version to be used.
     */
    public NgrokVersion getNgrokVersion() {
        return ngrokVersion;
    }

    /**
     * Get the maximum number of <code>ngrok</code> logs to retain in the monitoring thread.
     */
    public int getMaxLogs() {
        return maxLogs;
    }

    /**
     * Get the max timeout, in seconds, to wait for <code>ngrok</code> to start.
     */
    public int getStartupTimeout() {
        return startupTimeout;
    }

    /**
     * Whether the <code>ngrok</code> process will continue to be monitored after it finishes starting up.
     */
    public boolean isKeepMonitoring() {
        return keepMonitoring;
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
     * Get the <code>ngrok</code> authtoken that will be passed to commands.
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
     * Get the log event callback that will be invoked each time <code>ngrok</code> emits a log.
     */
    public Function<NgrokLog, Void> getLogEventCallback() {
        return logEventCallback;
    }

    /**
     * A <code>ngrok</code> API key.
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * The <code>ngrok</code> config version.
     */
    public ConfigVersion getConfigVersion() {
        return configVersion;
    }

    /**
     * Builder for a {@link JavaNgrokConfig}, see docs for that class for example usage.
     */
    public static class Builder {

        private NgrokVersion ngrokVersion = NgrokVersion.V3;
        private int maxLogs = 100;
        private int startupTimeout = 15;
        private boolean keepMonitoring = true;
        private ConfigVersion configVersion = ConfigVersion.V2;

        private Path ngrokPath;
        private Path configPath;
        private String authToken;
        private Region region;
        private Function<NgrokLog, Void> logEventCallback;
        private String apiKey;

        /**
         * Construct a JavaNgrokConfig Builder.
         */
        public Builder() {
        }

        /**
         * Copy a {@link JavaNgrokConfig} in to a new Builder.
         *
         * @param javaNgrokConfig The JavaNgrokConfig to copy.
         */
        public Builder(final JavaNgrokConfig javaNgrokConfig) {
            this.ngrokVersion = javaNgrokConfig.ngrokVersion;
            this.maxLogs = javaNgrokConfig.maxLogs;
            this.startupTimeout = javaNgrokConfig.startupTimeout;
            this.keepMonitoring = javaNgrokConfig.keepMonitoring;
            this.ngrokPath = javaNgrokConfig.ngrokPath;
            this.configPath = javaNgrokConfig.configPath;
            this.authToken = javaNgrokConfig.authToken;
            this.region = javaNgrokConfig.region;
            this.logEventCallback = javaNgrokConfig.logEventCallback;
            this.apiKey = javaNgrokConfig.apiKey;
            this.configVersion = javaNgrokConfig.configVersion;
        }

        /**
         * The major version of <code>ngrok</code> to be used.
         */
        public Builder withNgrokVersion(final NgrokVersion ngrokVersion) {
            this.ngrokVersion = ngrokVersion;
            return this;
        }

        /**
         * The maximum number of <code>ngrok</code> logs to retain in the monitoring thread.
         *
         * @throws IllegalArgumentException The argument was invalid.
         */
        public Builder withMaxLogs(final int maxLogs) {
            if (maxLogs < 1) {
                throw new IllegalArgumentException("\"maxLogs\" must be greater than 0.");
            }

            this.maxLogs = maxLogs;
            return this;
        }

        /**
         * The max timeout, in seconds, to wait for <code>ngrok</code> to start.
         *
         * @throws IllegalArgumentException The argument was invalid.
         */
        public Builder withStartupTimeout(final int startupTimeout) {
            if (startupTimeout < 1) {
                throw new IllegalArgumentException("\"startupTimeout\" must be greater than 0.");
            }

            this.startupTimeout = startupTimeout;
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
         * The path to the <code>ngrok</code> binary, defaults to being placed in the same directory as
         * <a href="https://ngrok.com/docs/agent/config/v2" target="_blank"><code>ngrok's</code> configs</a>.
         */
        public Builder withNgrokPath(final Path ngrokPath) {
            this.ngrokPath = ngrokPath;
            return this;
        }

        /**
         * The path to the <code>ngrok</code> config file, defaults to <a href="https://ngrok.com/docs/agent/config/v2"
         * target="_blank"><code>ngrok's</code> default config location</a>.
         */
        public Builder withConfigPath(final Path configPath) {
            this.configPath = configPath;
            return this;
        }

        /**
         * A <code>ngrok</code> authtoken to pass to commands (overrides what is in the config). If not set here, the
         * {@link Builder} will attempt to use the environment variable <code>NGROK_AUTHTOKEN</code> if it is set.
         */
        public Builder withAuthToken(final String authToken) {
            this.authToken = authToken;
            return this;
        }

        /**
         * The region in which <code>ngrok</code> should start.
         */
        public Builder withRegion(final Region region) {
            this.region = region;
            return this;
        }

        /**
         * A callback that will be invoked each time <code>ngrok</code> emits a log. {@link #keepMonitoring} must be set
         * to <code>true</code> or the function will stop being called after <code>ngrok</code> finishes starting. See
         * {@link JavaNgrokConfig} for example usage.
         */
        public Builder withLogEventCallback(final Function<NgrokLog, Void> logEventCallback) {
            this.logEventCallback = logEventCallback;
            return this;
        }

        /**
         * A <code>ngrok</code> API key. If not set here, the {@link Builder} will attempt to use the environment
         * variable <code>NGROK_API_KEY</code> if it is set.
         */
        public Builder withApiKey(final String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * The <code>ngrok</code> config version.
         */
        public Builder withConfigVersion(final ConfigVersion configVersion) {
            this.configVersion = configVersion;
            return this;
        }

        /**
         * Build the {@link JavaNgrokConfig}.
         */
        public JavaNgrokConfig build() {
            if (isNull(ngrokPath)) {
                ngrokPath = NgrokInstaller.DEFAULT_NGROK_PATH;
            }
            if (isNull(configPath)) {
                configPath = NgrokInstaller.DEFAULT_CONFIG_PATH;
            }
            final String envAuthToken = System.getenv("NGROK_AUTHTOKEN");
            if (isNull(authToken) && nonNull(envAuthToken)) {
                authToken = envAuthToken;
            }
            final String envApiKey = System.getenv("NGROK_API_KEY");
            if (isNull(apiKey) && nonNull(envApiKey)) {
                apiKey = envApiKey;
            }

            return new JavaNgrokConfig(this);
        }
    }
}
