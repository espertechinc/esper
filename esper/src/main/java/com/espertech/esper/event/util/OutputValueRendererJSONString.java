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
package com.espertech.esper.event.util;

/**
 * Renderer for a String-value into JSON strings.
 */
public class OutputValueRendererJSONString implements OutputValueRenderer {
    public void render(Object object, StringBuilder buf) {
        if (object == null) {
            buf.append("null");
            return;
        }

        enquote(object.toString(), buf);
    }

    /**
     * JSON-Enquote the passed string.
     *
     * @param s  string to enqoute
     * @param sb buffer to populate
     */
    public static void enquote(String s, StringBuilder sb) {
        if (s == null || s.length() == 0) {
            sb.append("\"\"");
            return;
        }

        char c;
        int i;
        int len = s.length();
        String t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            c = s.charAt(i);
            if ((c == '\\') || (c == '"')) {
                sb.append('\\');
                sb.append(c);
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\f') {
                sb.append("\\f");
            } else if (c == '\r') {
                sb.append("\\r");
            } else {
                if (c < ' ') {
                    t = "000" + Integer.toHexString(c);
                    sb.append("\\u");
                    sb.append(t.substring(t.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
    }
}
