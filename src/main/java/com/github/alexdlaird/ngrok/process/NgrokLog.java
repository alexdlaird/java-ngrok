/*
 * Copyright (c) 2021-2024 Alex Laird
 *
 * SPDX-License-Identifier: MIT
 */

package com.github.alexdlaird.ngrok.process;

import static com.github.alexdlaird.util.StringUtils.isBlank;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                    default:
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
                default:
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

        return List.of(tokens.toArray(new String[]{}));
    }
}
