/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.installer;

/**
 * An enum that maps systems and architectures to their corresponding <code>ngrok</code> V3 download URLs.
 */
public enum NgrokV3CDNUrl implements NgrokCDNUrl {

    DARWIN_x86_64("ngrok-v3-stable-darwin-amd64.zip"),
    DARWIN_i386_arm("ngrok-v3-stable-darwin-arm64.zip"),
    WINDOWS_x86_64("ngrok-v3-stable-windows-amd64.zip"),
    WINDOWS_i386("ngrok-v3-stable-windows-386.zip"),
    LINUX_x86_64_arm("ngrok-v3-stable-linux-arm64.zip"),
    LINUX_i386_arm("ngrok-v3-stable-linux-arm.zip"),
    LINUX_i386("ngrok-v3-stable-linux-386.zip"),
    LINUX_x86_64("ngrok-v3-stable-linux-amd64.zip"),
    FREEBSD_x86_64("ngrok-v3-stable-freebsd-amd64.zip"),
    FREEBSD_i386("ngrok-v3-stable-freebsd-386.zip");

    private static final String CDN_URL_PREFIX = "https://bin.equinox.io/c/bNyj1mQVY4c/";

    private final String url;

    NgrokV3CDNUrl(String filename) {
        this.url = CDN_URL_PREFIX + filename;
    }

    @Override
    public String getUrl() {
        return url;
    }
}
