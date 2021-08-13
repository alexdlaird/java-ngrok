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

package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.http.Response;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import com.github.alexdlaird.ngrok.protocol.Version;

import java.util.Collections;

import static java.util.Objects.isNull;

/**
 * A client for interacting with  <a href="https://ngrok.com/docs">ngrok</a>, its binary, and its APIs.
 */
public class NgrokClient {

    private final JavaNgrokConfig javaNgrokConfig;
    private final NgrokInstaller ngrokInstaller;
    private final NgrokProcess ngrokProcess;
    private final HttpClient httpClient;

    // TODO: interactions with NgrokProcess in this class are POC for simple testing while the API is built out, java-ngrok will soon manage its own binary

    private NgrokClient(final Builder builder) {
        this.javaNgrokConfig = builder.javaNgrokConfig;
        this.ngrokInstaller = builder.ngrokInstaller;
        this.ngrokProcess = builder.ngrokProcess;
        this.httpClient = builder.httpClient;
    }

    public Tunnel connect(final CreateTunnel createTunnel) {
        ngrokProcess.start();

        final Response<Tunnel> response = httpClient.post("/api/tunnels", createTunnel, Collections.emptyList(), Collections.emptyMap(), Tunnel.class);

        final Tunnel tunnel;
        if (createTunnel.getProto().equals("http") && createTunnel.getBindTls().equals("both")) {
            final Response<Tunnel> getResponse = httpClient.get(response.getBody().getUri() + "%20%28http%29", Collections.emptyList(), Collections.emptyMap(), Tunnel.class);
            tunnel = getResponse.getBody();
        } else {
            tunnel = response.getBody();
        }

        return tunnel;
    }

    public Tunnel connect() {
        return connect(new CreateTunnel.Builder().build());
    }

    public void disconnect(final String publicUrl) {
        ngrokProcess.start();

        final Tunnels tunnels = getTunnels();
        Tunnel tunnel = null;
        // TODO: cache active tunnels so we can first check that before falling back to an API request
        for (Tunnel t : tunnels.getTunnels()) {
            if (t.getPublicUrl().equals(publicUrl)) {
                tunnel = t;
                break;
            }
        }

        if (isNull(tunnel)) {
            return;
        }

        httpClient.delete(tunnel.getUri(), Collections.emptyList(), Collections.emptyMap(), Object.class);
    }

    public Tunnels getTunnels() {
        final Response<Tunnels> response = httpClient.get("/api/tunnels", Collections.emptyList(), Collections.emptyMap(), Tunnels.class);

        return response.getBody();
    }

    public void kill() {
        ngrokProcess.stop();
    }

    public void setAuthToken(final String authToken) {
        ngrokProcess.setAuthToken(authToken);
    }

    public void update() {
        ngrokProcess.update();
    }

    public Version getVersion() {
        final String ngrokVersion = ngrokProcess.getVersion();

        // TODO: parse out java-ngrok version from POM
        String javaNgrokVersion = null;

//        new Version(ngrokVersion, javaNgrokVersion);

        throw new UnsupportedOperationException();
    }

    public JavaNgrokConfig getJavaNgrokConfig() {
        return javaNgrokConfig;
    }

    public NgrokInstaller getNgrokInstaller() {
        return ngrokInstaller;
    }

    public NgrokProcess getNgrokProcess() {
        return ngrokProcess;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public static class Builder {

        private JavaNgrokConfig javaNgrokConfig;
        private NgrokInstaller ngrokInstaller;
        private NgrokProcess ngrokProcess;
        private HttpClient httpClient;

        public Builder withJavaNgrokConfig(final JavaNgrokConfig javaNgrokConfig) {
            this.javaNgrokConfig = javaNgrokConfig;
            return this;
        }

        public Builder withNgrokInstaller(final NgrokInstaller ngrokInstaller) {
            this.ngrokInstaller = ngrokInstaller;
            return this;
        }

        public Builder withNgrokProcess(final NgrokProcess ngrokProcess) {
            this.ngrokProcess = ngrokProcess;
            return this;
        }

        public Builder withHttpClient(final HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public NgrokClient build() {
            if (isNull(javaNgrokConfig)) {
                javaNgrokConfig = new JavaNgrokConfig.Builder().build();
            }
            if (isNull(ngrokInstaller)) {
                ngrokInstaller = new NgrokInstaller();
            }
            if (isNull(ngrokProcess)) {
                ngrokProcess = new NgrokProcess(javaNgrokConfig, ngrokInstaller);
            }
            if (isNull(httpClient)) {
                httpClient = new DefaultHttpClient.Builder(ngrokProcess.getApiUrl()).build();
            }

            return new NgrokClient(this);
        }
    }
}
