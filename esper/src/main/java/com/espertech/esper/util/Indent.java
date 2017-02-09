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

import java.util.Arrays;

/**
 * Utility class around indenting and formatting text.
 */
public class Indent {
    /**
     * Utility method to indent a text for a number of characters.
     *
     * @param numChars is the number of character to indent with spaces
     * @return the formatted string
     */
    public static String indent(int numChars) {
        if (numChars < 0) {
            throw new IllegalArgumentException("Number of characters less then zero");
        }
        char[] buf = new char[numChars];
        Arrays.fill(buf, ' ');
        return String.valueOf(buf);
    }
}
