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

package com.github.alexdlaird.ngrok;

import com.github.alexdlaird.http.DefaultHttpClient;
import com.github.alexdlaird.http.HttpClient;
import com.github.alexdlaird.http.Response;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnel;
import com.github.alexdlaird.ngrok.protocol.Tunnels;

import java.io.IOException;
import java.util.Collections;

import static java.util.Objects.isNull;

/**
 * A client for interacting with  <a href="https://ngrok.com/docs">ngrok</a>, its binary, and its APIs.
 */
public class NgrokClient {

    private final HttpClient httpClient;
    private final NgrokProcess ngrokProcess;

    // TODO: interactions with NgrokProcess in this class are POC for simple testing while the API is built out, java-ngrok will soon manage its own binary

    private NgrokClient(final Builder builder) {
        this.httpClient = builder.httpClient;
        this.ngrokProcess = builder.ngrokProcess;
    }

    public Tunnel connect(final CreateTunnel createTunnel) throws IOException, InterruptedException {
        ngrokProcess.start();

        final Response<Tunnel> response = httpClient.post("/api/tunnels", createTunnel, Collections.emptyList(), Collections.emptyMap(), Tunnel.class);

        final Tunnel tunnel;
        if (createTunnel.getProto().equals("http") && createTunnel.getBindTls().equals("both")) {
            final Response<Tunnel> getResponse = httpClient.get(response.getBody().getUri() + "%20%28http%29", Collections.emptyList(), Collections.emptyMap(), Tunnel.class);
            tunnel = getResponse.getBody();
        } else {
            tunnel = response.getBody();
        }

        return tunnel;
    }

    public void disconnect(final String publicUrl) throws IOException, InterruptedException {
        ngrokProcess.start();

        final Tunnels tunnels = getTunnels();
        Tunnel tunnel = null;
        // TODO: cache active tunnels so we can first check that before falling back to an API request
        for (Tunnel t : tunnels.getTunnels()) {
            if (t.getPublicUrl().equals(publicUrl)) {
                tunnel = t;
                break;
            }
        }

        if (isNull(tunnel)) {
            return;
        }

        httpClient.delete(tunnel.getUri(), Collections.emptyList(), Collections.emptyMap(), Object.class);
    }

    public Tunnels getTunnels() {
        final Response<Tunnels> response = httpClient.get("/api/tunnels", Collections.emptyList(), Collections.emptyMap(), Tunnels.class);

        return response.getBody();
    }

    public void kill() throws InterruptedException {
        ngrokProcess.stop();
    }

    public NgrokProcess getNgrokProcess() {
        return ngrokProcess;
    }

    public static class Builder {

        private HttpClient httpClient;
        private NgrokProcess ngrokProcess;

        public Builder() {
            // TODO: determine the port dynamically once NgrokProcess is properly implemented
            this.httpClient = new DefaultHttpClient.Builder("http://localhost:4040").build();
            this.ngrokProcess = new NgrokProcess();
        }

        public Builder withHttpClient(final HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder withNgrokProcess(final NgrokProcess ngrokProcess) {
            this.ngrokProcess = ngrokProcess;
            return this;
        }

        public NgrokClient build() {
            return new NgrokClient(this);
        }
    }
}
