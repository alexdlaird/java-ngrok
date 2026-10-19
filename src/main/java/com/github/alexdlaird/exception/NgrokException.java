/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.exception;

import com.github.alexdlaird.ngrok.process.NgrokLog;
import com.github.alexdlaird.ngrok.process.NgrokProcess;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thrown from {@link NgrokProcess} when an error occurs interacting directly with the <code>ngrok</code> binary.
 */
public class NgrokException extends JavaNgrokException {

    private static final Pattern NGROK_ERROR_CODE_PATTERN = Pattern.compile("ERR_NGROK_\\d+");

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

        this.ngrokLogs = List.of(ngrokLogs.toArray(new NgrokLog[]{}));
        this.ngrokError = null;
    }

    /**
     * An exception with a message, <code>ngrok</code> logs, and the error that caused <code>ngrok</code> to fail.
     *
     * @param message    The message describing the exception.
     * @param ngrokLogs  The <code>ngrok</code> logs.
     * @param ngrokError The error that caused the <code>ngrok</code> process to fail.
     */
    public NgrokException(final String message, final List<NgrokLog> ngrokLogs, final String ngrokError) {
        super(message);

        this.ngrokLogs = List.of(ngrokLogs.toArray(new NgrokLog[]{}));
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

    /**
     * Get the <code>ngrok</code> error code parsed from {@link #getNgrokError()}, if present. See
     * <a href="https://ngrok.com/docs/errors/" target="_blank"><code>ngrok</code>'s error code reference</a> for
     * details on each code.
     */
    public String getNgrokErrorCode() {
        return parseNgrokErrorCode(ngrokError);
    }

    private static String parseNgrokErrorCode(final String ngrokError) {
        if (ngrokError == null) {
            return null;
        }

        final Matcher matcher = NGROK_ERROR_CODE_PATTERN.matcher(ngrokError);
        return matcher.find() ? matcher.group() : null;
    }
}
