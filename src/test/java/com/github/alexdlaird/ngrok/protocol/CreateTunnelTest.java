/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
            .withIpRestriction(new TunnelIPRestriction
                .Builder().withAllowCidrs(Collections.singletonList(("allowed")))
                          .withDenyCidrs(Collections.singletonList(("denied")))
                          .build())
            .withVerifyWebhook(new TunnelVerifyWebhook.Builder().withProvider("provider")
                                                                .withSecret("secret")
                                                                .build())
            .withUserAgentFilter(new TunnelUserAgentFilter.Builder()
                .withAllow(Collections.singletonList("allow-user-agent"))
                .withDeny(Collections.singletonList("deny-user-agent"))
                .build())
            .withPolicyInbound(new TunnelPolicy.Builder()
                .withName("inbound-policy")
                .withActions(new TunnelPolicyActions.Builder()
                    .withType("inbound-policy-actions-type")
                    .withConfig("inbound-policy-actions-config")
                    .build())
                .withExpressions(Collections.singletonList("inbound-policy-expression"))
                .build())
            .withPolicyOutbound(new TunnelPolicy.Builder()
                .withName("outbound-policy")
                .withActions(new TunnelPolicyActions.Builder()
                    .withType("outbound-policy-actions-type")
                    .withConfig("outbound-policy-actions-config")
                    .build())
                .withExpressions(Collections.singletonList("outbound-policy-expression"))
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
            .withTunnelDefinition(Collections.singletonMap("labels",
                Collections.singletonList(("edge=some-edge-id"))))
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
            .withTunnelDefinition(Collections.unmodifiableMap(tunnelDefinitions)));
    }

    @Test
    public void testCreateWithTunnelDefinitions() {
        // WHEN
        final Map<String, Object> tunnelDefinition = new HashMap<>();
        tunnelDefinition.put("labels", Collections.singletonList("edge=some-edge-id"));
        tunnelDefinition.put("proto", "tcp");
        tunnelDefinition.put("domain", "java-ngrok.com");
        tunnelDefinition.put("addr", "5000");
        tunnelDefinition.put("inspect", "false");
        tunnelDefinition.put("basic_auth", Collections.singletonList("auth-token"));
        tunnelDefinition.put("host_header", "host-header");
        tunnelDefinition.put("hostname", "hostname");
        tunnelDefinition.put("crt", "crt");
        tunnelDefinition.put("key", "key");
        tunnelDefinition.put("client_cas", "clientCas");
        tunnelDefinition.put("remote_addr", "remoteAddr");
        tunnelDefinition.put("metadata", "metadata");
        tunnelDefinition.put("compression", "false");
        tunnelDefinition.put("mutual_tls_cas", "mutualTlsCas");
        tunnelDefinition.put("proxy_proto", "proxyProto");
        tunnelDefinition.put("websocket_tcp_converter", "false");
        tunnelDefinition.put("terminate_at", "provider");
        final Map<String, Object> requestHeaderDefinition = new HashMap<>();
        requestHeaderDefinition.put("add", Collections.singletonList("req-addition"));
        requestHeaderDefinition.put("remove", Collections.singletonList("req-subtraction"));
        tunnelDefinition.put("request_header", Collections.unmodifiableMap(requestHeaderDefinition));
        final Map<String, Object> responseHeaderDefinition = new HashMap<>();
        responseHeaderDefinition.put("add", Collections.singletonList("res-addition"));
        responseHeaderDefinition.put("remove", Collections.singletonList("res-subtraction"));
        tunnelDefinition.put("response_header", Collections.unmodifiableMap(responseHeaderDefinition));
        final Map<String, Object> ipRestrictionsDefinition = new HashMap<>();
        ipRestrictionsDefinition.put("allow_cidrs", Collections.singletonList("allowed"));
        ipRestrictionsDefinition.put("deny_cidrs", Collections.singletonList("denied"));
        tunnelDefinition.put("ip_restriction", Collections.unmodifiableMap(ipRestrictionsDefinition));
        final Map<String, Object> verifyWebhookDefinition = new HashMap<>();
        verifyWebhookDefinition.put("provider", "provider");
        verifyWebhookDefinition.put("secret", "secret");
        tunnelDefinition.put("verify_webhook", Collections.unmodifiableMap(verifyWebhookDefinition));
        final Map<String, Object> userAgentFilter = new HashMap<>();
        userAgentFilter.put("allow", Collections.singletonList("allow-user-agent"));
        userAgentFilter.put("deny", Collections.singletonList("deny-user-agent"));
        tunnelDefinition.put("user_agent_filter", Collections.unmodifiableMap(userAgentFilter));
        final Map<String, Object> inboundPolicy = new HashMap<>();
        inboundPolicy.put("name", "inbound-policy");
        inboundPolicy.put("expressions", Collections.singletonList("inbound-policy-expression"));
        final Map<String, Object> inboundPolicyActions = new HashMap<>();
        inboundPolicyActions.put("type", "inbound-policy-actions-type");
        inboundPolicyActions.put("config", "inbound-policy-actions-config");
        inboundPolicy.put("actions", Collections.unmodifiableMap(inboundPolicyActions));
        final Map<String, Object> outboundPolicy = new HashMap<>();
        outboundPolicy.put("name", "outbound-policy");
        outboundPolicy.put("expressions", Collections.singletonList("outbound-policy-expression"));
        final Map<String, Object> outboundPolicyActions = new HashMap<>();
        outboundPolicyActions.put("type", "outbound-policy-actions-type");
        outboundPolicyActions.put("config", "outbound-policy-actions-config");
        outboundPolicy.put("actions", Collections.unmodifiableMap(outboundPolicyActions));
        final Map<String, Object> policies = new HashMap<>();
        policies.put("inbound", Collections.unmodifiableMap(inboundPolicy));
        policies.put("outbound", Collections.unmodifiableMap(outboundPolicy));
        tunnelDefinition.put("policy", Collections.unmodifiableMap(policies));
        tunnelDefinition.put("pooling_enabled", "false");
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withTunnelDefinition(Collections.unmodifiableMap(tunnelDefinition))
            .build();

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
    public void testCreateWithTunnelDefinitionBasicAuth() {
        // WHEN
        final CreateTunnel createTunnel = new CreateTunnel.Builder()
            .withTunnelDefinition(Collections.singletonMap("basic_auth", Collections.unmodifiableList(
                Stream.of("token-1", "token-2")
                      .collect(Collectors.toList()))))
            .build();

        // THEN
        assertEquals(2, createTunnel.getBasicAuth().size());
        assertEquals("token-1", createTunnel.getBasicAuth().get(0));
        assertEquals("token-2", createTunnel.getBasicAuth().get(1));
    }
}
