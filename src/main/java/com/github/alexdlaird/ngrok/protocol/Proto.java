/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

import com.google.gson.annotations.SerializedName;

/**
 * An enum representing <code>ngrok</code>'s valid protos, as defined in <a
 * href="https://ngrok.com/docs/agent/config/v2/#tunnel-configurations"
 * target="_blank"><code>ngrok</code>'s docs</a>.
 */
public enum Proto {

    @SerializedName("http")
    HTTP,
    @SerializedName("tcp")
    TCP,
    @SerializedName("tls")
    TLS;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
