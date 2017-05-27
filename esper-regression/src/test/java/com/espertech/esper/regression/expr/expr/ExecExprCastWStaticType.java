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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

public class ExecExprCastWStaticType implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("anInt", String.class);
        map.put("anDouble", String.class);
        map.put("anLong", String.class);
        map.put("anFloat", String.class);
        map.put("anByte", String.class);
        map.put("anShort", String.class);
        map.put("intPrimitive", int.class);
        map.put("intBoxed", Integer.class);
        configuration.addEventType("TestEvent", map);
    }

    public void run(EPServiceProvider epService) throws Exception {

        String stmt = "select cast(anInt, int) as intVal, " +
                "cast(anDouble, double) as doubleVal, " +
                "cast(anLong, long) as longVal, " +
                "cast(anFloat, float) as floatVal, " +
                "cast(anByte, byte) as byteVal, " +
                "cast(anShort, short) as shortVal, " +
                "cast(intPrimitive, int) as intOne, " +
                "cast(intBoxed, int) as intTwo, " +
                "cast(intPrimitive, java.lang.Long) as longOne, " +
                "cast(intBoxed, long) as longTwo " +
                "from TestEvent";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmt);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("anInt", "100");
        map.put("anDouble", "1.4E-1");
        map.put("anLong", "-10");
        map.put("anFloat", "1.001");
        map.put("anByte", "0x0A");
        map.put("anShort", "223");
        map.put("intPrimitive", 10);
        map.put("intBoxed", 11);

        epService.getEPRuntime().sendEvent(map, "TestEvent");
        EventBean row = listener.assertOneGetNewAndReset();
        assertEquals(100, row.get("intVal"));
        assertEquals(0.14d, row.get("doubleVal"));
        assertEquals(-10L, row.get("longVal"));
        assertEquals(1.001f, row.get("floatVal"));
        assertEquals((byte) 10, row.get("byteVal"));
        assertEquals((short) 223, row.get("shortVal"));
        assertEquals(10, row.get("intOne"));
        assertEquals(11, row.get("intTwo"));
        assertEquals(10L, row.get("longOne"));
        assertEquals(11L, row.get("longTwo"));
    }
}
