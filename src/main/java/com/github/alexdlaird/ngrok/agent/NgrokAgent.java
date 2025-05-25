/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.agent;

import com.github.alexdlaird.exception.JavaNgrokException;
import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.http.Parameter;
import com.github.alexdlaird.http.Response;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import com.github.alexdlaird.ngrok.protocol.AgentStatus;
import com.github.alexdlaird.ngrok.protocol.CapturedRequest;
import com.github.alexdlaird.ngrok.protocol.CapturedRequests;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.nonNull;

/**
 * A client for interaction with the <code>ngrok</code> agent.
 *
 * <p>For usage examples, see
 * <a href="https://alexdlaird.github.io/java-ngrok/" target="_blank"><code>java-ngrok</code>'s documentation</a>.
 */
public class NgrokAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(NgrokAgent.class);

    private final NgrokProcess ngrokProcess;

    private final HttpClient httpClient;

    /**
     * See {@link NgrokAgent#NgrokAgent(NgrokProcess, HttpClient)}.
     *
     * @param ngrokProcess The <code>ngrok</code> process.
     */
    public NgrokAgent(final NgrokProcess ngrokProcess) {
        this(ngrokProcess, new DefaultHttpClient.Builder().build());
    }

    /**
     * Construct to interact with the <code>ngrok</code> agent.
     *
     * @param ngrokProcess The <code>ngrok</code> process.
     * @param httpClient   The HTTP client.
     */
    public NgrokAgent(final NgrokProcess ngrokProcess,
                      final HttpClient httpClient) {
        this.ngrokProcess = Objects.requireNonNull(ngrokProcess);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    /**
     * Get the <code>ngrok</code> agent status.
     *
     * @return The agent status.
     */
    public Response<AgentStatus> getAgentStatus() {
        if (!ngrokProcess.isRunning()) {
            throw new JavaNgrokException("ngrok is not running.");
        }

        final String apiUrl = ngrokProcess.getApiUrl();

        LOGGER.info("Getting agent status from {}", apiUrl);

        return httpClient.get(String.format("%s/api/status", apiUrl), AgentStatus.class);
    }

    /**
     * See {@link NgrokAgent#getRequests(String)}.
     */
    public Response<CapturedRequests> getRequests() {
        return getRequests(null);
    }

    /**
     * Get the list of requests made to either all tunnels, or the given tunnel name.
     *
     * @param tunnelName The optional tunnel name to filter by.
     */
    public Response<CapturedRequests> getRequests(final String tunnelName) {
        if (!ngrokProcess.isRunning()) {
            throw new JavaNgrokException("ngrok is not running.");
        }
        List<Parameter> params = nonNull(tunnelName) ? List.of(new Parameter("tunnel_name", tunnelName)) : List.of();

        final String apiUrl = ngrokProcess.getApiUrl();

        LOGGER.info("Listing captured requests from {} with {}", apiUrl, params);

        return httpClient.get(String.format("%s/api/requests/http", apiUrl),
            params,
            Map.of(),
            CapturedRequests.class);
    }

    /**
     * Get the given request.
     *
     * @param requestId The ID of the request to fetch.
     */
    public Response<CapturedRequest> getRequest(final String requestId) {
        if (!ngrokProcess.isRunning()) {
            throw new JavaNgrokException("ngrok is not running.");
        }

        final String apiUrl = ngrokProcess.getApiUrl();

        LOGGER.info("Getting captured request {} from {}", requestId, apiUrl);

        return httpClient.get(String.format("%s/api/requests/http/%s", apiUrl, requestId), CapturedRequest.class);
    }

    /**
     * See {@link NgrokAgent#replayRequest(String, String)}.
     */
    public void replayRequest(final String requestId) {
        replayRequest(requestId, null);
    }

    /**
     * Replay a given request through its original tunnel, or through a different given tunnel.
     *
     * @param requestId  The request ID.
     * @param tunnelName The optional name of tunnel to replay the request through.
     */
    public void replayRequest(final String requestId,
                              final String tunnelName) {
        if (!ngrokProcess.isRunning()) {
            throw new JavaNgrokException("ngrok is not running.");
        }

        final String apiUrl = ngrokProcess.getApiUrl();

        LOGGER.info("Replaying captured request {} from {}", requestId, apiUrl);

        httpClient.post(String.format("%s/api/requests/http", apiUrl),
            new ReplayRequest(requestId, tunnelName),
            Object.class);
    }

    /**
     * Delete request history.
     */
    public void deleteRequests() {
        if (!ngrokProcess.isRunning()) {
            throw new JavaNgrokException("ngrok is not running.");
        }

        final String apiUrl = ngrokProcess.getApiUrl();

        LOGGER.info("Deleting captured requests from {}", apiUrl);

        httpClient.delete(String.format("%s/api/requests/http", apiUrl));
    }

    private static class ReplayRequest {
        private final String id;
        private final String tunnelName;

        private ReplayRequest(final String id,
                              final String tunnelName) {
            this.id = id;
            this.tunnelName = tunnelName;
        }

        private String getId() {
            return id;
        }

        private String getTunnelName() {
            return tunnelName;
        }
    }
}
