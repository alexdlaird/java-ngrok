/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.protocol;

/**
 * An object representing <code>ngrok</code>'s version and <code>java-ngrok</code>'s version.
 */
public class Version {

    private final String ngrokVersion;
    private final String javaNgrokVersion;

    /**
     * Construct a version.
     *
     * @param ngrokVersion     The <code>ngrok</code> version.
     * @param javaNgrokVersion The <code>java-ngrok</code> version.
     */
    public Version(final String ngrokVersion, final String javaNgrokVersion) {
        this.ngrokVersion = ngrokVersion;
        this.javaNgrokVersion = javaNgrokVersion;
    }

    /**
     * Get the <code>ngrok</code> version.
     */
    public String getNgrokVersion() {
        return ngrokVersion;
    }

    /**
     * Get the <code>java-ngrok</code> version.
     */
    public String getJavaNgrokVersion() {
        return javaNgrokVersion;
    }
}
