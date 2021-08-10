package com.github.alexdlaird.http;

/**
 * Root exception for {@link DefaultHttpClient} interactions.
 */
public class HttpClientException extends RuntimeException {
    /**
     * An exception with a message.
     *
     * @param message The message describing the exception.
     */
    public HttpClientException(final String message) {
        super(message);
    }

    /**
     * An exception with a message and a root cause.
     *
     * @param message The message describing the exception.
     * @param cause   The initial cause of the exception.
     */
    public HttpClientException(final String message, final Exception cause) {
        super(message, cause);
    }
}
