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
package com.espertech.esper.epl.parse;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.supportunit.epl.parse.SupportParserHelper;
import com.espertech.esper.util.StringValue;
import junit.framework.TestCase;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestASTFilterSpecHelper extends TestCase {
    public void testGetPropertyName() throws Exception {
        final String PROPERTY = "a('aa').b[1].c";

        // Should parse and result in the exact same property name
        Pair<Tree, CommonTokenStream> parsed = SupportParserHelper.parseEventProperty(PROPERTY);
        Tree propertyNameExprNode = parsed.getFirst().getChild(0);
        ASTUtil.dumpAST(propertyNameExprNode);
        String propertyName = ((RuleNode) propertyNameExprNode).getText();
        assertEquals(PROPERTY, propertyName);

        // Try AST with tokens separated, same property name
        parsed = SupportParserHelper.parseEventProperty("a(    'aa'   ). b [ 1 ] . c");
        propertyNameExprNode = parsed.getFirst().getChild(0);
        propertyName = ((RuleNode) propertyNameExprNode).getText();
        assertEquals(PROPERTY, propertyName);
    }

    public void testGetPropertyNameEscaped() throws Exception {
        final String PROPERTY = "a\\.b\\.c";
        Pair<Tree, CommonTokenStream> parsed = SupportParserHelper.parseEventProperty(PROPERTY);
        Tree propertyNameExprNode = parsed.getFirst().getChild(0);
        ASTUtil.dumpAST(propertyNameExprNode);
        String propertyName = ((RuleNode) propertyNameExprNode).getText();
        assertEquals(PROPERTY, propertyName);
    }

    public void testEscapeDot() throws Exception {
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

    public void testUnescapeIndexOf() throws Exception {
        Object[][] inout = new Object[][]{
                {"a", -1},
                {"", -1},
                {" ", -1},
                {".", 0},
                {" . .", 1},
                {"a.", 1},
                {".a", 0},
                {"a.b", 1},
                {"a..b", 1},
                {"a\\.b", -1},
                {"a.\\..b", 1},
                {"a\\..b", 3},
                {"a.b.c", 1},
                {"abc.", 3}
        };

        for (int i = 0; i < inout.length; i++) {
            String input = (String) inout[i][0];
            int expected = (Integer) inout[i][1];
            assertEquals("for input " + input, expected, StringValue.unescapedIndexOfDot(input));
        }
    }

    public void testUnescapeDot() throws Exception {
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

    private static final Logger log = LoggerFactory.getLogger(TestASTFilterSpecHelper.class);
}
