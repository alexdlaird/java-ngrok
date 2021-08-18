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
}
