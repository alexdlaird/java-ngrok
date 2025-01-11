/*
 * Copyright (c) 2021-2025 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.installer;

/**
 * An enum for the list of supported <code>ngrok</code> config versions.
 */
public enum ConfigVersion {

    V2("2"),
    V3("3");

    private final String version;

    ConfigVersion(final String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
