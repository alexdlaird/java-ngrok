/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

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
            .withDomain("java-ngrok.com")
            .withAddr(5000)
            .withoutInspect()
            .withAuth("auth-token")
            .withHostHeader("host-header")
            .withBindTls(false)
            .withSubdomain("subdomain")
            .withCrt("crt")
            .withKey("key")
            .withRemoteAddr("remoteAddr")
            .withMetadata("metadata")
            .withOAuth(new TunnelOAuth.Builder()
                .withProvider("testcase")
                .withAllowDomains(List.of("one.domain", "two.domain"))
                .withAllowEmails(List.of("one@email", "two@email"))
                .withScopes(List.of("ascope", "bscope"))
                .build())
            .withCircuitBreaker(0.5f)
            .withCompression(false)
            .withMutualTlsCas("mutualTlsCas")
            .withProxyProto("proxyProto")
            .withWebsocketTcpConverter(false)
            .withTerminateAt("provider")
            .withRequestHeader(new TunnelHeader.Builder()
                .withAdd(List.of("req-addition"))
                .withRemove(List.of("req-subtraction"))
                .build())
            .withResponseHeader(new TunnelHeader.Builder()
                .withAdd(List.of("res-addition"))
                .withRemove(List.of("res-subtraction"))
                .build())
            .withIpRestriction(new TunnelIPRestriction.Builder()
                .withAllowCidrs(List.of("allowed"))
                .withDenyCidrs(List.of("denied"))
                .build())
            .withVerifyWebhook(new TunnelVerifyWebhook.Builder()
                .withProvider("provider")
                .withSecret("secret")
                .build())
            .withUserAgentFilter(new TunnelUserAgentFilter.Builder()
                .withAllow(List.of("allow-user-agent"))
                .withDeny(List.of("deny-user-agent"))
                .build())
            .withPolicyInbound(new TunnelPolicy.Builder()
                .withName("inbound-policy")
                .withExpressions(List.of("inbound-policy-expression"))
                .withActions(
                    new TunnelPolicyActions.Builder()
                        .withType("inbound-policy-actions-type")
                        .withConfig("inbound-policy-actions-config")
                        .build())
                .build())
            .withPolicyOutbound(new TunnelPolicy.Builder()
                .withName("outbound-policy")
                .withExpressions(List.of("outbound-policy-expression"))
                .withActions(new TunnelPolicyActions.Builder()
                    .withType("outbound-policy-actions-type")
                    .withConfig("outbound-policy-actions-config")
                    .build())
                .build())
            .withPoolingEnabled(false)
            .build();

        // THEN
        assertEquals(NgrokVersion.V2, createTunnel.getNgrokVersion());
        assertEquals("name", createTunnel.getName());
        assertEquals(Proto.TCP, createTunnel.getProto());
        assertEquals("java-ngrok.com", createTunnel.getDomain());
        assertEquals("5000", createTunnel.getAddr());
        assertFalse(createTunnel.isInspect());
        assertEquals("auth-token", createTunnel.getAuth());
        assertEquals("host-header", createTunnel.getHostHeader());
        assertEquals(BindTls.FALSE, createTunnel.getBindTls());
        assertEquals("subdomain", createTunnel.getSubdomain());
        assertEquals("crt", createTunnel.getCrt());
        assertEquals("key", createTunnel.getKey());
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
        assertTrue(createTunnel.getIpRestriction().getAllowCidrs().contains("allowed"));
        assertTrue(createTunnel.getIpRestriction().getDenyCidrs().contains("denied"));
        assertEquals("provider", createTunnel.getVerifyWebhook().getProvider());
        assertEquals("secret", createTunnel.getVerifyWebhook().getSecret());
        assertTrue(createTunnel.getUserAgentFilter().getAllow().contains("allow-user-agent"));
        assertTrue(createTunnel.getUserAgentFilter().getDeny().contains("deny-user-agent"));
        assertEquals("inbound-policy", createTunnel.getPolicyInbound().getName());
        assertTrue(createTunnel.getPolicyInbound().getExpressions().contains("inbound-policy-expression"));
        assertEquals("inbound-policy-actions-type", createTunnel.getPolicyInbound().getActions().getType());
        assertEquals("inbound-policy-actions-config", createTunnel.getPolicyInbound().getActions().getConfig());
        assertEquals("outbound-policy", createTunnel.getPolicyOutbound().getName());
        assertTrue(createTunnel.getPolicyOutbound().getExpressions().contains("outbound-policy-expression"));
        assertEquals("outbound-policy-actions-type", createTunnel.getPolicyOutbound().getActions().getType());
        assertEquals("outbound-policy-actions-config", createTunnel.getPolicyOutbound().getActions().getConfig());
        assertFalse(createTunnel.isPoolingEnabled());

        assertNull(createTunnel.getSchemes());
    }

    @Test
    public void testCreateTunnelWithDefaults() {
        // WHEN
        final CreateTunnel createTunnel1 = new CreateTunnel.Builder(true)
            .withNgrokVersion(NgrokVersion.V3)
            .withBindTls(true)
            .build();
        final CreateTunnel createTunnel2 = new CreateTunnel.Builder(true)
            .withNgrokVersion(NgrokVersion.V3)
            .withBindTls(false)
            .build();

        // THEN
        assertEquals(NgrokVersion.V3, createTunnel1.getNgrokVersion());
        assertEquals(1, createTunnel1.getSchemes().size());
        assertEquals("https", createTunnel1.getSchemes().get(0));

        assertEquals(NgrokVersion.V3, createTunnel2.getNgrokVersion());
        assertEquals(1, createTunnel2.getSchemes().size());
        assertEquals("http", createTunnel2.getSchemes().get(0));
    }

    @Test
    public void testCreateTunnelSchemes() {
        // WHEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withSchemes(List.of("http", "https"))
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
            .withSchemes(List.of("http", "https")));

        assertThrows(IllegalArgumentException.class, () -> new CreateTunnel.Builder()
            .withSchemes(List.of("http", "https"))
            .withBindTls(BindTls.TRUE));
    }

    @Test
    public void testCreateTunnelBasicAuth() {
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withBasicAuth(List.of("token-1", "token-2"))
            .build();

        assertEquals(2, createTunnel.getBasicAuth().size());
        assertEquals("token-1", createTunnel.getBasicAuth().get(0));
        assertEquals("token-2", createTunnel.getBasicAuth().get(1));
    }

    @Test
    public void testCreateTunnelAuthAndBasicAuthFails() {
        assertThrows(IllegalArgumentException.class, () -> new CreateTunnel.Builder()
            .withAuth("auth-token")
            .withBasicAuth(List.of("auth-token")));

        assertThrows(IllegalArgumentException.class, () -> new CreateTunnel.Builder()
            .withBasicAuth(List.of("auth-token"))
            .withAuth("auth-token"));
    }

    @Test
    public void testCreateBindTlsLabelsFails() {
        // WHEN
        assertThrows(IllegalArgumentException.class, () -> new CreateTunnel.Builder()
            .withTunnelDefinition(Map.of("bind_tls", true, "labels", List.of("edge=some-edge-id"))));
    }

    @Test
    public void testCreateWithTunnelDefinitions() {
        // WHEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withTunnelDefinition(Map.ofEntries(
                    Map.entry("labels", List.of("edge=some-edge-id")),
                    Map.entry("proto", "tcp"),
                    Map.entry("domain", "java-ngrok.com"),
                    Map.entry("addr", "5000"),
                    Map.entry("inspect", "false"),
                    Map.entry("basic_auth", List.of("auth-token")),
                    Map.entry("host_header", "host-header"),
                    Map.entry("crt", "crt"),
                    Map.entry("key", "key"),
                    Map.entry("remote_addr", "remoteAddr"),
                    Map.entry("metadata", "metadata"),
                    Map.entry("compression", "false"),
                    Map.entry("mutual_tls_cas", "mutualTlsCas"),
                    Map.entry("proxy_proto", "proxyProto"),
                    Map.entry("websocket_tcp_converter", "false"),
                    Map.entry("terminate_at", "provider"),
                    Map.entry("request_header",
                        Map.of("add", List.of("req-addition"), "remove", List.of("req-subtraction"))),
                    Map.entry("response_header",
                        Map.of("add", List.of("res-addition"), "remove", List.of("res-subtraction"))),
                    Map.entry("ip_restriction",
                        Map.of("allow_cidrs", List.of("allowed"), "deny_cidrs", List.of("denied"))),
                    Map.entry("verify_webhook",
                        Map.of("provider", "provider", "secret", "secret")),
                    Map.entry("user_agent_filter",
                        Map.of("allow", List.of("allow-user-agent"), "deny", List.of("deny-user-agent"))),
                    Map.entry("policy",
                        Map.of("inbound",
                            Map.of("name", "inbound-policy",
                                "expressions", List.of("inbound-policy-expression"),
                                "actions",
                                Map.of("type", "inbound-policy-actions-type",
                                    "config", "inbound-policy-actions-config")),
                            "outbound",
                            Map.of("name", "outbound-policy",
                                "expressions", List.of("outbound-policy-expression"),
                                "actions",
                                Map.of("type", "outbound-policy-actions-type",
                                    "config", "outbound-policy-actions-config")))),
                    Map.entry("pooling_enabled", "false")
                )
            ).build();

        // THEN
        assertEquals(1, createTunnel.getLabels().size());
        assertEquals(Proto.TCP, createTunnel.getProto());
        assertEquals("java-ngrok.com", createTunnel.getDomain());
        assertEquals("5000", createTunnel.getAddr());
        assertFalse(createTunnel.isInspect());
        assertEquals("edge=some-edge-id", createTunnel.getLabels().get(0));
        assertTrue(createTunnel.getBasicAuth().contains("auth-token"));
        assertEquals("host-header", createTunnel.getHostHeader());
        assertEquals("crt", createTunnel.getCrt());
        assertEquals("key", createTunnel.getKey());
        assertEquals("remoteAddr", createTunnel.getRemoteAddr());
        assertEquals("metadata", createTunnel.getMetadata());
        assertFalse(createTunnel.isCompression());
        assertEquals("mutualTlsCas", createTunnel.getMutualTlsCas());
        assertEquals("proxyProto", createTunnel.getProxyProto());
        assertFalse(createTunnel.isWebsocketTcpConverter());
        assertEquals("provider", createTunnel.getTerminateAt());
        assertTrue(createTunnel.getRequestHeader().getAdd().contains("req-addition"));
        assertTrue(createTunnel.getRequestHeader().getRemove().contains("req-subtraction"));
        assertTrue(createTunnel.getResponseHeader().getAdd().contains("res-addition"));
        assertTrue(createTunnel.getResponseHeader().getRemove().contains("res-subtraction"));
        assertTrue(createTunnel.getIpRestriction().getAllowCidrs().contains("allowed"));
        assertTrue(createTunnel.getIpRestriction().getDenyCidrs().contains("denied"));
        assertEquals("provider", createTunnel.getVerifyWebhook().getProvider());
        assertEquals("secret", createTunnel.getVerifyWebhook().getSecret());
        assertTrue(createTunnel.getUserAgentFilter().getAllow().contains("allow-user-agent"));
        assertTrue(createTunnel.getUserAgentFilter().getDeny().contains("deny-user-agent"));
        assertEquals("inbound-policy", createTunnel.getPolicyInbound().getName());
        assertTrue(createTunnel.getPolicyInbound().getExpressions().contains("inbound-policy-expression"));
        assertEquals("inbound-policy-actions-type", createTunnel.getPolicyInbound().getActions().getType());
        assertEquals("inbound-policy-actions-config", createTunnel.getPolicyInbound().getActions().getConfig());
        assertEquals("outbound-policy", createTunnel.getPolicyOutbound().getName());
        assertTrue(createTunnel.getPolicyOutbound().getExpressions().contains("outbound-policy-expression"));
        assertEquals("outbound-policy-actions-type", createTunnel.getPolicyOutbound().getActions().getType());
        assertEquals("outbound-policy-actions-config", createTunnel.getPolicyOutbound().getActions().getConfig());
        assertFalse(createTunnel.isPoolingEnabled());

        assertNull(createTunnel.getBindTls());
    }

    @Test
    public void testCreateWithTunnelDefinitionLegacyAuth() {
        // WHEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withTunnelDefinition(Map.of("auth", "some-token"))
            .build();

        // THEN
        assertEquals("some-token", createTunnel.getAuth());
    }

    @Test
    public void testCreateWithTunnelDefinitionBasicAuth() {
        // WHEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withTunnelDefinition(Map.of("basic_auth", List.of("token-1", "token-2")))
            .build();

        // THEN
        assertEquals(2, createTunnel.getBasicAuth().size());
        assertEquals("token-1", createTunnel.getBasicAuth().get(0));
        assertEquals("token-2", createTunnel.getBasicAuth().get(1));
    }

    @Test
    public void testCreateWithTunnelDefinitionOnHttpRequestResponse() {
        // WHEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withTunnelDefinition(Map.ofEntries(
                    Map.entry("policy",
                        Map.of("on_http_request",
                            Map.of("name", "inbound-policy",
                                "expressions", List.of("inbound-policy-expression"),
                                "actions",
                                Map.of("type", "inbound-policy-actions-type",
                                    "config", "inbound-policy-actions-config")),
                            "on_http_response",
                            Map.of("name", "outbound-policy",
                                "expressions", List.of("outbound-policy-expression"),
                                "actions",
                                Map.of("type", "outbound-policy-actions-type",
                                    "config", "outbound-policy-actions-config"))))
                )
            ).build();

        // THEN
        assertEquals("inbound-policy", createTunnel.getPolicyInbound().getName());
        assertTrue(createTunnel.getPolicyInbound().getExpressions().contains("inbound-policy-expression"));
        assertEquals("inbound-policy-actions-type", createTunnel.getPolicyInbound().getActions().getType());
        assertEquals("inbound-policy-actions-config", createTunnel.getPolicyInbound().getActions().getConfig());
        assertEquals("outbound-policy", createTunnel.getPolicyOutbound().getName());
        assertTrue(createTunnel.getPolicyOutbound().getExpressions().contains("outbound-policy-expression"));
        assertEquals("outbound-policy-actions-type", createTunnel.getPolicyOutbound().getActions().getType());
        assertEquals("outbound-policy-actions-config", createTunnel.getPolicyOutbound().getActions().getConfig());

        assertNull(createTunnel.getBindTls());
    }
}
