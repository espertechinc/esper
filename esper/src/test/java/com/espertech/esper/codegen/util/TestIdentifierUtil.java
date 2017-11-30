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
package com.espertech.esper.codegen.util;

import com.espertech.esper.codegen.util.IdentifierUtil;
import junit.framework.TestCase;

public class TestIdentifierUtil extends TestCase {
    public void testGetIdent() {
        assertNoop("a");
        assertNoop("ab");
        assertNoop("a_b");
        assertNoop("a__b");
        assertNoop("converts_0_or_not");
        assertNoop("0123456789");
        assertNoop("$");
        assertNoop("package");
        assertNoop("class");

        assertDiff("46", ".");
        assertDiff("32", " ");
        assertDiff("45", "-");
        assertDiff("43", "+");
        assertDiff("40", "(");
        assertDiff("59", ";");
        assertDiff("9", "\t");
        assertDiff("10", "\n");

        assertDiff("x32y32z", "x y z");
        assertDiff("a46b", "a.b");
    }

    private void assertDiff(String expected, String input) {
        assertEquals(expected, IdentifierUtil.getIdentifierMayStartNumeric(input));
    }

    private void assertNoop(String input) {
        assertEquals(input, IdentifierUtil.getIdentifierMayStartNumeric(input));
    }
}
