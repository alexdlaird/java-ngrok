/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.exception.JavaNgrokHTTPException;
import com.github.alexdlaird.exception.JavaNgrokSecurityException;
import com.github.alexdlaird.exception.NgrokException;
import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.http.HttpClientException;
import com.github.alexdlaird.http.Response;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.conf.JavaNgrokVersion;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import com.github.alexdlaird.ngrok.protocol.ApiResponse;
import com.github.alexdlaird.ngrok.protocol.BindTls;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Proto;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import com.github.alexdlaird.ngrok.protocol.Version;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.alexdlaird.util.ProcessUtils.captureRunProcess;
import static com.github.alexdlaird.util.StringUtils.isBlank;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A client for interacting with <a href="https://ngrok.com/docs" target="_blank">ngrok</a>, its binary, and its APIs.
 * Can be configured with {@link JavaNgrokConfig}.
 *
 * <p>For usage examples, see
 * <a href="https://alexdlaird.github.io/java-ngrok/" target="_blank"><code>java-ngrok</code>'s documentation</a>.
 */
public class NgrokClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NgrokClient.class);

    private final Map<String, Tunnel> currentTunnels = new HashMap<>();

    private final String javaNgrokVersion;
    private final JavaNgrokConfig javaNgrokConfig;
    private final NgrokProcess ngrokProcess;
    private final HttpClient httpClient;

    private NgrokClient(final Builder builder) {
        this.javaNgrokVersion = builder.javaNgrokVersion;
        this.javaNgrokConfig = builder.javaNgrokConfig;
        this.ngrokProcess = builder.ngrokProcess;
        this.httpClient = builder.httpClient;
    }

    /**
     * Establish a new <code>ngrok</code> tunnel for the Tunnel creation request, returning an object representing the
     * connected tunnel.
     *
     * <p>If a <a href="https://ngrok.com/docs/agent/config/v2/#tunnel-configurations"
     * target="_blank">tunnel definition in ngrok's config file</a> matches the given
     * {@link CreateTunnel.Builder#withName(String)}, it will be loaded and used to start the tunnel. When
     * {@link CreateTunnel.Builder#withName(String)} is not set and a "java-ngrok-default" tunnel definition exists in
     * <code>ngrok</code>'s config, it will be loaded and used. Any properties defined on {@link CreateTunnel} will
     * override properties from the loaded tunnel definition.
     *
     * <p>If <code>ngrok</code> is not installed at {@link JavaNgrokConfig}'s <code>ngrokPath</code>, calling this
     * method will first download and install <code>ngrok</code>.
     *
     * <p><code>java-ngrok</code> is compatible with <code>ngrok</code> v2 and v3, but by default it will install v2.
     * To install v3 instead, set the version with {@link JavaNgrokConfig.Builder#withNgrokVersion(NgrokVersion)} and
     * {@link CreateTunnel.Builder#withNgrokVersion(NgrokVersion)}.
     *
     * <p>If <code>ngrok</code> is not running, calling this method will first start a process with
     * {@link JavaNgrokConfig}.
     *
     * <p><strong>Note:</strong> <code>ngrok</code> v2's default behavior for <code>http</code> when no additional
     * properties are passed is to open <em>two</em> tunnels, one <code>http</code> and one <code>https</code>. This
     * method will return a reference to the <code>http</code> tunnel in this case. If only a single tunnel is needed,
     * call {@link CreateTunnel.Builder#withBindTls(BindTls)} with {@link BindTls#TRUE} and a reference to the
     * <code>https</code> tunnel will be returned.
     *
     * @param createTunnel The tunnel definition.
     * @return The created Tunnel.
     * @throws JavaNgrokException         The tunnel definition was invalid, or response was incompatible with
     *                                    <code>java-ngrok</code>.
     * @throws JavaNgrokHTTPException     An HTTP error occurred communicating with the <code>ngrok</code> API.
     * @throws JavaNgrokSecurityException The URL was not supported.
     */
    public Tunnel connect(final CreateTunnel createTunnel) {
        ngrokProcess.start();

        final CreateTunnel finalTunnel = interpolateTunnelDefinition(createTunnel);

        LOGGER.info("Opening tunnel named: {}", finalTunnel.getName());

        final Response<Tunnel> response;
        try {
            response = httpClient.post(String.format("%s/api/tunnels", ngrokProcess.getApiUrl()), finalTunnel,
                Tunnel.class);
        } catch (final HttpClientException e) {
            throw new JavaNgrokHTTPException(String.format("An error occurred when POSTing to create the tunnel %s.",
                finalTunnel.getName()), e, e.getUrl(), e.getStatusCode(), e.getBody());
        }

        final Tunnel tunnel;
        if (javaNgrokConfig.getNgrokVersion() == NgrokVersion.V2
            && finalTunnel.getProto() == Proto.HTTP
            && finalTunnel.getBindTls() == BindTls.BOTH) {
            try {
                final Response<Tunnel> getResponse = httpClient.get(ngrokProcess.getApiUrl()
                                                                    + response.getBody().getUri() + "%20%28http%29",
                    Tunnel.class);
                tunnel = getResponse.getBody();

                LOGGER.info("ngrok v2 opens multiple tunnels, fetching just HTTP tunnel {} for return",
                    tunnel.getId());
            } catch (final HttpClientException e) {
                throw new JavaNgrokHTTPException(String.format("An error occurred when GETing the HTTP tunnel %s.",
                    response.getBody().getName()), e, e.getUrl(), e.getStatusCode(), e.getBody());
            }
        } else {
            tunnel = response.getBody();
        }

        applyEdgeToTunnel(tunnel);

        currentTunnels.put(tunnel.getPublicUrl(), tunnel);

        return tunnel;
    }

    /**
     * See {@link #connect(CreateTunnel)}.
     */
    public Tunnel connect() {
        return connect(new CreateTunnel.Builder().withNgrokVersion(javaNgrokConfig.getNgrokVersion()).build());
    }

    /**
     * Disconnect the <code>ngrok</code> tunnel for the given URL, if open.
     *
     * <p>If <code>ngrok</code> is not running, calling this method will first start a process with
     * {@link JavaNgrokConfig}.
     *
     * @param publicUrl The public URL of the tunnel to disconnect.
     * @throws JavaNgrokHTTPException     An HTTP error occurred communicating with the <code>ngrok</code> API.
     * @throws JavaNgrokSecurityException The URL was not supported.
     */
    public void disconnect(final String publicUrl) {
        // If ngrok is not running, there are no tunnels to disconnect
        if (!ngrokProcess.isRunning()) {
            LOGGER.trace("\"ngrokPath\" {} is not running a process", javaNgrokConfig.getNgrokPath());

            return;
        }

        if (!currentTunnels.containsKey(publicUrl)) {
            getTunnels();

            // One more check, if the given URL is still not in the list of tunnels, it is not active
            if (!currentTunnels.containsKey(publicUrl)) {
                return;
            }
        }

        final Tunnel tunnel = currentTunnels.get(publicUrl);

        ngrokProcess.start();

        LOGGER.info("Disconnecting tunnel: {}", tunnel.getPublicUrl());

        try {
            httpClient.delete(ngrokProcess.getApiUrl() + tunnel.getUri());
        } catch (final HttpClientException e) {
            throw new JavaNgrokHTTPException(String.format("An error occurred when DELETing the tunnel %s.",
                publicUrl), e, e.getUrl(), e.getStatusCode(), e.getBody());
        }
    }

    /**
     * Get a list of active <code>ngrok</code> tunnels.
     *
     * <p>If <code>ngrok</code> is not running, calling this method will first start a process with
     * {@link JavaNgrokConfig}.
     *
     * @return The active <code>ngrok</code> tunnels.
     * @throws JavaNgrokException         The response was invalid or not compatible with <code>java-ngrok</code>.
     * @throws JavaNgrokHTTPException     An HTTP error occurred communicating with the <code>ngrok</code> API.
     * @throws JavaNgrokSecurityException The URL was not supported.
     */
    public List<Tunnel> getTunnels() {
        ngrokProcess.start();

        try {
            final Response<Tunnels> response = httpClient.get(String.format("%s/api/tunnels",
                ngrokProcess.getApiUrl()), Tunnels.class);

            currentTunnels.clear();
            for (final Tunnel tunnel : response.getBody().getTunnels()) {
                applyEdgeToTunnel(tunnel);
                currentTunnels.put(tunnel.getPublicUrl(), tunnel);
            }

            final List<Tunnel> sortedTunnels = new ArrayList<>(currentTunnels.values());
            sortedTunnels.sort(Comparator.comparing(Tunnel::getProto));
            return List.of(sortedTunnels.toArray(new Tunnel[]{}));
        } catch (final HttpClientException e) {
            throw new JavaNgrokHTTPException("An error occurred when GETing the tunnels.", e, e.getUrl(),
                e.getStatusCode(), e.getBody());
        }
    }

    /**
     * Get the latest metrics for the given {@link Tunnel} and update its <code>metrics</code> attribute.
     *
     * @param tunnel The Tunnel to update.
     * @throws JavaNgrokException         The API did not return <code>metrics</code>.
     * @throws JavaNgrokSecurityException The URL was not supported.
     */
    public void refreshMetrics(final Tunnel tunnel) {
        Response<Tunnel> latestTunnel = httpClient.get(String.format("%s%s", ngrokProcess.getApiUrl(),
            tunnel.getUri()), Tunnel.class);

        if (isNull(latestTunnel.getBody().getMetrics()) || latestTunnel.getBody().getMetrics().isEmpty()) {
            throw new JavaNgrokException("The ngrok API did not return \"metrics\" in the response");
        }

        tunnel.setMetrics(latestTunnel.getBody().getMetrics());
    }

    /**
     * Terminate the <code>ngrok</code> processes, if running. This method will not block, it will just issue a kill
     * request.
     */
    public void kill() {
        ngrokProcess.stop();

        currentTunnels.clear();
    }

    /**
     * Set the <code>ngrok</code> auth token in the config file to streamline access to more features (for instance,
     * multiple concurrent tunnels, custom domains, etc.).
     *
     * <p>The auth token can also be set in the {@link JavaNgrokConfig} that is passed to the
     * {@link NgrokClient.Builder}, or use the environment variable <code>NGROK_AUTHTOKEN</code>.
     *
     * <pre>
     * // Setting an auth token allows you to do things like open multiple tunnels at the same time
     * final NgrokClient ngrokClient = new NgrokClient.Builder().build();
     * ngrokClient.setAuthToken("&lt;NGROK_AUTHTOKEN&gt;")
     *
     * // &lt;NgrokTunnel: "https://&lt;public_sub1&gt;.ngrok.io" -&gt; "http://localhost:80"&gt;
     * final Tunnel ngrokTunnel1 = ngrokClient.connect();
     * // &lt;NgrokTunnel: "https://&lt;public_sub2&gt;.ngrok.io" -&gt; "http://localhost:8000"&gt;
     * final CreateTunnel sshCreateTunnel = new CreateTunnel.Builder()
     *         .withAddr(8000)
     *         .build();
     * final Tunnel ngrokTunnel2 = ngrokClient.connect(createTunnel);
     * </pre>
     *
     * @param authToken The auth token.
     */
    public void setAuthToken(final String authToken) {
        ngrokProcess.setAuthToken(authToken);
    }

    /**
     * Set the <code>ngrok</code> API key in the config file to enable access to more features (for instance,
     * <a href="https://ngrok.com/docs/universal-gateway/internal-endpoints/">Internal Endpoints</a>).
     *
     * <p>The API key can also be set in the {@link JavaNgrokConfig} that is passed to the
     * {@link NgrokClient.Builder}, or use the environment variable <code>NGROK_API_KEY</code>.
     *
     * <pre>
     * // Setting an API key allows you to use things like Internal Endpoints
     * final NgrokClient ngrokClient = new NgrokClient.Builder().build();
     * ngrokClient.setApiKey("&lt;NGROK_API_KEY&gt;")
     *
     * // &lt;NgrokTunnel: "tls://some-endpoint.internal" -&gt; "localhost:9000"&gt;
     * final CreateTunnel createInternalEndpoint = new CreateTunnel.Builder()
     *     .withAddr("9000")
     *     .withProto(Proto.TLS)
     *     .withDomain("some-endpoint.internal")
     *     .withPoolingEnabled(true)
     *     .build();
     * final Tunnel internalEndpoint = ngrokClient.connect(createInternalEndpoint);
     * </pre>
     *
     * @param apiKey The API key.
     */
    public void setApiKey(final String apiKey) {
        ngrokProcess.setApiKey(apiKey);
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

        return new Version(ngrokVersion, javaNgrokVersion);
    }

    /**
     * Run a <code>ngrok</code> command against the <code>api</code> with the given args. This will use the local agent
     * to run a remote API request for <code>ngrok</code>, which requires that an API key has been set. For a list of
     * available commands, pass <code>List.of("--help")</code>.
     *
     * @param args The args to pass to the <code>api</code> command.
     * @return The response from executing the <code>api</code> command.
     * @throws NgrokException       The <code>ngrok</code> process exited with an error.
     * @throws IOException          An I/O exception occurred.
     * @throws InterruptedException The thread was interrupted during execution.
     */
    public ApiResponse api(final List<String> args)
        throws IOException, InterruptedException {
        final List<String> cmdArgs = new ArrayList<>();
        if (nonNull(javaNgrokConfig.getConfigPath())) {
            cmdArgs.add("--config");
            cmdArgs.add(javaNgrokConfig.getConfigPath().toString());
        }
        cmdArgs.add("api");
        if (nonNull(javaNgrokConfig.getApiKey())) {
            cmdArgs.add("--api-key");
            cmdArgs.add(javaNgrokConfig.getApiKey());
        }
        cmdArgs.addAll(args);

        LOGGER.info("Executing \"ngrok api\" command with args: {}", args);

        return ApiResponse.fromBody(captureRunProcess(javaNgrokConfig.getNgrokPath(), cmdArgs));
    }

    /**
     * Get the <code>java-ngrok</code> to use when interacting with the <code>ngrok</code> binary.
     */
    public JavaNgrokConfig getJavaNgrokConfig() {
        return javaNgrokConfig;
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

    @Deprecated
    private void applyEdgeToTunnel(final Tunnel tunnel) {
        if ((isNull(tunnel.getPublicUrl()) || tunnel.getPublicUrl().isEmpty())
            && nonNull(javaNgrokConfig.getApiKey()) && nonNull(tunnel.getId())) {
            final Map<String, String> ngrokApiHeaders = Map.of(
                "Authorization", String.format("Bearer %s", javaNgrokConfig.getApiKey()),
                "Ngrok-Version", "2");
            final Response<Map> tunnelResponse = httpClient.get(String.format("https://api.ngrok.com/tunnels/%s",
                tunnel.getId()), List.of(), ngrokApiHeaders, Map.class);

            if (!tunnelResponse.getBody().containsKey("labels")
                || !(tunnelResponse.getBody().get("labels") instanceof Map)
                || !((Map) tunnelResponse.getBody().get("labels")).containsKey("edge")) {
                throw new JavaNgrokException(String.format("Tunnel %s does not have 'labels', use a Tunnel "
                                                           + "configured on an Edge.", tunnel.getId()));
            }

            final String edge = (String) ((Map) tunnelResponse.getBody().get("labels")).get("edge");
            final String edgesPrefix;
            if (edge.startsWith("edghts_")) {
                edgesPrefix = "https";
            } else if (edge.startsWith("edgtcp")) {
                edgesPrefix = "tcp";
            } else if (edge.startsWith("edgtls")) {
                edgesPrefix = "tls";
            } else {
                throw new JavaNgrokException(String.format("Unknown Edge prefix: %s.", edge));
            }

            LOGGER.info("Applying edge {} to tunnel {}", edge, tunnel.getId());

            final Response<Map> edgeResponse = httpClient.get(String.format("https://api.ngrok.com/edges/%s/%s",
                edgesPrefix, edge), List.of(), ngrokApiHeaders, Map.class);

            if (!edgeResponse.getBody().containsKey("hostports")
                || !(edgeResponse.getBody().get("hostports") instanceof List)
                || ((List) edgeResponse.getBody().get("hostports")).isEmpty()) {
                throw new JavaNgrokException(String.format("No Endpoint is attached to your Edge %s, "
                                                           + "login to the ngrok dashboard to attach an Endpoint to "
                                                           + "your Edge first.",
                    edge));
            }

            tunnel.setPublicUrl(String.format("%s://%s", edgesPrefix,
                ((List) edgeResponse.getBody().get("hostports")).get(0)));
            tunnel.setProto(edgesPrefix);

            LOGGER.warn("ngrok has deprecated Edges and will sunset Labeled Tunnels on December 31st, 2025. "
                        + "See https://github.com/alexdlaird/java-ngrok/issues/158 for more details.");
        }
    }

    private synchronized CreateTunnel interpolateTunnelDefinition(final CreateTunnel createTunnel) {
        final CreateTunnel.Builder createTunnelBuilder = new CreateTunnel.Builder(createTunnel);

        final Map<String, Object> config;
        if (Files.exists(javaNgrokConfig.getConfigPath())) {
            config = ngrokProcess.getNgrokInstaller().getNgrokConfig(javaNgrokConfig.getConfigPath());
        } else {
            config = ngrokProcess.getNgrokInstaller().getDefaultConfig(javaNgrokConfig.getNgrokVersion(),
                javaNgrokConfig.getConfigVersion());
        }

        final String name;
        final Map<String, Object> tunnelDefinitions = (Map<String, Object>) config.getOrDefault("tunnels", Map.of());
        if (isNull(createTunnel.getName()) && tunnelDefinitions.containsKey("java-ngrok-default")) {
            LOGGER.info("java-ngrok-default found defined in config, using for tunnel definition");

            name = "java-ngrok-default";
            createTunnelBuilder.withName(name);
        } else {
            name = createTunnel.getName();
        }

        if (nonNull(name) && tunnelDefinitions.containsKey(name)) {
            if (((Map<String, Object>) tunnelDefinitions.get(name)).containsKey("labels")
                && isBlank(javaNgrokConfig.getApiKey())) {
                throw new JavaNgrokException("'JavaNgrokConfig.apiKey' must be set when 'labels' is "
                                             + "on the tunnel definition.");
            }

            createTunnelBuilder.withTunnelDefinition((Map<String, Object>) tunnelDefinitions.get(name));
            createTunnelBuilder.withName(String.format("%s-api", name));
        }

        return createTunnelBuilder.build();
    }

    /**
     * Builder for a {@link NgrokClient}, see docs for that class for example usage.
     */
    public static class Builder {

        private String javaNgrokVersion;
        private JavaNgrokConfig javaNgrokConfig;
        private NgrokInstaller ngrokInstaller;
        private NgrokProcess ngrokProcess;
        private HttpClient httpClient;

        /**
         * The <code>java-ngrok</code> to use when interacting with the <code>ngrok</code> binary.
         */
        public Builder withJavaNgrokConfig(final JavaNgrokConfig javaNgrokConfig) {
            this.javaNgrokConfig = Objects.requireNonNull(javaNgrokConfig);
            return this;
        }

        /**
         * The class used to download and install <code>ngrok</code>. Only needed if
         * {@link #withNgrokProcess(NgrokProcess)} is not called.
         */
        public Builder withNgrokInstaller(final NgrokInstaller ngrokInstaller) {
            this.ngrokInstaller = Objects.requireNonNull(ngrokInstaller);
            return this;
        }

        /**
         * The class used to manage the <code>ngrok</code> binary.
         */
        public Builder withNgrokProcess(final NgrokProcess ngrokProcess) {
            this.ngrokProcess = Objects.requireNonNull(ngrokProcess);
            return this;
        }

        /**
         * The class used to make HTTP requests to <code>ngrok</code>'s APIs.
         */
        public Builder withHttpClient(final HttpClient httpClient) {
            this.httpClient = Objects.requireNonNull(httpClient);
            return this;
        }

        /**
         * Build the {@link NgrokClient}.
         */
        public NgrokClient build() {
            javaNgrokVersion = JavaNgrokVersion.getInstance().getVersion();

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
