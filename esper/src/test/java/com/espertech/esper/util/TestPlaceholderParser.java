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

import java.util.List;

public class TestPlaceholderParser extends TestCase {
    public void testParseValid() throws Exception {
        Object[][] testdata = new Object[][]{
                {"a  a $${lib}", new Object[]{textF("a  a ${lib}")}},
                {"a ${lib} b", new Object[]{textF("a "), paramF("lib"), textF(" b")}},
                {"${lib} b", new Object[]{paramF("lib"), textF(" b")}},
                {"a${lib}", new Object[]{textF("a"), paramF("lib")}},
                {"$${lib}", new Object[]{textF("${lib}")}},
                {"$${lib} c", new Object[]{textF("${lib} c")}},
                {"a$${lib}", new Object[]{textF("a${lib}")}},
                {"sometext ${a} text $${d} ${e} text",
                        new Object[]{textF("sometext "), paramF("a"), textF(" text ${d} "), paramF("e"), textF(" text")}},
                {"$${lib} c $${lib}", new Object[]{textF("${lib} c ${lib}")}},
                {"$${lib}$${lib}", new Object[]{textF("${lib}${lib}")}},
                {"${xxx}$${lib}", new Object[]{paramF("xxx"), textF("${lib}")}},
                {"$${xxx}${lib}", new Object[]{textF("${xxx}"), paramF("lib")}},
                {"${lib} ${lib}", new Object[]{paramF("lib"), textF(" "), paramF("lib")}},
                {"${lib}${lib}", new Object[]{paramF("lib"), paramF("lib")}},
                {"$${lib", new Object[]{textF("${lib")}},
                {"lib}", new Object[]{textF("lib}")}}
        };

        for (int i = 0; i < testdata.length; i++) {
            testParseValid(testdata[i]);
        }
    }

    public void testParseValid(Object[] inputAndResults) throws Exception {
        String parseString = (String) inputAndResults[0];
        Object[] expected = (Object[]) inputAndResults[1];

        List<PlaceholderParser.Fragment> result = PlaceholderParser.parsePlaceholder(parseString);

        assertEquals("Incorrect count for '" + parseString + "'", expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Incorrect value for '" + parseString + "' at " + i, expected[i], result.get(i));
        }
    }

    public void testParseInvalid() {
        tryParseInvalid("${lib");
        tryParseInvalid("${lib} ${aa");
    }

    private void tryParseInvalid(String parseString) {
        try {
            PlaceholderParser.parsePlaceholder(parseString);
            fail();
        } catch (PlaceholderParseException ex) {
            // expected
        }
    }

    private PlaceholderParser.TextFragment textF(String text) {
        return new PlaceholderParser.TextFragment(text);
    }

    private PlaceholderParser.ParameterFragment paramF(String text) {
        return new PlaceholderParser.ParameterFragment(text);
    }
}
