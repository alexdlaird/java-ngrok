/*
 * Copyright (c) 2022 Alex Laird
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

package com.github.alexdlaird.http;

import com.github.alexdlaird.ngrok.NgrokTestCase;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.protocol.CapturedRequest;
import com.github.alexdlaird.ngrok.protocol.CapturedRequests;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

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

class DefaultHttpClientTest extends NgrokTestCase {

    private DefaultHttpClient defaultHttpClient;

    @BeforeEach
    public void setUp() {
        super.setUp();

        ngrokProcessV2.start();

        defaultHttpClient = new DefaultHttpClient.Builder().build();
    }

    @Test
    public void testPost() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder(true)
                .withName("my-tunnel")
                .build(NgrokVersion.V2);

        // WHEN
        final Response<Tunnel> postResponse = defaultHttpClient.post(String.format("%s/api/tunnels", ngrokProcessV2.getApiUrl()), createTunnel, Tunnel.class);

        // THEN
        assertEquals(HTTP_CREATED, postResponse.getStatusCode());
        assertEquals("my-tunnel", postResponse.getBody().getName());
    }

    @Test
    public void testGet() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder(true)
                .withName("my-tunnel")
                .withBindTls(true)
                .build(NgrokVersion.V2);
        defaultHttpClient.post(String.format("%s/api/tunnels", ngrokProcessV2.getApiUrl()), createTunnel, Tunnel.class);

        // WHEN
        final Response<Tunnels> getResponse = defaultHttpClient.get(String.format("%s/api/tunnels", ngrokProcessV2.getApiUrl()), Tunnels.class);

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
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder(true)
                .build(NgrokVersion.V2);
        final Tunnel tunnel = defaultHttpClient.post(String.format("%s/api/tunnels", ngrokProcessV2.getApiUrl()), createTunnel, Tunnel.class).getBody();

        // WHEN
        final Response<?> deleteResponse = defaultHttpClient.delete(ngrokProcessV2.getApiUrl() + tunnel.getUri());

        // THEN
        assertEquals(HTTP_NO_CONTENT, deleteResponse.getStatusCode());
        assertNull(deleteResponse.getBody());
    }

    @Test
    public void testPut() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder(true)
                .withName("my-tunnel")
                .withBindTls(true)
                .build(NgrokVersion.V2);

        // WHEN
        final HttpClientException exception = assertThrows(HttpClientException.class, () -> defaultHttpClient.put(String.format("%s/api/tunnels", ngrokProcessV2.getApiUrl()), createTunnel, Tunnels.class));

        // THEN
        assertEquals(HTTP_BAD_METHOD, exception.getStatusCode());
    }

    @Test
    public void testGetWithQueryParameters() throws InterruptedException, MalformedURLException {
        // GIVEN
        final CreateTunnel request = new CreateTunnel.Builder(true)
                .withName("my-tunnel")
                .withAddr(new URL(ngrokProcessV2.getApiUrl()).getPort())
                .withBindTls(true)
                .build(NgrokVersion.V2);
        final Response<Tunnel> createResponse = defaultHttpClient.post(String.format("%s/api/tunnels", ngrokProcessV2.getApiUrl()), request, Tunnel.class);
        final String publicUrl = createResponse.getBody().getPublicUrl();

        Thread.sleep(1000);

        defaultHttpClient.get(String.format("%s/status", publicUrl), Object.class);

        Thread.sleep(3000);

        // WHEN
        final Response<CapturedRequests> response1 = defaultHttpClient.get(String.format("%s/api/requests/http", publicUrl), CapturedRequests.class);
        final Response<CapturedRequests> response2 = defaultHttpClient.get(String.format("%s/api/requests/http", publicUrl), List.of(new Parameter("tunnel_name", "my-tunnel")), Collections.emptyMap(), CapturedRequests.class);
        final Response<CapturedRequests> response3 = defaultHttpClient.get(String.format("%s/api/requests/http", publicUrl), List.of(new Parameter("tunnel_name", "my-tunnel (http)")), Collections.emptyMap(), CapturedRequests.class);

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
}
