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

package com.github.alexdlaird.ngrok.process;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NgrokLogTest {
    @Test
    public void testNgrokLogInfo() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("lvl=INFO msg=Test");

        // THEN
        assertEquals("INFO", ngrokLog.getLvl());
        assertEquals("Test", ngrokLog.getMsg());
    }

    @Test
    public void testNgrokLogWarn() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("lvl=WARN msg=Test");

        // THEN
        assertEquals("WARNING", ngrokLog.getLvl());
        assertEquals("Test", ngrokLog.getMsg());
    }

    @Test
    public void testNgrokLogMsgWithSpaces() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("lvl=WARN msg=\"Test=Test with spaces");

        // THEN
        assertEquals("WARNING", ngrokLog.getLvl());
        assertEquals("Test=Test with spaces", ngrokLog.getMsg());
    }

    @Test
    public void testNgrokLogErrNoMsg() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("lvl=ERR no_msg");

        // THEN
        assertEquals("SEVERE", ngrokLog.getLvl());
        assertNull(ngrokLog.getMsg());
    }

    @Test
    public void testNgrokLogCrit() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("lvl=CRIT err=\"Some error\"");

        // THEN
        assertEquals("SEVERE", ngrokLog.getLvl());
        assertEquals("Some error", ngrokLog.getErr());
        assertNull(ngrokLog.getMsg());
    }

    @Test
    public void testNgrokLogLvlNotSet() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("lvl=");

        // THEN
        assertEquals("INFO", ngrokLog.getLvl());
        assertNull(ngrokLog.getMsg());
    }

    @Test
    public void testNgrokLogTimestamp() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("t=123456789");

        // THEN
        assertEquals("123456789", ngrokLog.getT());
    }
}
