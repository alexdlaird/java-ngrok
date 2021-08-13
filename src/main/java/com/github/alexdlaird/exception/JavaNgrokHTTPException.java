package com.github.alexdlaird.exception;

/**
 * Thrown when an error occurs making a request to the `ngrok` web interface.
 */
public class JavaNgrokHTTPException extends JavaNgrokException {
    /**
     * An exception with a message.
     *
     * @param message The message describing the exception.
     */
    public JavaNgrokHTTPException(final String message) {
        super(message);
    }

    /**
     * An exception with a message and a root cause.
     *
     * @param message The message describing the exception.
     * @param cause   The initial cause of the exception.
     */
    public JavaNgrokHTTPException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
