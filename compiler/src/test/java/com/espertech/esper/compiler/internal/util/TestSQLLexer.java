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

import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import junit.framework.TestCase;

public class TestSQLLexer extends TestCase {
    public void testLexSampleSQL() throws ExprValidationException {
        String[][] testcases = new String[][]{
                {"select * from A where a=b and c=d", "select * from A where 1=0 and a=b and c=d"},
                {"select * from A where 1=0", "select * from A where 1=0 and 1=0"},
                {"select * from A", "select * from A where 1=0"},
                {"select * from A group by x", "select * from A where 1=0 group by x"},
                {"select * from A having a>b", "select * from A where 1=0 having a>b"},
                {"select * from A order by d", "select * from A where 1=0 order by d"},
                {"select * from A group by a having b>c order by d", "select * from A where 1=0 group by a having b>c order by d"},
                {"select * from A where (7<4) group by a having b>c order by d", "select * from A where 1=0 and (7<4) group by a having b>c order by d"},
                {"select * from A union select * from B", "select * from A  where 1=0 union  select * from B where 1=0"},
                {"select * from A where a=2 union select * from B where 2=3", "select * from A where 1=0 and a=2 union  select * from B where 1=0 and 2=3"},
                {"select * from A union select * from B union select * from C", "select * from A  where 1=0 union  select * from B  where 1=0 union  select * from C where 1=0"},
        };

        for (int i = 0; i < testcases.length; i++) {
            String result = null;
            try {
                result = SQLLexer.lexSampleSQL(testcases[i][0]).trim();
            } catch (Exception ex) {
                fail("failed case with exception:" + testcases[i][0]);
            }
            String expected = testcases[i][1].trim();
            assertEquals("failed case " + i + " :" + testcases[i][0], expected, result);
        }
    }
}
