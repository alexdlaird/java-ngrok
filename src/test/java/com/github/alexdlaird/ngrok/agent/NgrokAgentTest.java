package com.github.alexdlaird.ngrok.agent;

import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.http.Response;
import com.github.alexdlaird.ngrok.NgrokTestCase;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.protocol.AgentStatus;
import com.github.alexdlaird.ngrok.protocol.CapturedRequest;
import com.github.alexdlaird.ngrok.protocol.CapturedRequests;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NgrokAgentTest extends NgrokTestCase {
    private final HttpClient httpClient = new DefaultHttpClient.Builder()
        .withEncoding("UTF-8")
        .withContentType("application/json")
        .withRetryCount(3)
        .build();

    private NgrokAgent ngrokAgent;

    @BeforeEach
    public void setUp() {
        super.setUp();

        ngrokAgent = new NgrokAgent(ngrokProcessV3, httpClient);
    }

    @Test
    public void testCapturedRequests()
        throws MalformedURLException, InterruptedException {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        final String tunnelName = "my-tunnel";
        ngrokProcessV3.start();
        final CreateTunnel request = new CreateTunnel.Builder(true)
            .withNgrokVersion(NgrokVersion.V3)
            .withName(tunnelName)
            .withAddr(new URL(ngrokProcessV3.getApiUrl()).getPort())
            .build();
        final Response<Tunnel> createResponse = httpClient.post(String.format("%s/api/tunnels",
            ngrokProcessV3.getApiUrl()), request, Tunnel.class);
        final String publicUrl = createResponse.getBody().getPublicUrl();

        Thread.sleep(1000);

        httpClient.get(String.format("%s/api/status", publicUrl), Object.class);

        Thread.sleep(3000);

        // WHEN
        final Response<CapturedRequests> response1 = ngrokAgent.getRequests();
        final Response<CapturedRequests> response2 = ngrokAgent.getRequests("unknown-tunnel");

        // THEN
        final CapturedRequests capturedRequests = response1.getBody();
        assertEquals(1, capturedRequests.getRequests().size());
        assertNotNull(capturedRequests.getUri());
        final CapturedRequest capturedRequest = capturedRequests.getRequests().get(0);
        assertNotNull(capturedRequest.getId());
        assertNotNull(capturedRequest.getUri());
        assertThat(capturedRequest.getDuration(), greaterThan(0));
        assertNotNull(capturedRequest.getStart());
        assertNotNull(capturedRequest.getRemoteAddr());
        assertNotNull(capturedRequest.getRequest().getMethod());
        assertNotNull(capturedRequest.getRequest().getUri());
        assertNotNull(capturedRequest.getRequest().getHeaders());
        assertNotNull(capturedRequest.getRequest().getProto());
        assertNotNull(capturedRequest.getRequest().getRaw());
        assertNotNull(capturedRequest.getResponse());
        assertNotNull(capturedRequest.getResponse().getStatus());
        assertEquals(200, capturedRequest.getResponse().getStatusCode());
        assertNotNull(capturedRequest.getResponse().getHeaders());
        assertNotNull(capturedRequest.getResponse().getProto());
        assertNotNull(capturedRequest.getResponse().getRaw());
        assertEquals(tunnelName, capturedRequest.getTunnelName());
        assertEquals(0, response2.getBody().getRequests().size());

        // WHEN
        final Response<CapturedRequest> response3 = ngrokAgent.getRequest(response1.getBody()
                                                                                   .getRequests().get(0).getId());

        // THEN
        assertEquals(response1.getBody().getRequests().get(0).getId(), response3.getBody().getId());
        assertEquals(tunnelName, response3.getBody().getTunnelName());

        // WHEN
        ngrokAgent.replayRequest(response1.getBody().getRequests().get(0).getId());
        final Response<CapturedRequests> response4 = ngrokAgent.getRequests();
        response4.getBody().getRequests().sort(Comparator.comparing(CapturedRequest::getId));

        // THEN
        assertEquals(2, response4.getBody().getRequests().size());
        assertEquals(response1.getBody().getRequests().get(0).getId(),
            response4.getBody().getRequests().get(0).getId());
        assertEquals(tunnelName, response4.getBody().getRequests().get(0).getTunnelName());
        assertEquals(tunnelName, response4.getBody().getRequests().get(1).getTunnelName());

        // WHEN
        ngrokAgent.deleteRequests();
        final Response<CapturedRequests> response5 = ngrokAgent.getRequests();

        // THEN
        assertEquals(0, response5.getBody().getRequests().size());
    }

    @Test
    public void testGetAgentStatus() {
        testRequiresEnvVar("NGROK_AUTHTOKEN");

        // GIVEN
        ngrokProcessV3.start();

        // WHEN
        final Response<AgentStatus> response = ngrokAgent.getAgentStatus();

        // THEN
        assertEquals("online", response.getBody().getStatus());
        assertNotNull(response.getBody().getAgentVersion());
        assertNotNull(response.getBody().getUri());
    }
}
