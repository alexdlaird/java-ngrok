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
import com.github.alexdlaird.ngrok.protocol.BindTls;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Proto;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import com.github.alexdlaird.ngrok.protocol.Version;

import java.util.Collections;
import java.util.logging.Logger;

import static java.util.Objects.isNull;

/**
 * A client for interacting with <a href="https://ngrok.com/docs">ngrok</a>, its binary, and its APIs.
 * Can be configured with {@link JavaNgrokConfig}.
 */
public class NgrokClient {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(NgrokClient.class));

    private static final String VERSION = "0.2.0";

    private final JavaNgrokConfig javaNgrokConfig;
    private final NgrokInstaller ngrokInstaller;
    private final NgrokProcess ngrokProcess;
    private final HttpClient httpClient;

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
     * <p><code>ngrok</code>'s default behavior for <code>http</code> when no additional properties are passed is to
     * open <em>two</em> tunnels, one <code>http</code> and one <code>https</code>. This method will return a
     * reference to the <code>http</code> tunnel in this case. If only a single tunnel is needed, call
     * {@link CreateTunnel.Builder#withBindTls(boolean)} with <code>true</code> and a reference to the
     * <code>https</code> tunnel will be returned.</p>
     *
     * @param createTunnel The tunnel definition.
     * @return The created Tunnel.
     */
    public Tunnel connect(final CreateTunnel createTunnel) {
        ngrokProcess.start();

        // TODO: If a "java-ngrok-default" tunnel definition exists in the ngrok config, use that

        // TODO: Use a tunnel definition for the given name, if it exists

        LOGGER.info(String.format("Opening tunnel named: %s", createTunnel.getName()));

        final Response<Tunnel> response;
        try {
            response = httpClient.post(String.format("%s/api/tunnels", ngrokProcess.getApiUrl()), createTunnel, Collections.emptyList(), Collections.emptyMap(), Tunnel.class);
        } catch (HttpClientException e) {
            throw new JavaNgrokHTTPException(String.format("An error occurred when POSTing to create the tunnel %s.", createTunnel.getName()),
                    e, e.getUrl(), e.getStatusCode(), e.getBody());
        }

        final Tunnel tunnel;
        if (createTunnel.getProto() == Proto.HTTP && createTunnel.getBindTls() == BindTls.BOTH) {
            try {
                final Response<Tunnel> getResponse = httpClient.get(ngrokProcess.getApiUrl() + response.getBody().getUri() + "%20%28http%29", Collections.emptyList(), Collections.emptyMap(), Tunnel.class);
                tunnel = getResponse.getBody();
            } catch (HttpClientException e) {
                throw new JavaNgrokHTTPException(String.format("An error occurred when GETing the HTTP tunnel %s.", response.getBody().getName()),
                        e, e.getUrl(), e.getStatusCode(), e.getBody());
            }
        } else {
            tunnel = response.getBody();
        }

        return tunnel;
    }

    /**
     * See {@link #connect(CreateTunnel)}.
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
        // If ngrok is not running, there are no tunnels to disconnect
        if (!ngrokProcess.isRunning()) {
            return;
        }

        final Tunnels tunnels = getTunnels();
        Tunnel tunnel = null;
        // TODO: cache active tunnels so we can first check that before falling back to an API request
        for (final Tunnel t : tunnels.getTunnels()) {
            if (t.getPublicUrl().equals(publicUrl)) {
                tunnel = t;
                break;
            }
        }

        if (isNull(tunnel)) {
            return;
        }

        ngrokProcess.start();

        LOGGER.info(String.format("Disconnecting tunnel: %s", tunnel.getPublicUrl()));

        try {
            httpClient.delete(ngrokProcess.getApiUrl() + tunnel.getUri(), Collections.emptyList(), Collections.emptyMap(), Object.class);
        } catch (HttpClientException e) {
            throw new JavaNgrokHTTPException(String.format("An error occurred when DELETing the tunnel %s.", publicUrl),
                    e, e.getUrl(), e.getStatusCode(), e.getBody());
        }
    }

    /**
     * Get a list of active <code>ngrok</code> tunnels.
     *
     * @return The active <code>ngrok</code> tunnels.
     */
    public Tunnels getTunnels() {
        ngrokProcess.start();

        try {
            final Response<Tunnels> response = httpClient.get(String.format("%s/api/tunnels", ngrokProcess.getApiUrl()), Collections.emptyList(), Collections.emptyMap(), Tunnels.class);

            return response.getBody();
        } catch (HttpClientException e) {
            throw new JavaNgrokHTTPException("An error occurred when GETing the tunnels.", e, e.getUrl(),
                    e.getStatusCode(), e.getBody());
        }
    }

    // TODO: implement a refreshMetrics method that gets the latest metrics for a given Tunnel

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

        return new Version(ngrokVersion, VERSION);
    }

    /**
     * Get the <code>java-ngrok</code> to use when interacting with the <code>ngrok</code> binary.
     */
    public JavaNgrokConfig getJavaNgrokConfig() {
        return javaNgrokConfig;
    }

    /**
     * Get the class used to download and install <code>ngrok</code>.
     */
    public NgrokInstaller getNgrokInstaller() {
        return ngrokInstaller;
    }

    /**
     * Get the class used to manage the <code>ngrok</code> binary.
     */
    public NgrokProcess getNgrokProcess() {
        return ngrokProcess;
    }

    /**
     * Get the class used to make HTTP requests to <code>ngrok</code>'s APIs.
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Builder for a {@link NgrokClient}.
     */
    public static class Builder {

        private JavaNgrokConfig javaNgrokConfig;
        private NgrokInstaller ngrokInstaller;
        private NgrokProcess ngrokProcess;
        private HttpClient httpClient;

        /**
         * The <code>java-ngrok</code> to use when interacting with the <code>ngrok</code> binary.
         */
        public Builder withJavaNgrokConfig(final JavaNgrokConfig javaNgrokConfig) {
            this.javaNgrokConfig = javaNgrokConfig;
            return this;
        }

        /**
         * The class used to download and install <code>ngrok</code>.
         */
        public Builder withNgrokInstaller(final NgrokInstaller ngrokInstaller) {
            this.ngrokInstaller = ngrokInstaller;
            return this;
        }

        /**
         * The class used to manage the <code>ngrok</code> binary.
         */
        public Builder withNgrokProcess(final NgrokProcess ngrokProcess) {
            this.ngrokProcess = ngrokProcess;
            return this;
        }

        /**
         * The class used to make HTTP requests to <code>ngrok</code>'s APIs.
         */
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
                httpClient = new DefaultHttpClient.Builder().build();
            }

            return new NgrokClient(this);
        }
    }
}
