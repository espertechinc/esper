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
package com.espertech.esper.rowregex;

import com.espertech.esper.epl.parse.EPLTreeWalkerListener;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import com.espertech.esper.supportunit.epl.parse.SupportParserHelper;
import junit.framework.TestCase;

import java.util.*;

public class TestEventRowRegexHelper extends TestCase {
    public void testVariableAnalysis() throws Exception {
        String[][] patternTests = new String[][]{
                {"A", "[A]", "[]"},
                {"A B", "[A, B]", "[]"},
                {"A B*", "[A]", "[B]"},
                {"A B B", "[A]", "[B]"},
                {"A B A", "[B]", "[A]"},
                {"A B+ C", "[A, C]", "[B]"},
                {"A B?", "[A, B]", "[]"},
                {"(A B)* C", "[C]", "[A, B]"},
                {"D (A B)+ (G H)? C", "[D, G, H, C]", "[A, B]"},
                {"A B | A C", "[A, B, C]", "[]"},
                {"(A B*) | (A+ C)", "[C]", "[B, A]"},
                {"(A | B) | (C | A)", "[A, B, C]", "[]"},
        };

        for (int i = 0; i < patternTests.length; i++) {
            String pattern = patternTests[i][0];
            String expression = "select * from MyEvent#keepall match_recognize (" +
                    "  partition by string measures A.string as a_string pattern ( " + pattern + ") define A as (A.value = 1) )";

            EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
            StatementSpecRaw raw = walker.getStatementSpec();

            RowRegexExprNode parent = raw.getMatchRecognizeSpec().getPattern();
            LinkedHashSet<String> singles = new LinkedHashSet<String>();
            LinkedHashSet<String> multiples = new LinkedHashSet<String>();

            EventRowRegexHelper.recursiveInspectVariables(parent, false, singles, multiples);

            String outText = "Failed in :" + pattern +
                    " result is : single " + Arrays.toString(singles.toArray()) +
                    " multiple " + Arrays.toString(multiples.toArray());

            assertEquals(outText, patternTests[i][1], Arrays.toString(singles.toArray()));
            assertEquals(outText, patternTests[i][2], Arrays.toString(multiples.toArray()));
        }
    }

    public void testVisibilityAnalysis() throws Exception {
        String[][] patternTests = new String[][]{
                {"A", "{}"},
                {"A B", "{B=[A]}"},
                {"A B*", "{B=[A]}"},
                {"A B B", "{B=[A]}"},
                {"A B A", "{A=[B], B=[A]}"},
                {"A B+ C", "{B=[A], C=[A, B]}"},
                {"(A B)+ C", "{B=[A], C=[A, B]}"},
                {"D (A B)+ (G H)? C", "{A=[D], B=[A, D], C=[A, B, D, G, H], G=[A, B, D], H=[A, B, D, G]}"},
                {"A B | A C", "{B=[A], C=[A]}"},
                {"(A B*) | (A+ C)", "{B=[A], C=[A]}"},
                {"A (B | C) D", "{B=[A], C=[A], D=[A, B, C]}"},
                {"(((A))) (((B))) (( C | (D E)))", "{B=[A], C=[A, B], D=[A, B], E=[A, B, D]}"},
                {"(A | B) C", "{C=[A, B]}"},
                {"(A | B) (C | A)", "{A=[B], C=[A, B]}"},
        };

        for (int i = 0; i < patternTests.length; i++) {
            String pattern = patternTests[i][0];
            String expected = patternTests[i][1];
            String expression = "select * from MyEvent#keepall match_recognize (" +
                    "  partition by string measures A.string as a_string pattern ( " + pattern + ") define A as (A.value = 1) )";

            EPLTreeWalkerListener walker = SupportParserHelper.parseAndWalkEPL(expression);
            StatementSpecRaw raw = walker.getStatementSpec();

            RowRegexExprNode parent = raw.getMatchRecognizeSpec().getPattern();

            Map<String, Set<String>> visibility = EventRowRegexHelper.determineVisibility(parent);

            // sort, for comparing
            Map<String, List<String>> visibilitySorted = new LinkedHashMap<String, List<String>>();
            List<String> tagsSorted = new ArrayList<String>(visibility.keySet());
            Collections.sort(tagsSorted);
            for (String tag : tagsSorted) {
                List<String> sorted = new ArrayList<String>(visibility.get(tag));
                Collections.sort(sorted);
                visibilitySorted.put(tag, sorted);
            }
            assertEquals("Failed in :" + pattern, expected, visibilitySorted.toString());
        }
    }

}
