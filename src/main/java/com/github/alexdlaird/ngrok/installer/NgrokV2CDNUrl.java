/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.installer;

/**
 * An enum that maps systems and architectures to their corresponding legacy <code>ngrok</code> V2 download URLs.
 */
public enum NgrokV2CDNUrl implements NgrokCDNUrl {

    DARWIN_x86_64("ngrok-stable-darwin-amd64.zip"),
    DARWIN_i386_arm("ngrok-stable-darwin-arm64.zip"),
    WINDOWS_x86_64("ngrok-stable-windows-amd64.zip"),
    WINDOWS_i386("ngrok-stable-windows-386.zip"),
    LINUX_x86_64_arm("ngrok-stable-linux-arm64.zip"),
    LINUX_i386_arm("ngrok-stable-linux-arm.zip"),
    LINUX_i386("ngrok-stable-linux-386.zip"),
    LINUX_x86_64("ngrok-stable-linux-amd64.zip"),
    FREEBSD_x86_64("ngrok-stable-freebsd-amd64.zip"),
    FREEBSD_i386("ngrok-stable-freebsd-386.zip");

    private static final String CDN_URL_PREFIX = "https://bin.equinox.io/c/4VmDzA7iaHb/";

    private final String url;

    NgrokV2CDNUrl(String filename) {
        this.url = CDN_URL_PREFIX + filename;
    }

    @Override
    public String getUrl() {
        return url;
    }
}
