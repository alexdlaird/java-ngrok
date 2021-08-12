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

import com.github.alexdlaird.ngrok.protocol.Region;

import java.io.File;

import static java.util.Objects.isNull;

/**
 * An object for managing <code>java-ngrok</code>'s configuration to interact the <code>ngrok</code> binary.
 */
public class JavaNgrokConfig {
    private final File ngrokPath;
    private final File configPath;
    private final String authToken;
    private final Region region;

    public JavaNgrokConfig(final Builder builder) {
        ngrokPath = builder.ngrokPath;
        configPath = builder.configPath;
        authToken = builder.authToken;
        region = builder.region;
    }

    public File getNgrokPath() {
        return ngrokPath;
    }

    public File getConfigPath() {
        return configPath;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Region getRegion() {
        return region;
    }

    public static class Builder {

        private File ngrokPath;
        private File configPath;
        private String authToken;
        private Region region;

        public Builder withNgrokPath(final File ngrokPath) {
            this.ngrokPath = ngrokPath;
            return this;
        }

        public Builder withConfigPath(final File configPath) {
            this.configPath = configPath;
            return this;
        }

        public Builder withAuthToken(final String authToken) {
            this.authToken = authToken;
            return this;
        }

        public Builder withRegion(final Region region) {
            this.region = region;
            return this;
        }

        public JavaNgrokConfig build() {
            if (isNull(ngrokPath)) {
                // TODO: once ngrok installer is implemented, this should point to the managed binary
                ngrokPath = new File("ngrok");
            }
            if (isNull(configPath)) {
                final String path = System.getProperty("user.home") + File.separator + ".ngrok2" + File.separator + "ngrok.yml";
                configPath = new File(path);
            }

            return new JavaNgrokConfig(this);
        }
    }
}
