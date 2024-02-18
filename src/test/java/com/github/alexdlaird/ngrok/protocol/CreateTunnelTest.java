/*
 * Copyright (c) 2023 Alex Laird
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

package com.github.alexdlaird.ngrok.protocol;

import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateTunnelTest {

    @Test
    public void testCreateTunnelParams() {
        // WHEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withNgrokVersion(NgrokVersion.V2)
                .withName("name")
                .withProto(Proto.TCP)
                .withAddr(5000)
                .withoutInspect()
                .withAuth("auth-token")
                .withHostHeader("host-header")
                .withBindTls(false)
                .withSubdomain("subdomain")
                .withHostname("hostname")
                .withCrt("crt")
                .withKey("key")
                .withClientCas("clientCas")
                .withRemoteAddr("remoteAddr")
                .withMetadata("metadata")
                .withOAuth(new TunnelOAuth.Builder().withProvider("testcase")
                        .withAllowDomains(Collections.unmodifiableList(
                                Stream.of("one.domain", "two.domain")
                                        .collect(Collectors.toList())))
                        .withAllowEmails(Collections.unmodifiableList(
                                Stream.of("one@email", "two@email")
                                        .collect(Collectors.toList())))
                        .withScopes(Collections.unmodifiableList(
                                Stream.of("ascope", "bscope")
                                        .collect(Collectors.toList())))
                        .build())
                .withCircuitBreaker(0.5f)
                .withCompression(false)
                .withMutualTlsCas("mutualTlsCas")
                .withProxyProto("proxyProto")
                .withWebsocketTcpConverter(false)
                .withTerminateAt("provider")
                .withRequestHeader(new TunnelHeader.Builder().withAdd(Collections.singletonList("req-addition"))
                        .withRemove(Collections.singletonList(("req-subtraction")))
                        .build())
                .withResponseHeader(new TunnelHeader.Builder().withAdd(Collections.singletonList(("res-addition")))
                        .withRemove(Collections.singletonList(("res-subtraction")))
                        .build())
                .withIpRestrictions(new TunnelIPRestrictions.Builder().withAllowCidrs(Collections.singletonList(("allowed")))
                        .withDenyCidrs(Collections.singletonList(("denied")))
                        .build())
                .withVerifyWebhook(new TunnelVerifyWebhook.Builder().withProvider("provider")
                        .withSecret("secret")
                        .build())
                .build();

        // THEN
        assertEquals(NgrokVersion.V2, createTunnel.getNgrokVersion());
        assertEquals("name", createTunnel.getName());
        assertEquals(Proto.TCP, createTunnel.getProto());
        assertEquals("5000", createTunnel.getAddr());
        assertFalse(createTunnel.isInspect());
        assertEquals("auth-token", createTunnel.getAuth());
        assertEquals("host-header", createTunnel.getHostHeader());
        assertEquals(BindTls.FALSE, createTunnel.getBindTls());
        assertEquals("subdomain", createTunnel.getSubdomain());
        assertEquals("hostname", createTunnel.getHostname());
        assertEquals("crt", createTunnel.getCrt());
        assertEquals("key", createTunnel.getKey());
        assertEquals("clientCas", createTunnel.getClientCas());
        assertEquals("remoteAddr", createTunnel.getRemoteAddr());
        assertEquals("metadata", createTunnel.getMetadata());
        assertEquals("testcase", createTunnel.getOauth().getProvider());
        assertTrue(createTunnel.getOauth().getAllowDomains().contains("one.domain"));
        assertTrue(createTunnel.getOauth().getAllowEmails().contains("two@email"));
        assertTrue(createTunnel.getOauth().getScopes().contains("ascope"));
        assertEquals(0.5f, createTunnel.getCircuitBreaker());
        assertFalse(createTunnel.isCompression());
        assertEquals("mutualTlsCas", createTunnel.getMutualTlsCas());
        assertEquals("proxyProto", createTunnel.getProxyProto());
        assertFalse(createTunnel.isWebsocketTcpConverter());
        assertEquals("provider", createTunnel.getTerminateAt());
        assertTrue(createTunnel.getRequestHeader().getAdd().contains("req-addition"));
        assertTrue(createTunnel.getRequestHeader().getRemove().contains("req-subtraction"));
        assertTrue(createTunnel.getResponseHeader().getAdd().contains("res-addition"));
        assertTrue(createTunnel.getResponseHeader().getRemove().contains("res-subtraction"));
        assertTrue(createTunnel.getIpRestrictions().getAllowCidrs().contains("allowed"));
        assertTrue(createTunnel.getIpRestrictions().getDenyCidrs().contains("denied"));
        assertEquals("provider", createTunnel.getVerifyWebhook().getProvider());
        assertEquals("secret", createTunnel.getVerifyWebhook().getSecret());

        assertNull(createTunnel.getSchemes());
    }

    @Test
    public void testCreateTunnelSchemes() {
        // WHEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withSchemes(Collections.unmodifiableList(
                        Stream.of("http", "https")
                                .collect(Collectors.toList())))
                .build();

        // THEN
        assertNull(createTunnel.getBindTls());
        assertEquals(2, createTunnel.getSchemes().size());
        assertEquals("http", createTunnel.getSchemes().get(0));
        assertEquals("https", createTunnel.getSchemes().get(1));
    }

    @Test
    public void testCreateTunnelBindTlsAndSchemesFails() {
        assertThrows(IllegalArgumentException.class, () -> new CreateTunnel.Builder()
                .withBindTls(BindTls.TRUE)
                .withSchemes(Collections.unmodifiableList(
                        Stream.of("http", "https")
                                .collect(Collectors.toList()))));

        assertThrows(IllegalArgumentException.class, () -> new CreateTunnel.Builder()
                .withSchemes(Collections.unmodifiableList(
                        Stream.of("http", "https")
                                .collect(Collectors.toList())))
                .withBindTls(BindTls.TRUE));
    }

    @Test
    public void testCreateTunnelBasicAuth() {
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withBasicAuth(Collections.unmodifiableList(
                        Stream.of("token-1", "token-2")
                                .collect(Collectors.toList())))
                .build();

        assertEquals(2, createTunnel.getBasicAuth().size());
        assertEquals("token-1", createTunnel.getBasicAuth().get(0));
        assertEquals("token-2", createTunnel.getBasicAuth().get(1));
    }

    @Test
    public void testCreateTunnelAuthAndBasicAuthFails() {
        assertThrows(IllegalArgumentException.class, () -> new CreateTunnel.Builder()
                .withAuth("auth-token")
                .withBasicAuth(Collections.singletonList(("auth-token"))));

        assertThrows(IllegalArgumentException.class, () -> new CreateTunnel.Builder()
                .withBasicAuth(Collections.singletonList(("auth-token")))
                .withAuth("auth-token"));
    }

    @Test
    public void testCreateLabelsWithTunnelDefinitions() {
        // WHEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withTunnelDefinition(Collections.singletonMap("labels", Collections.singletonList(("edge=some-edge-id"))))
                .build();

        // THEN
        assertNull(createTunnel.getBindTls());
        assertEquals(1, createTunnel.getLabels().size());
        assertEquals("edge=some-edge-id", createTunnel.getLabels().get(0));
    }

    @Test
    public void testCreateBindTlsLabelsFails() {
        // WHEN
        Map<String, Object> tunnelDefinitions = new HashMap<>();
        tunnelDefinitions.put("bind_tls", true);
        tunnelDefinitions.put("labels", Collections.singletonList(("edge=some-edge-id")));
        assertThrows(IllegalArgumentException.class, () -> new CreateTunnel.Builder()
                .withTunnelDefinition(tunnelDefinitions));
    }

    @Test
    public void testCreateWithTunnelDefinitions() {
        // WHEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withTunnelDefinition(Map.ofEntries(
                        Map.entry("labels", Collections.singletonList("edge=some-edge-id")),
                        Map.entry("auth", "auth-token"),
                        Map.entry("host_header", "host-header"),
                        Map.entry("hostname", "hostname"),
                        Map.entry("crt", "crt"),
                        Map.entry("key", "key"),
                        Map.entry("client_cas", "clientCas"),
                        Map.entry("remote_addr", "remoteAddr"),
                        Map.entry("metadata", "metadata"),
                        Map.entry("compression", "false"),
                        Map.entry("mutual_tls_cas", "mutualTlsCas"),
                        Map.entry("proxy_proto", "proxyProto"),
                        Map.entry("websocket_tcp_converter", "false"),
                        Map.entry("terminate_at", "provider"),
                        Map.entry("request_header", Map.of("add", Collections.singletonList("req-addition"), "remove", Collections.singletonList("req-subtraction"))),
                        Map.entry("response_header", Map.of("add", Collections.singletonList("req-addition"), "remove", Collections.singletonList("req-subtraction"))),
                        Map.entry("ip_restrictions", Map.of("allow_cidrs", Collections.singletonList("allowed"), "deny_cidrs", Collections.singletonList("denied"))),
                        Map.entry("verify_webhook", Map.of("provider", "provider", "secret", "secret"))))
                .build();

        // THEN
        assertEquals(1, createTunnel.getLabels().size());
        assertEquals("edge=some-edge-id", createTunnel.getLabels().get(0));
        assertEquals("auth-token", createTunnel.getAuth());
        assertEquals("host-header", createTunnel.getHostHeader());
        assertEquals("hostname", createTunnel.getHostname());
        assertEquals("crt", createTunnel.getCrt());
        assertEquals("key", createTunnel.getKey());
        assertEquals("clientCas", createTunnel.getClientCas());
        assertEquals("remoteAddr", createTunnel.getRemoteAddr());
        assertEquals("metadata", createTunnel.getMetadata());
        assertFalse(createTunnel.isCompression());
        assertEquals("mutualTlsCas", createTunnel.getMutualTlsCas());
        assertEquals("proxyProto", createTunnel.getProxyProto());
        assertFalse(createTunnel.isWebsocketTcpConverter());
        assertEquals("provider", createTunnel.getTerminateAt());
        assertTrue(createTunnel.getRequestHeader().getAdd().contains("req-addition"));
        assertTrue(createTunnel.getRequestHeader().getRemove().contains("req-subtraction"));
        assertTrue(createTunnel.getResponseHeader().getAdd().contains("req-addition"));
        assertTrue(createTunnel.getResponseHeader().getRemove().contains("req-subtraction"));
        assertTrue(createTunnel.getIpRestrictions().getAllowCidrs().contains("allowed"));
        assertTrue(createTunnel.getIpRestrictions().getDenyCidrs().contains("denied"));
        assertEquals("provider", createTunnel.getVerifyWebhook().getProvider());
        assertEquals("secret", createTunnel.getVerifyWebhook().getSecret());

        assertNull(createTunnel.getBindTls());
    }

    @Test
    public void testCreateWithTunnelDefinitionBasicAuth() {
        // WHEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withTunnelDefinition(Map.of("basic_auth", Collections.unmodifiableList(
                        Stream.of("token-1", "token-2")
                                .collect(Collectors.toList()))))
                .build();

        // THEN
        assertEquals(2, createTunnel.getBasicAuth().size());
        assertEquals("token-1", createTunnel.getBasicAuth().get(0));
        assertEquals("token-2", createTunnel.getBasicAuth().get(1));
    }
}
