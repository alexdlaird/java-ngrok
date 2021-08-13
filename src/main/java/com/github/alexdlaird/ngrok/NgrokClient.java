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

import com.github.alexdlaird.exception.JavaNgrokHTTPException;
import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.http.HttpClientException;
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
 * A client for interacting with <a href="https://ngrok.com/docs">ngrok</a>, its binary, and its APIs.
 * Can be configured with {@link JavaNgrokConfig}.
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

    /**
     * Establish a new <code>ngrok</code> tunnel for the tunnel definition, returning an object representing
     * the connected tunnel.
     *
     * @param createTunnel The tunnel definition.
     * @return The created Tunnel.
     */
    public Tunnel connect(final CreateTunnel createTunnel) {
        ngrokProcess.start();

        final Response<Tunnel> response;
        try {
            response = httpClient.post("/api/tunnels", createTunnel, Collections.emptyList(), Collections.emptyMap(), Tunnel.class);
        } catch (HttpClientException e) {
            throw new JavaNgrokHTTPException(String.format("An error occurred when POSTing to create the tunnel %s.", createTunnel.getName()), e);
        }

        final Tunnel tunnel;
        if (createTunnel.getProto().equals("http") && createTunnel.getBindTls().equals("both")) {
            try {
                final Response<Tunnel> getResponse = httpClient.get(response.getBody().getUri() + "%20%28http%29", Collections.emptyList(), Collections.emptyMap(), Tunnel.class);
                tunnel = getResponse.getBody();
            } catch (HttpClientException e) {
                throw new JavaNgrokHTTPException(String.format("An error occurred when GETing the HTTP tunnel %s.", response.getBody().getName()), e);
            }
        } else {
            tunnel = response.getBody();
        }

        return tunnel;
    }

    /**
     * Establish a new <code>ngrok</code> tunnel with a default tunnel definition, returning an object representing
     * the connected tunnel.
     *
     * @return The created Tunnel.
     */
    public Tunnel connect() {
        return connect(new CreateTunnel.Builder().build());
    }

    /**
     * Disconnect the <code>ngrok</code> tunnel for the given URL, if open.
     *
     * @param publicUrl The public URL of the tunnel to disconnect.
     */
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

        try {
            httpClient.delete(tunnel.getUri(), Collections.emptyList(), Collections.emptyMap(), Object.class);
        } catch (HttpClientException e) {
            throw new JavaNgrokHTTPException(String.format("An error occurred when DELETing the tunnel %s.", publicUrl), e);
        }
    }

    /**
     * Get a list of active <code>ngrok</code> tunnels.
     *
     * @return The active <code>ngrok</code> tunnels.
     */
    public Tunnels getTunnels() {
        try {
            final Response<Tunnels> response = httpClient.get("/api/tunnels", Collections.emptyList(), Collections.emptyMap(), Tunnels.class);

            return response.getBody();
        } catch (HttpClientException e) {
            throw new JavaNgrokHTTPException("An error occurred when GETing the tunnels.", e);
        }
    }

    /**
     * Terminate the <code>ngrok</code> processes, if running. This method will not block, it will
     * just issue a kill request.
     */
    public void kill() {
        ngrokProcess.stop();
    }

    /**
     * Set the <code>ngrok</code> auth token in the config file, enabling authenticated features (for instance,
     * more concurrent tunnels, custom subdomains, etc.).
     *
     * @param authToken The auth token.
     */
    public void setAuthToken(final String authToken) {
        ngrokProcess.setAuthToken(authToken);
    }

    /**
     * Update <code>ngrok</code>, if an update is available.
     */
    public void update() {
        ngrokProcess.update();
    }

    /**
     * Get the <code>ngrok</code> and <code>java-ngrok</code> version.
     *
     * @return The versions.
     */
    public Version getVersion() {
        final String ngrokVersion = ngrokProcess.getVersion();

        // TODO: parse out java-ngrok version from POM
        String javaNgrokVersion = null;

        // new Version(ngrokVersion, javaNgrokVersion);

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
