/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.exception;

import com.github.alexdlaird.ngrok.NgrokClient;

/**
 * Root exception for the {@link NgrokClient} and the <code>java-ngrok</code> library.
 */
public class JavaNgrokException extends RuntimeException {

    /**
     * An exception with a message.
     *
     * @param message The message describing the exception.
     */
    public JavaNgrokException(final String message) {
        super(message);
    }

    /**
     * An exception with a message and a root cause.
     *
     * @param message The message describing the exception.
     * @param cause   The initial cause of the exception.
     */
    public JavaNgrokException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
