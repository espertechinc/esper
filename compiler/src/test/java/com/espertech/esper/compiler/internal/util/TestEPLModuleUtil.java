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
package com.espertech.esper.compiler.internal.util;

import junit.framework.TestCase;

import java.util.List;

public class TestEPLModuleUtil extends TestCase {
    public void testParse() throws Exception {
        String epl;

        epl = "/* Comment One */\n" +
                "select * from A;";
        runAssertion(epl, new EPLModuleParseItem(epl.replace(";", ""), 1, 0, 33, 2, 2, 2));

        epl = "/* Comment One */ select * from A;\n" +
                "/* Comment Two */  select   *  from  B ;\n";
        runAssertion(epl, new EPLModuleParseItem("/* Comment One */ select * from A", 1, 0, 33, 1, 1, 1),
                new EPLModuleParseItem("/* Comment Two */  select   *  from  B", 2, 34, 73, 2, 2, 2));

        epl = "select /* Comment One\n\r; */ *, ';', \";\" from A order by x;; ;\n\n \n;\n" +
                "/* Comment Two */  select   *  from  B ;\n";
        runAssertion(epl, new EPLModuleParseItem("select /* Comment One\n\r; */ *, ';', \";\" from A order by x", 1, 0, 57, 2, 1, 2),
                new EPLModuleParseItem("/* Comment Two */  select   *  from  B", 6, 63, 102, 6, 6, 6));
    }
    
    private void runAssertion(String epl, EPLModuleParseItem ... expecteds) throws Exception {
        List<EPLModuleParseItem> result = EPLModuleUtil.parse(epl);
        assertEquals(result.size(), expecteds.length);
        for (int i = 0; i < expecteds.length; i++) {
            String message = "failed at epl:\n-----\n" + epl + "-----\nfailed at module item #" + i;
            assertEquals(message, expecteds[i].getExpression(), result.get(i).getExpression());
            assertEquals(message, expecteds[i].getLineNum(), result.get(i).getLineNum());
            assertEquals(message, expecteds[i].getStartChar(), result.get(i).getStartChar());
            assertEquals(message, expecteds[i].getEndChar(), result.get(i).getEndChar());
            assertEquals(message, expecteds[i].getLineNumEnd(), result.get(i).getLineNumEnd());
            assertEquals(message, expecteds[i].getLineNumContent(), result.get(i).getLineNumContent());
            assertEquals(message, expecteds[i].getLineNumContentEnd(), result.get(i).getLineNumContentEnd());
        }
    }
}
