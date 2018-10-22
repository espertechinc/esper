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
package com.espertech.esper.compiler.internal.parse;

import junit.framework.TestCase;

public class TestASTUtil extends TestCase {
    public void testEscapeDot() {
        String[][] inout = new String[][]{
                {"a", "a"},
                {"", ""},
                {" ", " "},
                {".", "\\."},
                {". .", "\\. \\."},
                {"a.", "a\\."},
                {".a", "\\.a"},
                {"a.b", "a\\.b"},
                {"a..b", "a\\.\\.b"},
                {"a\\.b", "a\\.b"},
                {"a\\..b", "a\\.\\.b"},
                {"a.\\..b", "a\\.\\.\\.b"},
                {"a.b.c", "a\\.b\\.c"}
        };

        for (int i = 0; i < inout.length; i++) {
            String input = inout[i][0];
            String expected = inout[i][1];
            assertEquals("for input " + input, expected, ASTUtil.escapeDot(input));
        }
    }

    public void testUnescapeDot() {
        String[][] inout = new String[][]{
                {"a", "a"},
                {"", ""},
                {" ", " "},
                {".", "."},
                {" . .", " . ."},
                {"a\\.", "a."},
                {"\\.a", ".a"},
                {"a\\.b", "a.b"},
                {"a.b", "a.b"},
                {".a", ".a"},
                {"a.", "a."},
                {"a\\.\\.b", "a..b"},
                {"a\\..\\.b", "a...b"},
                {"a.\\..b", "a...b"},
                {"a\\..b", "a..b"},
                {"a.b\\.c", "a.b.c"},
        };

        for (int i = 0; i < inout.length; i++) {
            String input = inout[i][0];
            String expected = inout[i][1];
            assertEquals("for input " + input, expected, ASTUtil.unescapeDot(input));
        }
    }
}
