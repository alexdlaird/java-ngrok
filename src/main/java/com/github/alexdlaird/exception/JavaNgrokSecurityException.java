/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.exception;

import com.github.alexdlaird.ngrok.process.NgrokProcess;

/**
 * Thrown from {@link NgrokProcess} when a security error occurs.
 */
public class JavaNgrokSecurityException extends JavaNgrokException {
    /**
     * An exception with a message.
     *
     * @param message The message describing the exception.
     */
    public JavaNgrokSecurityException(final String message) {
        super(message);
    }
}
