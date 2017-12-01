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

    private void tryInvalid(String invalidString) {
        try {
            StringValue.parseString(invalidString);
        } catch (IllegalArgumentException ex) {
            // Expected exception
        }

    }
}
