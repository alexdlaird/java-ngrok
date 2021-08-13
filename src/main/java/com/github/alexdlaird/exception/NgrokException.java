package com.github.alexdlaird.exception;

/**
 * Thrown when an error occurs interacting directly with the `ngrok` binary.
 */
public class NgrokException extends JavaNgrokException {
    /**
     * An exception with a message and a root cause.
     *
     * @param message The message describing the exception.
     * @param cause   The initial cause of the exception.
     */
    public NgrokException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
