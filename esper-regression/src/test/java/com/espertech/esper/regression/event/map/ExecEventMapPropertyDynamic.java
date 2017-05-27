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
package com.espertech.esper.regression.event.map;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecEventMapPropertyDynamic implements RegressionExecution {
    public void run(EPServiceProvider epService) {
        runAssertionMapWithinMap(epService);
        runAssertionMapWithinMapExists(epService);
        runAssertionMapWithinMap2LevelsInvalid(epService);
    }

    private void runAssertionMapWithinMap(EPServiceProvider epService) {
        Properties properties = new Properties();
        properties.put("innermap", Map.class.getName());
        epService.getEPAdministrator().getConfiguration().addEventType("MyLevel2", properties);

        String statementText = "select " +
                "innermap.int? as t0, " +
                "innermap.innerTwo?.nested as t1, " +
                "innermap.innerTwo?.innerThree.nestedTwo as t2, " +
                "dynamicOne? as t3, " +
                "dynamicTwo? as t4, " +
                "indexed[1]? as t5, " +
                "mapped('keyOne')? as t6, " +
                "innermap.indexedTwo[0]? as t7, " +
                "innermap.mappedTwo('keyTwo')? as t8 " +
                "from MyLevel2#length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        HashMap map = new HashMap<String, Object>();
        map.put("dynamicTwo", 20L);
        map.put("innermap", makeMap(
                "int", 10,
                "indexedTwo", new int[]{-10},
                "mappedTwo", makeMap("keyTwo", "def"),
                "innerTwo", makeMap("nested", 30d,
                        "innerThree", makeMap("nestedTwo", 99))));
        map.put("indexed", new float[]{-1, -2, -3});
        map.put("mapped", makeMap("keyOne", "abc"));
        epService.getEPRuntime().sendEvent(map, "MyLevel2");
        assertResults(listener.assertOneGetNewAndReset(), new Object[]{10, 30d, 99, null, 20L, -2.0f, "abc", -10, "def"});

        map = new HashMap<String, Object>();
        map.put("innermap", makeMap(
                "indexedTwo", new int[]{},
                "mappedTwo", makeMap("yyy", "xxx"),
                "innerTwo", null));
        map.put("indexed", new float[]{});
        map.put("mapped", makeMap("xxx", "yyy"));
        epService.getEPRuntime().sendEvent(map, "MyLevel2");
        assertResults(listener.assertOneGetNewAndReset(), new Object[]{null, null, null, null, null, null, null, null, null});

        epService.getEPRuntime().sendEvent(new HashMap<String, Object>(), "MyLevel2");
        assertResults(listener.assertOneGetNewAndReset(), new Object[]{null, null, null, null, null, null, null, null, null});

        map = new HashMap<String, Object>();
        map.put("innermap", "xxx");
        map.put("indexed", null);
        map.put("mapped", "xxx");
        epService.getEPRuntime().sendEvent(map, "MyLevel2");
        assertResults(listener.assertOneGetNewAndReset(), new Object[]{null, null, null, null, null, null, null, null, null});

        statement.destroy();
    }

    private void runAssertionMapWithinMapExists(EPServiceProvider epService) {
        Properties properties = new Properties();
        properties.put("innermap", Map.class.getName());
        epService.getEPAdministrator().getConfiguration().addEventType("MyLevel2", properties);

        String statementText = "select " +
                "exists(innermap.int?) as t0, " +
                "exists(innermap.innerTwo?.nested) as t1, " +
                "exists(innermap.innerTwo?.innerThree.nestedTwo) as t2, " +
                "exists(dynamicOne?) as t3, " +
                "exists(dynamicTwo?) as t4, " +
                "exists(indexed[1]?) as t5, " +
                "exists(mapped('keyOne')?) as t6, " +
                "exists(innermap.indexedTwo[0]?) as t7, " +
                "exists(innermap.mappedTwo('keyTwo')?) as t8 " +
                "from MyLevel2#length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        HashMap map = new HashMap<String, Object>();
        map.put("dynamicTwo", 20L);
        map.put("innermap", makeMap(
                "int", 10,
                "indexedTwo", new int[]{-10},
                "mappedTwo", makeMap("keyTwo", "def"),
                "innerTwo", makeMap("nested", 30d,
                        "innerThree", makeMap("nestedTwo", 99))));
        map.put("indexed", new float[]{-1, -2, -3});
        map.put("mapped", makeMap("keyOne", "abc"));
        epService.getEPRuntime().sendEvent(map, "MyLevel2");
        assertResults(listener.assertOneGetNewAndReset(), new Object[]{true, true, true, false, true, true, true, true, true});

        map = new HashMap<String, Object>();
        map.put("innermap", makeMap(
                "indexedTwo", new int[]{},
                "mappedTwo", makeMap("yyy", "xxx"),
                "innerTwo", null));
        map.put("indexed", new float[]{});
        map.put("mapped", makeMap("xxx", "yyy"));
        epService.getEPRuntime().sendEvent(map, "MyLevel2");
        assertResults(listener.assertOneGetNewAndReset(), new Object[]{false, false, false, false, false, false, false, false, false});

        epService.getEPRuntime().sendEvent(new HashMap<String, Object>(), "MyLevel2");
        assertResults(listener.assertOneGetNewAndReset(), new Object[]{false, false, false, false, false, false, false, false, false});

        map = new HashMap<String, Object>();
        map.put("innermap", "xxx");
        map.put("indexed", null);
        map.put("mapped", "xxx");
        epService.getEPRuntime().sendEvent(map, "MyLevel2");
        assertResults(listener.assertOneGetNewAndReset(), new Object[]{false, false, false, false, false, false, false, false, false});

        statement.destroy();
    }

    private void runAssertionMapWithinMap2LevelsInvalid(EPServiceProvider epService) {
        Properties properties = new Properties();
        properties.put("innermap", Map.class.getName());
        epService.getEPAdministrator().getConfiguration().addEventType("MyLevel2", properties);

        String statementText = "select innermap.int as t0 from MyLevel2#length(5)";
        try {
            epService.getEPAdministrator().createEPL(statementText);
            fail();
        } catch (EPException ex) {
            // expected
        }

        statementText = "select innermap.int.inner2? as t0 from MyLevel2#length(5)";
        try {
            epService.getEPAdministrator().createEPL(statementText);
            fail();
        } catch (EPException ex) {
            // expected
        }

        statementText = "select innermap.int.inner2? as t0 from MyLevel2#length(5)";
        try {
            epService.getEPAdministrator().createEPL(statementText);
            fail();
        } catch (EPException ex) {
            // expected
        }
    }

    private void assertResults(EventBean theEvent, Object[] result) {
        for (int i = 0; i < result.length; i++) {
            assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }

    private Map makeMap(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        Object[][] pairs = new Object[keysAndValues.length / 2][2];
        for (int i = 0; i < keysAndValues.length; i++) {
            int index = i / 2;
            if (i % 2 == 0) {
                pairs[index][0] = keysAndValues[i];
            } else {
                pairs[index][1] = keysAndValues[i];
            }
        }
        return makeMap(pairs);
    }

    private Map makeMap(Object[][] pairs) {
        Map map = new HashMap();
        for (int i = 0; i < pairs.length; i++) {
            map.put(pairs[i][0], pairs[i][1]);
        }
        return map;
    }
}
