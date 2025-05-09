/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.process;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NgrokLogTest {
    @Test
    public void testNgrokLog() {
        // GIVEN
        final String logLine = "t=2024-03-08T08:45:07-0600 lvl=info msg=\"starting web service\" "
                               + "obj=web addr=127.0.0.1:4040 allow_hosts=[]";

        // WHEN
        final NgrokLog ngrokLog = new NgrokLog(logLine);

        // THEN
        assertEquals(ngrokLog.getLine(), logLine);
        assertEquals(ngrokLog.getT(), "2024-03-08T08:45:07-0600");
        assertEquals(ngrokLog.getLvl(), "INFO");
        assertEquals(ngrokLog.getMsg(), "starting web service");
        assertEquals(ngrokLog.getObj(), "web");
        assertEquals(ngrokLog.getAddr(), "127.0.0.1:4040");
    }

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
    public void testNgrokLogMsgWithPossessiveQuote() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("lvl=WARN msg=\"Test=This is Tom's test\"");

        // THEN
        assertEquals("WARNING", ngrokLog.getLvl());
        assertEquals("Test=This is Tom's test", ngrokLog.getMsg());
    }

    @Test
    public void testNgrokLogMsgWithSpaces() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("lvl=WARN msg=\"Test=Test with spaces\"");

        // THEN
        assertEquals("WARNING", ngrokLog.getLvl());
        assertEquals("Test=Test with spaces", ngrokLog.getMsg());
    }

    @Test
    public void testNgrokLogErrNoMsg() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("lvl=ERR no_msg");

        // THEN
        assertEquals("ERROR", ngrokLog.getLvl());
        assertNull(ngrokLog.getMsg());
    }

    @Test
    public void testNgrokLogCrit() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("lvl=CRIT err=\"Some error\"");

        // THEN
        assertEquals("CRITICAL", ngrokLog.getLvl());
        assertEquals("Some error", ngrokLog.getErr());
        assertNull(ngrokLog.getMsg());
    }

    @Test
    public void testNgrokLogLvlNotSet() {
        // WHEN
        final NgrokLog ngrokLog = new NgrokLog("lvl=");

        // THEN
        assertEquals("NOTSET", ngrokLog.getLvl());
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
