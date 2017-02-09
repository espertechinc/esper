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
 * Renderer for a String-value into XML strings.
 */
public class OutputValueRendererXMLString implements OutputValueRenderer {
    public OutputValueRendererXMLString() {
    }

    public void render(Object object, StringBuilder buf) {
        if (object == null) {
            buf.append("null");
            return;
        }

        xmlEncode(object.toString(), buf, true);
    }

    /**
     * XML-Encode the passed string.
     *
     * @param s                   string to encode
     * @param sb                  string buffer to populate
     * @param isEncodeSpecialChar true for encoding of special characters below ' ', false for leaving special chars
     */
    public static void xmlEncode(String s, StringBuilder sb, boolean isEncodeSpecialChar) {
        if (s == null || s.length() == 0) {
            return;
        }

        char c;
        int i;
        int len = s.length();
        String t;

        for (i = 0; i < len; i += 1) {
            c = s.charAt(i);
            // replace literal values with entities

            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '\'') {
                sb.append("&apos;");
            } else if (c == '\"') {
                sb.append("&quot;");
            } else {
                if ((c < ' ') && isEncodeSpecialChar) {
                    t = "000" + Integer.toHexString(c);
                    sb.append("\\u");
                    sb.append(t.substring(t.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }
    }
}
