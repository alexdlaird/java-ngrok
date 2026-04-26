/*
 * Copyright (c) 2021-2026 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateEndpointTest {

    @Test
    public void testCreateEndpointParams() {
        // WHEN
        final CreateEndpoint createEndpoint = new CreateEndpoint.Builder()
            .withName("name")
            .withUrl("https://my-endpoint.ngrok.dev")
            .withUpstream(new Upstream.Builder()
                .withUrl("http://localhost:8000")
                .withProtocol("http1")
                .build())
            .withPoolingEnabled(true)
            .withTrafficPolicy(Map.of("on_http_request", List.of()))
            .withMetadata("metadata")
            .withDescription("description")
            .withBindings(List.of("public"))
            .build();

        // THEN
        assertEquals("name", createEndpoint.getName());
        assertEquals("https://my-endpoint.ngrok.dev", createEndpoint.getUrl());
        assertNotNull(createEndpoint.getUpstream());
        assertEquals("http://localhost:8000", createEndpoint.getUpstream().getUrl());
        assertEquals("http1", createEndpoint.getUpstream().getProtocol());
        assertTrue(createEndpoint.isPoolingEnabled());
        assertEquals(Map.of("on_http_request", List.of()), createEndpoint.getTrafficPolicy());
        assertEquals("metadata", createEndpoint.getMetadata());
        assertEquals("description", createEndpoint.getDescription());
        assertEquals(1, createEndpoint.getBindings().size());
        assertEquals("public", createEndpoint.getBindings().get(0));
    }

    @Test
    public void testCreateEndpointUpstreamShortcut() {
        // WHEN
        final CreateEndpoint createEndpoint = new CreateEndpoint.Builder()
            .withUpstream("http://localhost:5000")
            .build();

        // THEN
        assertNotNull(createEndpoint.getUpstream());
        assertEquals("http://localhost:5000", createEndpoint.getUpstream().getUrl());
        assertNull(createEndpoint.getUpstream().getProtocol());
    }

    @Test
    public void testCreateEndpointRequiresUpstream() {
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> new CreateEndpoint.Builder().build());
    }

    @Test
    public void testCreateEndpointWithEndpointDefinition() {
        // WHEN
        final CreateEndpoint createEndpoint = new CreateEndpoint.Builder()
            .withEndpointDefinition(Map.ofEntries(
                Map.entry("url", "https://name.ngrok.dev"),
                Map.entry("upstream", Map.of("url", "http://localhost:9000", "protocol", "http2")),
                Map.entry("pooling_enabled", "true"),
                Map.entry("traffic_policy", Map.of("on_http_request", List.of())),
                Map.entry("metadata", "metadata"),
                Map.entry("description", "description"),
                Map.entry("bindings", List.of("public"))
            ))
            .build();

        // THEN
        assertEquals("https://name.ngrok.dev", createEndpoint.getUrl());
        assertNotNull(createEndpoint.getUpstream());
        assertEquals("http://localhost:9000", createEndpoint.getUpstream().getUrl());
        assertEquals("http2", createEndpoint.getUpstream().getProtocol());
        assertTrue(createEndpoint.isPoolingEnabled());
        assertEquals(Map.of("on_http_request", List.of()), createEndpoint.getTrafficPolicy());
        assertEquals("metadata", createEndpoint.getMetadata());
        assertEquals("description", createEndpoint.getDescription());
        assertEquals(1, createEndpoint.getBindings().size());
    }

    @Test
    public void testCreateEndpointWithEndpointDefinitionUpstreamString() {
        // WHEN
        final CreateEndpoint createEndpoint = new CreateEndpoint.Builder()
            .withEndpointDefinition(Map.of("upstream", "http://localhost:7000"))
            .build();

        // THEN
        assertNotNull(createEndpoint.getUpstream());
        assertEquals("http://localhost:7000", createEndpoint.getUpstream().getUrl());
    }

    @Test
    public void testCreateEndpointBuilderCopy() {
        // GIVEN
        final CreateEndpoint original = new CreateEndpoint.Builder()
            .withName("name")
            .withUpstream("http://localhost:8000")
            .withPoolingEnabled(false)
            .build();

        // WHEN
        final CreateEndpoint copy = new CreateEndpoint.Builder(original).build();

        // THEN
        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getUpstream().getUrl(), copy.getUpstream().getUrl());
        assertFalse(copy.isPoolingEnabled());
    }

    @Test
    public void testUpstreamRequiresUrl() {
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> new Upstream.Builder().build());
    }
}
