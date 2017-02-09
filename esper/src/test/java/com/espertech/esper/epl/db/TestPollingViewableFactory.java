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
package com.espertech.esper.epl.db;

import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.core.support.SupportStatementContextFactory;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.DBStatementStreamSpec;
import com.espertech.esper.epl.spec.ViewSpec;
import com.espertech.esper.supportunit.epl.SupportDatabaseService;
import com.espertech.esper.view.EventCollection;
import junit.framework.TestCase;

import java.math.BigDecimal;

public class TestPollingViewableFactory extends TestCase {
    public void testDBStatementViewFactory() throws Exception {
        DBStatementStreamSpec spec = new DBStatementStreamSpec("s0", ViewSpec.EMPTY_VIEWSPEC_ARRAY,
                "mydb_part", "select * from mytesttable where mybigint=${idnum}", null);

        EventCollection eventCollection = DatabasePollingViewableFactory.createDBStatementView(1, 1, spec,
                SupportDatabaseService.makeService(),
                SupportEventAdapterService.getService(), null, null, null, true, new DataCacheFactory(), SupportStatementContextFactory.makeContext());

        assertEquals(Long.class, eventCollection.getEventType().getPropertyType("mybigint"));
        assertEquals(String.class, eventCollection.getEventType().getPropertyType("myvarchar"));
        assertEquals(Boolean.class, eventCollection.getEventType().getPropertyType("mybool"));
        assertEquals(BigDecimal.class, eventCollection.getEventType().getPropertyType("mynumeric"));
        assertEquals(BigDecimal.class, eventCollection.getEventType().getPropertyType("mydecimal"));
    }

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
                result = DatabasePollingViewableFactory.lexSampleSQL(testcases[i][0]).trim();
            } catch (Exception ex) {
                fail("failed case with exception:" + testcases[i][0]);
            }
            String expected = testcases[i][1].trim();
            assertEquals("failed case " + i + " :" + testcases[i][0], expected, result);
        }
    }
}
