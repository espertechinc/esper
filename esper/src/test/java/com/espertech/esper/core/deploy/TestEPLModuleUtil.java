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
package com.espertech.esper.core.deploy;

import junit.framework.TestCase;

import java.util.List;

public class TestEPLModuleUtil extends TestCase {
    public void testParse() throws Exception {

        Object[][] testdata = new Object[][]{
                {"/* Comment One */ select * from A;\n" +
                        "/* Comment Two */  select   *  from  B ;\n",
                        new EPLModuleParseItem[]{
                                new EPLModuleParseItem("/* Comment One */ select * from A", 1, 0, 33),
                                new EPLModuleParseItem("/* Comment Two */  select   *  from  B", 2, 34, 73)},
                },

                {"select /* Comment One\n\r; */ *, ';', \";\" from A order by x;; ;\n\n \n;\n" +
                        "/* Comment Two */  select   *  from  B ;\n",
                        new EPLModuleParseItem[]{
                                new EPLModuleParseItem("select /* Comment One\n\r; */ *, ';', \";\" from A order by x", 1, 0, 57),
                                new EPLModuleParseItem("/* Comment Two */  select   *  from  B", 6, 63, 102)},
                }
        };

        for (int i = 0; i < testdata.length; i++) {
            String input = (String) testdata[i][0];
            EPLModuleParseItem[] expected = (EPLModuleParseItem[]) testdata[i][1];
            List<EPLModuleParseItem> result = EPLModuleUtil.parse(input);

            assertEquals(expected.length, result.size());
            for (int j = 0; j < expected.length; j++) {
                String message = "failed at item " + i + " and segment " + j;
                assertEquals(message, expected[j].getExpression(), result.get(j).getExpression());
                assertEquals(message, expected[j].getLineNum(), result.get(j).getLineNum());
                assertEquals(message, expected[j].getStartChar(), result.get(j).getStartChar());
                assertEquals(message, expected[j].getEndChar(), result.get(j).getEndChar());
            }
        }
    }
}
