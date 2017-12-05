/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.util;

import java.io.StringWriter;

public final class StringValue {

    /**
     * Parse the string literal consisting of text between double-quotes or single-quotes.
     *
     * @param value is the text wthin double or single quotes
     * @return parsed value
     */
    public static String parseString(String value) {
        if ((value.startsWith("\"")) && (value.endsWith("\"")) ||
                (value.startsWith("'")) && (value.endsWith("'"))) {
            if (value.length() > 1) {
                if (value.indexOf('\\') != -1) {
                    return unescape(value.substring(1, value.length() - 1));
                }
                return value.substring(1, value.length() - 1);
            }
        }

        throw new IllegalArgumentException("String value of '" + value + "' cannot be parsed");
    }

    /**
     * Find the index of an unescaped dot (.) character, or return -1 if none found.
     *
     * @param identifier text to find an un-escaped dot character
     * @return index of first unescaped dot
     */
    public static int unescapedIndexOfDot(String identifier) {
        int indexof = identifier.indexOf(".");
        if (indexof == -1) {
            return -1;
        }

        for (int i = 0; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (c != '.') {
                continue;
            }

            if (i > 0) {
                if (identifier.charAt(i - 1) == '\\') {
                    continue;
                }
            }

            return i;
        }

        return -1;
    }

    /**
     * Escape all unescape dot characters in the text (identifier only) passed in.
     *
     * @param identifierToEscape text to escape
     * @return text where dots are escaped
     */
    protected static String escapeDot(String identifierToEscape) {
        int indexof = identifierToEscape.indexOf(".");
        if (indexof == -1) {
            return identifierToEscape;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < identifierToEscape.length(); i++) {
            char c = identifierToEscape.charAt(i);
            if (c != '.') {
                builder.append(c);
                continue;
            }

            if (i > 0) {
                if (identifierToEscape.charAt(i - 1) == '\\') {
                    builder.append('.');
                    continue;
                }
            }

            builder.append('\\');
            builder.append('.');
        }

        return builder.toString();
    }

    /**
     * Un-Escape all escaped dot characters in the text (identifier only) passed in.
     *
     * @param identifierToUnescape text to un-escape
     * @return string
     */
    public static String unescapeDot(String identifierToUnescape) {
        int indexof = identifierToUnescape.indexOf(".");
        if (indexof == -1) {
            return identifierToUnescape;
        }
        indexof = identifierToUnescape.indexOf("\\");
        if (indexof == -1) {
            return identifierToUnescape;
        }

        StringBuilder builder = new StringBuilder();
        int index = -1;
        int max = identifierToUnescape.length() - 1;
        do {
            index++;
            char c = identifierToUnescape.charAt(index);
            if (c != '\\') {
                builder.append(c);
                continue;
            }
            if (index < identifierToUnescape.length() - 1) {
                if (identifierToUnescape.charAt(index + 1) == '.') {
                    builder.append('.');
                    index++;
                }
            }
        }
        while (index < max);

        return builder.toString();
    }

    public static String unescapeBacktick(String text) {
        int indexof = text.indexOf("`");
        if (indexof == -1) {
            return text;
        }

        StringBuilder builder = new StringBuilder();
        int index = -1;
        int max = text.length() - 1;
        boolean skip = false;
        do {
            index++;
            char c = text.charAt(index);
            if (c == '`') {
                skip = !skip;
            } else {
                builder.append(c);
            }
        }
        while (index < max);

        return builder.toString();
    }

    /**
     * Renders a constant as an EPL.
     *
     * @param writer   to output to
     * @param constant to render
     */
    public static void renderConstantAsEPL(StringWriter writer, Object constant) {
        if (constant == null) {
            writer.write("null");
            return;
        }

        if ((constant instanceof String) ||
                (constant instanceof Character)) {
            writer.write('\"');
            writer.write(constant.toString());
            writer.write('\"');
        } else if (constant instanceof Long) {
            writer.write(constant.toString() + "L");
        } else if (constant instanceof Double) {
            writer.write(constant.toString() + "d");
        } else if (constant instanceof Float) {
            writer.write(constant.toString() + "f");
        } else {
            writer.write(constant.toString());
        }
    }

    /**
     * Remove tick '`' character from a string start and end.
     *
     * @param tickedString delimited string
     * @return delimited string with ticks removed, if starting and ending with tick
     */
    public static String removeTicks(String tickedString) {
        int indexFirst = tickedString.indexOf('`');
        int indexLast = tickedString.lastIndexOf('`');
        if ((indexFirst != indexLast) && (indexFirst != -1) && (indexLast != -1)) {
            return tickedString.substring(indexFirst + 1, indexLast);
        }
        return tickedString;
    }

    private static String unescape(String s) {
        int i = 0, len = s.length();
        char c;
        StringBuilder sb = new StringBuilder(len);
        while (i < len) {
            c = s.charAt(i++);
            if (c == '\\') {
                if (i < len) {
                    c = s.charAt(i++);
                    if (c == 'u') {
                        c = (char) Integer.parseInt(s.substring(i, i + 4), 16);
                        i += 4;
                    } // add other cases here as desired...
                }
            } // fall through: \ escapes itself, quotes any character but u
            sb.append(c);
        }
        return sb.toString();
    }
}
