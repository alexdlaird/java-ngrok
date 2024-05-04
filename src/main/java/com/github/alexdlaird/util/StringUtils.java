/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * Convenience methods for String manipulation.
 */
public class StringUtils {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Convert an {@link InputStream} to a String.
     *
     * @param inputStream The Input Stream to read from.
     * @param charset     The charset of the Input Stream.
     * @return The resulting String.
     * @throws IOException An I/O exception occurred.
     */
    public static String streamToString(final InputStream inputStream, final Charset charset)
        throws IOException {
        if (inputStream == null) {
            return null;
        }

        final char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        final StringBuilder stringBuilder = new StringBuilder();
        final Reader reader = new InputStreamReader(inputStream, charset);

        int size = reader.read(buffer, 0, buffer.length);
        while (size >= 0) {
            stringBuilder.append(buffer, 0, size);

            size = reader.read(buffer, 0, buffer.length);
        }

        return stringBuilder.toString();
    }

    /**
     * Check that a String contains text.
     *
     * @param cs The character sequence to check for text.
     * @return <code>true</code> if not blank.
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * Check that a String does not contain text.
     *
     * @param cs The character sequence to check for text.
     * @return <code>true</code> if blank.
     */
    public static boolean isBlank(final CharSequence cs) {
        int strLen;

        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }
}
