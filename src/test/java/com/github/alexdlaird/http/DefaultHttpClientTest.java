/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.http;

import static com.github.alexdlaird.util.StringUtils.isNotBlank;
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.alexdlaird.exception.JavaNgrokHTTPException;
import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.NgrokTestCase;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.protocol.CapturedRequest;
import com.github.alexdlaird.ngrok.protocol.CapturedRequests;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultHttpClientTest extends NgrokTestCase {

    private DefaultHttpClient defaultHttpClient;

    @BeforeEach
    public void setUp() {
        super.setUp();

        defaultHttpClient = spy(new DefaultHttpClient.Builder()
                .withRetryCount(3)
                .build());
    }

    @Test
    public void testPost() {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        ngrokProcessV3.start();
        final CreateTunnel createTunnel = new CreateTunnel.Builder(true)
                .withNgrokVersion(NgrokVersion.V3)
                .withName("my-tunnel")
                .build();

        // WHEN
        final Response<Tunnel> postResponse = defaultHttpClient.post(String.format("%s/api/tunnels",
            ngrokProcessV3.getApiUrl()), createTunnel, Tunnel.class);

        // THEN
        assertEquals(HTTP_CREATED, postResponse.getStatusCode());
        assertEquals("my-tunnel", postResponse.getBody().getName());
    }

    @Test
    public void testGet() {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        ngrokProcessV3.start();
        final CreateTunnel createTunnel = new CreateTunnel.Builder(true)
                .withNgrokVersion(NgrokVersion.V3)
                .withName("my-tunnel")
                .withBindTls(true)
                .build();
        defaultHttpClient.post(String.format("%s/api/tunnels",
            ngrokProcessV3.getApiUrl()), createTunnel, Tunnel.class);

        // WHEN
        final Response<Tunnels> getResponse = defaultHttpClient.get(String.format("%s/api/tunnels",
            ngrokProcessV3.getApiUrl()), Tunnels.class);

        // THEN
        assertEquals(HTTP_OK, getResponse.getStatusCode());
        assertEquals("/api/tunnels", getResponse.getBody().getUri());
        assertEquals(1, getResponse.getBody().getTunnels().size());
        assertEquals("my-tunnel", getResponse.getBody().getTunnels().get(0).getName());
        assertThat(getResponse.getBodyRaw(), containsString("my-tunnel"));
        assertThat(getResponse.getBodyRaw(), containsString("/api/tunnels/"));
        assertEquals(4, getResponse.getHeaderFields().size());
        assertEquals(1, getResponse.getHeaderFields().get("Content-Type").size());
        assertEquals("application/json", getResponse.getHeaderFields().get("Content-Type").get(0));
    }

    @Test
    public void testDelete() {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        ngrokProcessV3.start();
        final CreateTunnel createTunnel = new CreateTunnel.Builder(true)
                .withNgrokVersion(NgrokVersion.V3)
                .build();
        final Tunnel tunnel = defaultHttpClient.post(String.format("%s/api/tunnels",
            ngrokProcessV3.getApiUrl()), createTunnel, Tunnel.class).getBody();

        // WHEN
        final Response<?> deleteResponse = defaultHttpClient.delete(ngrokProcessV3.getApiUrl() + tunnel.getUri());

        // THEN
        assertEquals(HTTP_NO_CONTENT, deleteResponse.getStatusCode());
        assertNull(deleteResponse.getBody());
    }

    @Test
    public void testPut() {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        ngrokProcessV3.start();
        final CreateTunnel createTunnel = new CreateTunnel.Builder(true)
                .withNgrokVersion(NgrokVersion.V3)
                .withName("my-tunnel")
                .withBindTls(true)
                .build();

        // WHEN
        final HttpClientException exception = assertThrows(HttpClientException.class, () -> defaultHttpClient.put(
            String.format("%s/api/tunnels", ngrokProcessV3.getApiUrl()), createTunnel, Tunnels.class));

        // THEN
        assertEquals(HTTP_BAD_METHOD, exception.getStatusCode());
    }

    @Test
    public void testGetWithQueryParameters() throws InterruptedException, MalformedURLException {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        ngrokProcessV3.start();
        final CreateTunnel request = new CreateTunnel.Builder(true)
                .withNgrokVersion(NgrokVersion.V3)
                .withName("my-tunnel")
                .withAddr(new URL(ngrokProcessV3.getApiUrl()).getPort())
                .withBindTls(true)
                .build();
        final Response<Tunnel> createResponse = defaultHttpClient.post(String.format("%s/api/tunnels",
            ngrokProcessV3.getApiUrl()), request, Tunnel.class);
        final String publicUrl = createResponse.getBody().getPublicUrl();

        Thread.sleep(1000);

        defaultHttpClient.get(String.format("%s/status", publicUrl), Object.class);

        Thread.sleep(3000);

        // WHEN
        final Response<CapturedRequests> response1 = defaultHttpClient.get(
            String.format("%s/api/requests/http", publicUrl), CapturedRequests.class);
        final Response<CapturedRequests> response2 = defaultHttpClient.get(
            String.format("%s/api/requests/http", publicUrl), List.of(
                new Parameter("tunnel_name", "my-tunnel")), Map.of(), CapturedRequests.class);
        final Response<CapturedRequests> response3 = defaultHttpClient.get(
            String.format("%s/api/requests/http", publicUrl), List.of(
                new Parameter("tunnel_name", "my-tunnel (http)")), Map.of(), CapturedRequests.class);

        // THEN
        assertEquals(HTTP_OK, response1.getStatusCode());
        assertThat(response1.getBody().getRequests().size(), greaterThan(0));
        assertEquals(HTTP_OK, response2.getStatusCode());
        assertThat(response2.getBody().getRequests().size(), greaterThan(0));
        assertEquals(HTTP_OK, response3.getStatusCode());
        assertEquals(response3.getBody().getRequests().size(), 0);
        final CapturedRequests capturedRequests = response1.getBody();
        assertEquals(2, capturedRequests.getRequests().size());
        assertNotNull(capturedRequests.getUri());
        final CapturedRequest capturedRequest = capturedRequests.getRequests().get(0);
        assertNotNull(capturedRequest.getRequest());
        assertNotNull(capturedRequest.getRequest().getMethod());
        assertNotNull(capturedRequest.getRequest().getUri());
        assertNotNull(capturedRequest.getRequest().getHeaders());
        assertNotNull(capturedRequest.getRequest().getProto());
        assertNotNull(capturedRequest.getRequest().getRaw());
        assertNotNull(capturedRequest.getResponse());
        assertNotNull(capturedRequest.getResponse().getStatus());
        assertEquals(0, capturedRequest.getResponse().getStatusCode());
        assertNull(capturedRequest.getResponse().getHeaders());
        assertNotNull(capturedRequest.getResponse().getProto());
        assertNull(capturedRequest.getResponse().getRaw());
        assertNotNull(capturedRequest.getUri());
        assertEquals(0, capturedRequest.getDuration());
        assertNotNull(capturedRequest.getId());
        assertNotNull(capturedRequest.getRemoteAddr());
        assertNotNull(capturedRequest.getStart());
        assertNotNull(capturedRequest.getTunnelName());
    }

    @Test
    public void testGetRetries() throws IOException, InterruptedException {
        // GIVEN
        final HttpURLConnection mockHttpUrlConnection = mock(HttpURLConnection.class);
        doReturn(mockHttpUrlConnection).when(defaultHttpClient).createHttpUrlConnection(any());
        doAnswer(invocation -> {
            throw new SocketTimeoutException("Download failed");
        }).when(mockHttpUrlConnection).getInputStream();

        // WHEN
        assertThrows(HttpClientException.class, () -> defaultHttpClient.get(
            "/some-url", List.of(), Map.of(), Paths.get("some", "path")));

        // THEN
        verify(defaultHttpClient, times(4)).get(any(), any(), any(), any(), anyInt());
        verify(defaultHttpClient, times(4)).getInputStream(any(), any(), any(), any());
    }

    @Test
    public void testGetThrowsException() throws IOException {
        // GIVEN
        doAnswer(invocation -> {
            throw new UnsupportedEncodingException("Bad input params");
        }).when(defaultHttpClient).urlWithParameters(any(), any());

        // WHEN
        assertThrows(HttpClientException.class, () -> defaultHttpClient.get(
            "/some-url", List.of(), Map.of(), Tunnel.class));
    }

    @Test
    public void testGetTunnelsThrowsException() throws IOException {
        assumeTrue(isNotBlank(System.getenv("NGROK_AUTHTOKEN")), "NGROK_AUTHTOKEN environment variable not set");

        // GIVEN
        final NgrokClient ngrokClientV3 = new NgrokClient.Builder()
                .withJavaNgrokConfig(javaNgrokConfigV3)
                .withNgrokProcess(ngrokProcessV3)
                .withHttpClient(defaultHttpClient)
                .build();
        final HttpURLConnection mockHttpUrlConnection = mock(HttpURLConnection.class);
        doReturn(mockHttpUrlConnection).when(defaultHttpClient).createHttpUrlConnection(any());
        doAnswer(invocation -> {
            throw new SocketTimeoutException("GET failed");
        }).when(mockHttpUrlConnection).getInputStream();

        // WHEN
        assertThrows(JavaNgrokHTTPException.class, () -> ngrokClientV3.getTunnels());
    }
}
