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
package com.espertech.esper.common.internal.epl.expression.dot.walk;

public class DotEscaper {
    /**
     * Escape all unescape dot characters in the text (identifier only) passed in.
     *
     * @param identifierToEscape text to escape
     * @return text where dots are escaped
     */
    public static String escapeDot(String identifierToEscape) {
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
}

