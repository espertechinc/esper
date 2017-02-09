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

public class TestIndent extends TestCase {
    public void testIndent() {
        assertEquals("", Indent.indent(0));
        assertEquals(" ", Indent.indent(1));
        assertEquals("  ", Indent.indent(2));

        try {
            Indent.indent(-1);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

}
