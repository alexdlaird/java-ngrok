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

package com.github.alexdlaird.http;

import com.github.alexdlaird.ngrok.NgrokTestCase;
import com.github.alexdlaird.ngrok.protocol.CapturedRequests;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultHttpClientTest extends NgrokTestCase {

    private DefaultHttpClient defaultHttpClient;

    @BeforeEach
    public void setUp() {
        super.setUp();

        ngrokProcess.start();

        defaultHttpClient = new DefaultHttpClient.Builder(ngrokProcess.getApiUrl()).build();
    }

    @Test
    public void testPost() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .build();

        // WHEN
        final Response<Tunnel> postResponse = defaultHttpClient.post("/api/tunnels", createTunnel, Collections.emptyList(), Collections.emptyMap(), Tunnel.class);

        // THEN
        assertEquals(postResponse.getStatusCode(), 201);
        assertEquals(postResponse.getBody().getName(), "my-tunnel");
    }

    @Test
    public void testGet() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .withBindTls(true)
                .build();
        defaultHttpClient.post("/api/tunnels", createTunnel, Collections.emptyList(), Collections.emptyMap(), Tunnel.class);

        // WHEN
        final Response<Tunnels> getResponse = defaultHttpClient.get("/api/tunnels", Collections.emptyList(), Collections.emptyMap(), Tunnels.class);

        // THEN
        assertEquals(getResponse.getStatusCode(), 200);
        assertEquals(getResponse.getBody().getTunnels().size(), 1);
        assertEquals(getResponse.getBody().getTunnels().get(0).getName(), "my-tunnel");
    }

    @Test
    public void testDelete() {
        // GIVEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .build();
        final Tunnel tunnel = defaultHttpClient.post("/api/tunnels", createTunnel, Collections.emptyList(), Collections.emptyMap(), Tunnel.class).getBody();

        // WHEN
        final Response<?> deleteResponse = defaultHttpClient.delete(tunnel.getUri(), Collections.emptyList(), Collections.emptyMap());

        // THEN
        assertEquals(deleteResponse.getStatusCode(), 204);
        assertNull(deleteResponse.getBody());
    }

    @Test
    public void testGetWithQueryParameters() throws InterruptedException {
        // GIVEN
        final CreateTunnel request = new CreateTunnel.Builder()
                .withName("my-tunnel")
                .withAddr(4040)
                .withBindTls(true)
                .build();
        final Response<Tunnel> createResponse = defaultHttpClient.post("/api/tunnels", request, Collections.emptyList(), Collections.emptyMap(), Tunnel.class);
        final String publicUrl = createResponse.getBody().getPublicUrl();
        final DefaultHttpClient publicHttpClient = new DefaultHttpClient.Builder(publicUrl).build();

        Thread.sleep(1000);

        publicHttpClient.get("/status", Collections.emptyList(), Collections.emptyMap(), Object.class);

        Thread.sleep(3000);

        // WHEN
        final Response<CapturedRequests> response1 = defaultHttpClient.get("/api/requests/http", Collections.emptyList(), Collections.emptyMap(), CapturedRequests.class);
        final Response<CapturedRequests> response2 = defaultHttpClient.get("/api/requests/http", List.of(new Parameter("tunnel_name", "my-tunnel")), Collections.emptyMap(), CapturedRequests.class);
        final Response<CapturedRequests> response3 = defaultHttpClient.get("/api/requests/http", List.of(new Parameter("tunnel_name", "my-tunnel (http)")), Collections.emptyMap(), CapturedRequests.class);

        // THEN
        assertEquals(response1.getStatusCode(), 200);
        assertTrue(response1.getBody().getRequests().size() > 0);
        assertEquals(response2.getStatusCode(), 200);
        assertTrue(response2.getBody().getRequests().size() > 0);
        assertEquals(response3.getStatusCode(), 200);
        assertEquals(response3.getBody().getRequests().size(), 0);
    }
}
