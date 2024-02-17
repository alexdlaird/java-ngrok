/*
 * Copyright (c) 2023 Alex Laird
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

package com.github.alexdlaird.ngrok.installer;

/**
 * An enum that maps systems and architectures to their corresponding <code>ngrok</code> download URLs.
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
    FREEBSD_i386("ngrok-v3-stable-freebsd-386.zip"),;

    private static final String CDN_URL_PREFIX = "https://bin.equinox.io/c/bNyj1mQVY4c/";

    private final String url;

    NgrokV3CDNUrl(String filename) {
        this.url = CDN_URL_PREFIX + filename;
    }

    public String getUrl() {
        return url;
    }
}
