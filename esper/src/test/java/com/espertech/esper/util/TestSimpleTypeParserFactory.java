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

import junit.framework.TestCase;

public class TestSimpleTypeParserFactory extends TestCase {
    public void testGetParser() throws Exception {
        Object[][] tests = new Object[][]{
                {Boolean.class, "TrUe", true},
                {Boolean.class, "false", false},
                {boolean.class, "false", false},
                {boolean.class, "true", true},
                {int.class, "73737474 ", 73737474},
                {Integer.class, " -1 ", -1},
                {long.class, "123456789001222L", 123456789001222L},
                {Long.class, " -2 ", -2L},
                {Long.class, " -2L ", -2L},
                {Long.class, " -2l ", -2L},
                {Short.class, " -3 ", (short) -3},
                {short.class, "111", (short) 111},
                {Double.class, " -3d ", -3d},
                {double.class, "111.38373", 111.38373d},
                {Double.class, " -3.1D ", -3.1D},
                {Float.class, " -3f ", -3f},
                {float.class, "111.38373", 111.38373f},
                {Float.class, " -3.1F ", -3.1f},
                {Byte.class, " -3 ", (byte) -3},
                {byte.class, " 1 ", (byte) 1},
                {char.class, "ABC", 'A'},
                {Character.class, " AB", ' '},
                {String.class, "AB", "AB"},
                {String.class, " AB ", " AB "},
        };

        for (int i = 0; i < tests.length; i++) {
            SimpleTypeParser parser = SimpleTypeParserFactory.getParser((Class) tests[i][0]);
            assertEquals("error in row:" + i, tests[i][2], parser.parse((String) tests[i][1]));
        }
    }
}
