/*
 * Copyright (c) 2021-2026 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.exception.JavaNgrokHTTPException;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.http.HttpClientException;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.ConfigVersion;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import com.github.alexdlaird.ngrok.protocol.CreateEndpoint;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Endpoint;
import com.github.alexdlaird.ngrok.protocol.Upstream;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NgrokClientEndpointsTest extends NgrokTestCase {

    private NgrokClient ngrokClient;

    @BeforeEach
    public void setUp() {
        super.setUp();

        ngrokClient = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig)
                                               .withNgrokProcess(ngrokProcess)
                                               .withHttpClient(retryHttpClient)
                                               .build();
    }

    @Test
    public void testConnectEndpoint() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
        final CreateEndpoint createEndpoint = new CreateEndpoint.Builder()
            .withUpstream("http://localhost:5000")
            .build();

        // WHEN
        final Endpoint endpoint = ngrokClient.connectEndpoint(createEndpoint);

        // THEN
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
        assertNotNull(endpoint.getName());
        assertNotNull(endpoint.getUrl());
        assertThat(endpoint.getUrl(), startsWith("https://"));
        assertNotNull(endpoint.getUpstream());
        assertEquals("http://localhost:5000", endpoint.getUpstream().getUrl());
    }

    @Test
    public void testConnectEndpointWithPooling() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final CreateEndpoint createEndpoint = new CreateEndpoint.Builder()
            .withUrl("https://java-ngrok.internal")
            .withUpstream("http://localhost:80")
            .withPoolingEnabled(true)
            .build();

        // WHEN
        final Endpoint endpoint = ngrokClient.connectEndpoint(createEndpoint);

        // THEN
        assertEquals("https://java-ngrok.internal", endpoint.getUrl());
    }

    @Test
    public void testConnectEndpointWithTrafficPolicy() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final Map<String, Object> trafficPolicy = Map.of(
            "on_http_request", List.of(Map.of("actions", List.of(Map.of("type", "deny")))));
        final CreateEndpoint createEndpoint = new CreateEndpoint.Builder()
            .withUpstream("http://localhost:5000")
            .withTrafficPolicy(trafficPolicy)
            .build();

        // WHEN
        final Endpoint endpoint = ngrokClient.connectEndpoint(createEndpoint);

        // THEN
        assertNotNull(endpoint.getUrl());
    }

    @Test
    public void testGetEndpoints()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final Endpoint endpoint = ngrokClient.connectEndpoint(new CreateEndpoint.Builder()
            .withName("get-endpoints-test")
            .withUpstream("http://localhost:5000")
            .build());
        Thread.sleep(1000);

        // WHEN
        final List<Endpoint> endpoints = ngrokClient.getEndpoints();

        // THEN
        assertEquals(1, endpoints.size());
        assertEquals(endpoint.getName(), endpoints.get(0).getName());
    }

    @Test
    public void testDisconnectEndpoint()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final Endpoint endpoint = ngrokClient.connectEndpoint(new CreateEndpoint.Builder()
            .withName("disconnect-endpoint-test")
            .withUpstream("http://localhost:5000")
            .build());
        assertTrue(ngrokClient.getNgrokProcess().isRunning());
        Thread.sleep(1000);

        // WHEN
        ngrokClient.disconnectEndpoint(endpoint.getName());

        // THEN
        final List<Endpoint> endpoints = ngrokClient.getEndpoints();
        assertEquals(0, endpoints.size());
    }

    @Test
    public void testKillClearsEndpointsAndTunnels()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokClient.connect(new CreateTunnel.Builder().withName("kill-tunnel").build());
        ngrokClient.connectEndpoint(new CreateEndpoint.Builder()
            .withUpstream("http://localhost:5000")
            .build());

        // WHEN
        ngrokClient.kill();
        Thread.sleep(1000);

        // THEN
        assertFalse(ngrokClient.getNgrokProcess().isRunning());
    }

    @Test
    public void testRefreshMetricsEndpoint()
        throws InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokClient.getNgrokProcess().start();
        final Endpoint endpoint = ngrokClient.connectEndpoint(new CreateEndpoint.Builder()
            .withName("refresh-metrics-test")
            .withUpstream("http://localhost:5000")
            .build());
        Thread.sleep(1000);

        // WHEN
        ngrokClient.refreshMetrics(endpoint);

        // THEN
        assertNotNull(endpoint.getMetrics());
        assertThat(endpoint.getMetrics().size(), greaterThan(0));
    }

    @Test
    public void testEndpointDefinitions() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final Map<String, Object> config = Map.of(
            "endpoints", List.of(Map.of(
                "name", "http-endpoint",
                "upstream", Map.of("url", "http://localhost:8000"))));

        final Path configPath2 = Path.of(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfig.getNgrokVersion(),
            ConfigVersion.V3);
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig).withConfigPath(
            configPath2).build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcess2)
                                                                  .build();

        // WHEN
        final CreateEndpoint createEndpoint = new CreateEndpoint.Builder()
            .withName("http-endpoint")
            .withUpstream("http://localhost:8000")
            .build();
        final Endpoint httpEndpoint = ngrokClient2.connectEndpoint(createEndpoint);

        // THEN
        assertEquals("http-endpoint-api", httpEndpoint.getName());
        assertNotNull(httpEndpoint.getUpstream());
        assertEquals("http://localhost:8000", httpEndpoint.getUpstream().getUrl());
    }

    @Test
    public void testEndpointDefinitionsJavaNgrokDefault() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final Map<String, Object> config = Map.of(
            "endpoints", List.of(Map.of(
                "name", "java-ngrok-default",
                "upstream", Map.of("url", "http://localhost:8080"))));

        final Path configPath2 = Path.of(javaNgrokConfig.getConfigPath().getParent().toString(), "config2.yml");
        ngrokInstaller.installDefaultConfig(configPath2, config, javaNgrokConfig.getNgrokVersion(),
            ConfigVersion.V3);
        final JavaNgrokConfig javaNgrokConfig2 = new JavaNgrokConfig.Builder(javaNgrokConfig).withConfigPath(
            configPath2).build();
        ngrokProcess2 = new NgrokProcess(javaNgrokConfig2, ngrokInstaller);
        final NgrokClient ngrokClient2 = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig2)
                                                                  .withNgrokProcess(ngrokProcess2)
                                                                  .build();

        // WHEN
        final Endpoint endpoint = ngrokClient2.connectEndpoint(new CreateEndpoint.Builder()
            .withUpstream("http://localhost:8080")
            .build());

        // THEN
        assertEquals("java-ngrok-default-api", endpoint.getName());
    }

    @Test
    public void testNgrokConnectEndpointHttpClientFails() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpClientException httpClientException = new HttpClientException("some message",
            new SocketTimeoutException(), "http://localhost:4040/api/endpoints", 500, "error body");
        final NgrokClient mockNgrokClient = new NgrokClient.Builder().withJavaNgrokConfig(javaNgrokConfig)
                                                                     .withNgrokProcess(ngrokProcess)
                                                                     .withHttpClient(httpClient)
                                                                     .build();
        when(httpClient.post(any(), any(), any())).thenThrow(httpClientException);

        // WHEN
        final JavaNgrokHTTPException javaNgrokHTTPException = assertThrows(JavaNgrokHTTPException.class,
            () -> mockNgrokClient.connectEndpoint(new CreateEndpoint.Builder()
                .withUpstream("http://localhost:5000").build()));

        // THEN
        assertThat(javaNgrokHTTPException.getMessage(),
            startsWith("An error occurred when POSTing to create the endpoint"));
        assertEquals("http://localhost:4040/api/endpoints", javaNgrokHTTPException.getUrl());
        assertEquals(500, javaNgrokHTTPException.getStatusCode());
        assertEquals("error body", javaNgrokHTTPException.getBody());
    }

    @Test
    public void testEndpointHttpException404HintsUpdate() {
        // GIVEN
        final JavaNgrokHTTPException exception = new JavaNgrokHTTPException(
            "An error occurred when POSTing to create the endpoint foo.",
            new RuntimeException(),
            "http://localhost:4040/api/endpoints",
            404,
            "404 page not found");

        // WHEN
        final String message = exception.getMessage();

        // THEN
        assertThat(message, containsString("NgrokClient.update()"));
        assertThat(message, containsString("Endpoints support"));
    }

    @Test
    public void testEndpointHttpExceptionNon404DoesNotHint() {
        // GIVEN
        final JavaNgrokHTTPException exception = new JavaNgrokHTTPException(
            "An error occurred.",
            new RuntimeException(),
            "http://localhost:4040/api/endpoints",
            500,
            "internal error");

        // WHEN
        final String message = exception.getMessage();

        // THEN
        assertFalse(message.contains("NgrokClient.update()"));
    }

    @Test
    public void testTunnelHttpException404DoesNotHint() {
        // GIVEN
        final JavaNgrokHTTPException exception = new JavaNgrokHTTPException(
            "An error occurred.",
            new RuntimeException(),
            "http://localhost:4040/api/tunnels",
            404,
            "404 page not found");

        // WHEN
        final String message = exception.getMessage();

        // THEN
        assertFalse(message.contains("NgrokClient.update()"));
    }

    @Test
    public void testUpstreamPojoFromBuilder() {
        // WHEN
        final Upstream upstream = new Upstream.Builder()
            .withUrl("http://localhost:8000")
            .withProtocol("http2")
            .build();

        // THEN
        assertEquals("http://localhost:8000", upstream.getUrl());
        assertEquals("http2", upstream.getProtocol());
    }
}
