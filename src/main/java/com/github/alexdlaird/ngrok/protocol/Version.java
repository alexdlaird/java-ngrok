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

package com.github.alexdlaird.ngrok.protocol;

/**
 * An object representing <code>ngrok</code>'s version and <code>java-ngrok</code>'s version.
 */
public class Version {

    private final String ngrokVersion;
    private final String javaNgrokVersion;

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
