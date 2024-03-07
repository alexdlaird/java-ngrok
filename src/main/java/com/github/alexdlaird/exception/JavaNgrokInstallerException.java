/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.exception;

import com.github.alexdlaird.ngrok.installer.NgrokInstaller;

/**
 * Thrown from {@link NgrokInstaller} when an error occurs while downloading and installing the <code>ngrok</code>
 * binary.
 */
public class JavaNgrokInstallerException extends JavaNgrokException {

    /**
     * An exception with a message.
     *
     * @param message The message describing the exception.
     */
    public JavaNgrokInstallerException(final String message) {
        super(message);
    }

    /**
     * An exception with a message and a root cause.
     *
     * @param message The message describing the exception.
     * @param cause   The initial cause of the exception.
     */
    public JavaNgrokInstallerException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
