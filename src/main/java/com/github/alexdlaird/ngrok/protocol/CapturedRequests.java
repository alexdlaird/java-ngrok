/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import java.util.List;

/**
 * An object representing CapturedRequests response from <code>ngrok</code>'s API.
 */
public class CapturedRequests {

    private List<CapturedRequest> requests;
    private String uri;

    /**
     * Get the list of {@link CapturedRequest}s.
     */
    public List<CapturedRequest> getRequests() {
        return requests;
    }

    /**
     * Get the URI of the captured requests.
     */
    public String getUri() {
        return uri;
    }
}
