package com.github.alexdlaird.ngrok.protocol;

import java.util.List;

public class CapturedRequests {
    private List<CapturedRequest> requests;
    private String uri;

    public List<CapturedRequest> getRequests() {
        return requests;
    }

    public String getUri() {
        return uri;
    }
}
