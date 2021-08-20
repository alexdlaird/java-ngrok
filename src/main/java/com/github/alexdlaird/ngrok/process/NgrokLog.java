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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.github.alexdlaird.util.StringUtils.isBlank;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * An object containing a parsed log from the <code>ngrok</code> process.
 */
public class NgrokLog extends HashMap<String, String> {

    private final String line;
    private String t;
    private String lvl = INFO.getName();
    private String msg;
    private String err;
    private String addr;

    /**
     * Parse a String <code>ngrok</code> log to an object representation.
     *
     * @param line The raw log line from <code>ngrok</code>.
     */
    public NgrokLog(final String line) {
        this.line = line.strip();

        for (final String i : shellSplit(this.line)) {
            final String[] split = i.split("=", 2);
            final String key = split[0];
            String value = "";
            if (split.length > 1) {
                value = split[1];
            }

            if (key.equals("lvl")) {
                if (isBlank(value)) {
                    value = this.lvl;
                }

                value = value.toUpperCase();
                switch (value) {
                    case "CRIT":
                    case "ERR":
                    case "EROR":
                        value = SEVERE.getName();
                        break;
                    case "WARN":
                        value = WARNING.getName();
                        break;
                }
            }

            switch (key) {
                case "t":
                    this.t = value;
                    break;
                case "lvl":
                    this.lvl = value;
                    break;
                case "msg":
                    this.msg = value;
                    break;
                case "err":
                    this.err = value;
                    break;
                case "addr":
                    this.addr = value;
                    break;
            }

            put(key, value);
        }
    }

    public String getLine() {
        return line;
    }

    public String getT() {
        return t;
    }

    public String getLvl() {
        return lvl;
    }

    public String getMsg() {
        return msg;
    }

    public String getErr() {
        return err;
    }

    public String getAddr() {
        return addr;
    }

    private List<String> shellSplit(final CharSequence line) {
        final List<String> tokens = new ArrayList<>();

        boolean escaping = false;
        char quoteChar = ' ';
        boolean quoting = false;
        int lastCloseQuoteIndex = Integer.MIN_VALUE;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
            } else if (c == '\\' && !(quoting && quoteChar == '\'')) {
                escaping = true;
            } else if (quoting && c == quoteChar) {
                quoting = false;
                lastCloseQuoteIndex = i;
            } else if (!quoting && (c == '\'' || c == '"')) {
                quoting = true;
                quoteChar = c;
            } else if (!quoting && Character.isWhitespace(c)) {
                if (current.length() > 0 || lastCloseQuoteIndex == (i - 1)) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0 || lastCloseQuoteIndex == (line.length() - 1)) {
            tokens.add(current.toString());
        }

        return Collections.unmodifiableList(tokens);
    }
}
