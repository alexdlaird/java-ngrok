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
    DARWIN_x86_64_arm("ngrok-v3-stable-darwin-arm64.zip"),
    WINDOWS_i386("ngrok-v3-stable-windows-386.zip"),
    WINDOWS_x86_64("ngrok-v3-stable-windows-amd64.zip"),
    WINDOWS_x86_64_arm("ngrok-v3-stable-windows-arm64.zip"),
    LINUX_i386("ngrok-v3-stable-linux-386.tgz"),
    LINUX_i386_arm("ngrok-v3-stable-linux-arm.tgz"),
    LINUX_x86_64("ngrok-v3-stable-linux-amd64.tgz"),
    LINUX_x86_64_arm("ngrok-v3-stable-linux-arm64.tgz"),
    LINUX_s390x("ngrok-v3-stable-linux-s390x.tgz"),
    LINUX_ppc64("ngrok-v3-stable-linux-ppc64.tgz"),
    LINUX_ppc64le("ngrok-v3-stable-linux-ppc64le.tgz"),
    FREEBSD_i386("ngrok-v3-stable-freebsd-386.tgz"),
    FREEBSD_i386_arm("ngrok-v3-stable-freebsd-arm.tgz"),
    FREEBSD_x86_64("ngrok-v3-stable-freebsd-amd64.tgz");

    private static final String CDN_URL_PREFIX = "https://bin.ngrok.com/c/bNyj1mQVY4c/";

    private final String url;

    NgrokV3CDNUrl(String filename) {
        this.url = CDN_URL_PREFIX + filename;
    }

    @Override
    public String getUrl() {
        return url;
    }
}
