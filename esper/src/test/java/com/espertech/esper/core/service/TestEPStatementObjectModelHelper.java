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
package com.espertech.esper.core.service;

import junit.framework.TestCase;

import java.io.StringWriter;

public class TestEPStatementObjectModelHelper extends TestCase {
    public void testRenderEPL() {
        assertEquals("null", tryConstant(null));
        assertEquals("\"\"", tryConstant(""));
        assertEquals("1", tryConstant(1));
        assertEquals("\"abc\"", tryConstant("abc"));
    }

    private String tryConstant(Object value) {
        StringWriter writer = new StringWriter();
        EPStatementObjectModelHelper.renderEPL(writer, value);
        return writer.toString();
    }
}
