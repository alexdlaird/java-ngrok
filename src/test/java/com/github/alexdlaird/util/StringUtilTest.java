/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringUtilTest {
    @Test
    public void testStreamToStringNullInputStream() throws IOException {
        // GIVEN
        final InputStream is = null;

        // WHEN
        final String str = StringUtils.streamToString(is, Charset.forName("UTF-8"));

        // THEN
        assertNull(str);
    }

    @Test
    public void testIsBlankWhitespace() {
        // GIVEN
        final String str = "   ";

        // WHEN
        final boolean isBlank = StringUtils.isBlank(str);

        // THEN
        assertTrue(isBlank);
    }
}
