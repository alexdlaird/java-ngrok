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
import com.github.alexdlaird.ngrok.protocol.Region;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.isNull;

/**
 * An object for managing <code>java-ngrok</code>'s configuration to interact the <code>ngrok</code> binary.
 */
public class JavaNgrokConfig {
    private final Path ngrokPath;
    private final Path configPath;
    private final String authToken;
    private final Region region;

    public JavaNgrokConfig(final Builder builder) {
        ngrokPath = builder.ngrokPath;
        configPath = builder.configPath;
        authToken = builder.authToken;
        region = builder.region;
    }

    public Path getNgrokPath() {
        return ngrokPath;
    }

    public Path getConfigPath() {
        return configPath;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Region getRegion() {
        return region;
    }

    public static class Builder {

        private Path ngrokPath;
        private Path configPath;
        private String authToken;
        private Region region;

        public Builder withNgrokPath(final Path ngrokPath) {
            this.ngrokPath = ngrokPath;
            return this;
        }

        public Builder withConfigPath(final Path configPath) {
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
                ngrokPath = Paths.get(System.getProperty("user.home") + File.separator + ".ngrok2" + File.separator + NgrokInstaller.getNgrokBin());
            }
            if (isNull(configPath)) {
                configPath = Paths.get(System.getProperty("user.home") + File.separator + ".ngrok2" + File.separator + "ngrok.yml");
            }

            return new JavaNgrokConfig(this);
        }
    }
}
