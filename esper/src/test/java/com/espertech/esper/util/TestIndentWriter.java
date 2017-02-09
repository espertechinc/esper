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

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestIndentWriter extends TestCase {
    private final static String NEWLINE = System.getProperty("line.separator");
    private StringWriter buf;
    private IndentWriter writer;

    public void setUp() {
        buf = new StringWriter();
        writer = new IndentWriter(new PrintWriter(buf), 0, 2);
    }

    public void testWrite() {
        writer.println("a");
        assertWritten("a");

        writer.incrIndent();
        writer.println("a");
        assertWritten("  a");

        writer.incrIndent();
        writer.println("a");
        assertWritten("    a");

        writer.decrIndent();
        writer.println("a");
        assertWritten("  a");

        writer.decrIndent();
        writer.println("a");
        assertWritten("a");

        writer.decrIndent();
        writer.println("a");
        assertWritten("a");
    }

    public void testCtor() {
        try {
            new IndentWriter(new PrintWriter(buf), 0, -1);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            new IndentWriter(new PrintWriter(buf), -1, 11);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    private void assertWritten(String text) {
        assertEquals(text + NEWLINE, buf.toString());
        StringBuffer buffer = buf.getBuffer();
        buf.getBuffer().delete(0, buffer.length());
    }

}
