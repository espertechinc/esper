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

import java.io.StringWriter;

public class TestStringValue extends TestCase {
    public void testParse() {
        assertEquals("a", StringValue.parseString("\"a\""));
        assertEquals("", StringValue.parseString("\"\""));
        assertEquals("", StringValue.parseString("''"));
        assertEquals("b", StringValue.parseString("'b'"));
    }

    public void testInvalid() {
        tryInvalid("\"");
        tryInvalid("'");
    }

    public void testRenderEPL() {
        assertEquals("null", tryConstant(null));
        assertEquals("\"\"", tryConstant(""));
        assertEquals("1", tryConstant(1));
        assertEquals("\"abc\"", tryConstant("abc"));
    }

    private String tryConstant(Object value) {
        StringWriter writer = new StringWriter();
        StringValue.renderConstantAsEPL(writer, value);
        return writer.toString();
    }

    private void tryInvalid(String invalidString) {
        try {
            StringValue.parseString(invalidString);
        } catch (IllegalArgumentException ex) {
            // Expected exception
        }

    }
}
