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
     * @throws IOException An I/O error has occurred.
     */
    public static String streamToString(final InputStream inputStream, final Charset charset) throws IOException {
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
