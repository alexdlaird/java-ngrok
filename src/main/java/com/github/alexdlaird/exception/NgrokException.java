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

package com.github.alexdlaird.exception;

import com.github.alexdlaird.ngrok.process.NgrokLog;
import com.github.alexdlaird.ngrok.process.NgrokProcess;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Thrown from {@link NgrokProcess} when an error occurs interacting directly with the <code>ngrok</code> binary.
 */
public class NgrokException extends JavaNgrokException {

    private final List<NgrokLog> ngrokLogs;
    private final String ngrokError;

    /**
     * An exception with a message.
     *
     * @param message The message describing the exception.
     */
    public NgrokException(String message) {
        super(message);

        this.ngrokLogs = null;
        this.ngrokError = null;
    }

    /**
     * An exception with a message and a root cause.
     *
     * @param message The message describing the exception.
     * @param cause   The initial cause of the exception.
     */
    public NgrokException(final String message, final Throwable cause) {
        super(message, cause);

        this.ngrokLogs = null;
        this.ngrokError = null;
    }

    /**
     * An exception with a message and <code>ngrok</code> logs.
     *
     * @param message   The message describing the exception.
     * @param ngrokLogs The <code>ngrok</code> logs.
     */
    public NgrokException(final String message, final List<NgrokLog> ngrokLogs) {
        super(message);

        this.ngrokLogs = Collections.unmodifiableList(
                Stream.of(ngrokLogs.toArray(new NgrokLog[]{}))
                        .collect(Collectors.toList()));
        this.ngrokError = null;
    }

    /**
     * An exception with a message, <code>ngrok</code> logs, and the error that caused <code>ngrok</code> to
     * fail.
     *
     * @param message    The message describing the exception.
     * @param ngrokLogs  The <code>ngrok</code> logs.
     * @param ngrokError The error that caused the <code>ngrok</code> process to fail.
     */
    public NgrokException(final String message, final List<NgrokLog> ngrokLogs, final String ngrokError) {
        super(message);

        this.ngrokLogs = Collections.unmodifiableList(
                Stream.of(ngrokLogs.toArray(new NgrokLog[]{}))
                        .collect(Collectors.toList()));
        this.ngrokError = ngrokError;
    }

    /**
     * Get the <code>ngrok</code> logs, which may be useful for debugging.
     */
    public List<NgrokLog> getNgrokLogs() {
        return ngrokLogs;
    }

    /**
     * Get The error that caused the <code>ngrok</code> process to fail.
     */
    public String getNgrokError() {
        return ngrokError;
    }
}
