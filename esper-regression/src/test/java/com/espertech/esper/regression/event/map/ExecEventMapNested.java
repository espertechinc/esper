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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanCombinedProps;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecEventMapNested implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("NestedMap", getTestDef());
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInsertInto(epService);
        runAssertionEventType(epService);
        runAssertionNestedPojo(epService);
        runAssertionIsExists(epService);
    }

    private void runAssertionInsertInto(EPServiceProvider epService) {
        String statementText = "insert into MyStream select " +
                "map.mapOne as val1" +
                " from NestedMap#length(5)";
        epService.getEPAdministrator().createEPL(statementText);

        statementText = "select val1 as a from MyStream";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Map<String, Object> testdata = getTestData();
        epService.getEPRuntime().sendEvent(testdata, "NestedMap");

        // test all properties exist
        String[] fields = "a".split(",");
        EventBean received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{ExecEventMap.getNestedKeyMap(testdata, "map", "mapOne")});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEventType(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from NestedMap");
        EventType eventType = stmt.getEventType();

        String[] propertiesReceived = eventType.getPropertyNames();
        String[] propertiesExpected = new String[]{"simple", "object", "nodefmap", "map"};
        EPAssertionUtil.assertEqualsAnyOrder(propertiesReceived, propertiesExpected);
        assertEquals(String.class, eventType.getPropertyType("simple"));
        assertEquals(Map.class, eventType.getPropertyType("map"));
        assertEquals(Map.class, eventType.getPropertyType("nodefmap"));
        assertEquals(SupportBean_A.class, eventType.getPropertyType("object"));

        assertNull(eventType.getPropertyType("map.mapOne.simpleOne"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNestedPojo(EPServiceProvider epService) {
        String statementText = "select " +
                "simple, object, nodefmap, map, " +
                "object.id as a1, nodefmap.key1? as a2, nodefmap.key2? as a3, nodefmap.key3?.key4 as a4, " +
                "map.objectOne as b1, map.simpleOne as b2, map.nodefmapOne.key2? as b3, map.mapOne.simpleTwo? as b4, " +
                "map.objectOne.indexed[1] as c1, map.objectOne.nested.nestedValue as c2," +
                "map.mapOne.simpleTwo as d1, map.mapOne.objectTwo as d2, map.mapOne.nodefmapTwo as d3, " +
                "map.mapOne.mapTwo as e1, map.mapOne.mapTwo.simpleThree as e2, map.mapOne.mapTwo.objectThree as e3, " +
                "map.mapOne.objectTwo.array[1].mapped('1ma').value as f1, map.mapOne.mapTwo.objectThree.id as f2" +
                " from NestedMap#length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Map<String, Object> testdata = getTestData();
        epService.getEPRuntime().sendEvent(testdata, "NestedMap");

        // test all properties exist
        EventBean received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, "simple,object,nodefmap,map".split(","),
                new Object[]{"abc", new SupportBean_A("A1"), testdata.get("nodefmap"), testdata.get("map")});
        EPAssertionUtil.assertProps(received, "a1,a2,a3,a4".split(","),
                new Object[]{"A1", "val1", null, null});
        EPAssertionUtil.assertProps(received, "b1,b2,b3,b4".split(","),
                new Object[]{ExecEventMap.getNestedKeyMap(testdata, "map", "objectOne"), 10, "val2", 300});
        EPAssertionUtil.assertProps(received, "c1,c2".split(","), new Object[]{2, "nestedValue"});
        EPAssertionUtil.assertProps(received, "d1,d2,d3".split(","),
                new Object[]{300, ExecEventMap.getNestedKeyMap(testdata, "map", "mapOne", "objectTwo"), ExecEventMap.getNestedKeyMap(testdata, "map", "mapOne", "nodefmapTwo")});
        EPAssertionUtil.assertProps(received, "e1,e2,e3".split(","),
                new Object[]{ExecEventMap.getNestedKeyMap(testdata, "map", "mapOne", "mapTwo"), 4000L, new SupportBean_B("B1")});
        EPAssertionUtil.assertProps(received, "f1,f2".split(","),
                new Object[]{"1ma0", "B1"});

        // test partial properties exist
        testdata = getTestDataThree();
        epService.getEPRuntime().sendEvent(testdata, "NestedMap");

        received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, "simple,object,nodefmap,map".split(","),
                new Object[]{"abc", new SupportBean_A("A1"), testdata.get("nodefmap"), testdata.get("map")});
        EPAssertionUtil.assertProps(received, "a1,a2,a3,a4".split(","),
                new Object[]{"A1", "val1", null, null});
        EPAssertionUtil.assertProps(received, "b1,b2,b3,b4".split(","),
                new Object[]{ExecEventMap.getNestedKeyMap(testdata, "map", "objectOne"), null, null, null});
        EPAssertionUtil.assertProps(received, "c1,c2".split(","), new Object[]{null, null});
        EPAssertionUtil.assertProps(received, "d1,d2,d3".split(","),
                new Object[]{null, ExecEventMap.getNestedKeyMap(testdata, "map", "mapOne", "objectTwo"), ExecEventMap.getNestedKeyMap(testdata, "map", "mapOne", "nodefmapTwo")});
        EPAssertionUtil.assertProps(received, "e1,e2,e3".split(","),
                new Object[]{ExecEventMap.getNestedKeyMap(testdata, "map", "mapOne", "mapTwo"), 4000L, null});
        EPAssertionUtil.assertProps(received, "f1,f2".split(","),
                new Object[]{"1ma0", null});
    }

    private void runAssertionIsExists(EPServiceProvider epService) {
        String statementText = "select " +
                "exists(map.mapOne?) as a," +
                "exists(map.mapOne?.simpleOne) as b," +
                "exists(map.mapOne?.simpleTwo) as c," +
                "exists(map.mapOne?.mapTwo) as d," +
                "exists(map.mapOne.mapTwo?) as e," +
                "exists(map.mapOne.mapTwo.simpleThree?) as f," +
                "exists(map.mapOne.mapTwo.objectThree?) as g " +
                " from NestedMap#length(5)";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Map<String, Object> testdata = getTestData();
        epService.getEPRuntime().sendEvent(testdata, "NestedMap");

        // test all properties exist
        String[] fields = "a,b,c,d,e,f,g".split(",");
        EventBean received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields,
                new Object[]{true, false, true, true, true, true, true});

        // test partial properties exist
        testdata = getTestDataThree();
        epService.getEPRuntime().sendEvent(testdata, "NestedMap");

        received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields,
                new Object[]{true, false, false, true, true, true, false});
    }

    private Map<String, Object> getTestDef() {
        Map<String, Object> levelThree = ExecEventMap.makeMap(new Object[][]{
                {"simpleThree", Long.class},
                {"objectThree", SupportBean_B.class},
        });

        Map<String, Object> levelTwo = ExecEventMap.makeMap(new Object[][]{
                {"simpleTwo", Integer.class},
                {"objectTwo", SupportBeanCombinedProps.class},
                {"nodefmapTwo", Map.class},
                {"mapTwo", levelThree},
        });

        Map<String, Object> levelOne = ExecEventMap.makeMap(new Object[][]{
                {"simpleOne", Integer.class},
                {"objectOne", SupportBeanComplexProps.class},
                {"nodefmapOne", Map.class},
                {"mapOne", levelTwo}
        });

        Map<String, Object> levelZero = ExecEventMap.makeMap(new Object[][]{
                {"simple", String.class},
                {"object", SupportBean_A.class},
                {"nodefmap", Map.class},
                {"map", levelOne}
        });

        return levelZero;
    }

    private Map<String, Object> getTestData() {
        Map<String, Object> levelThree = ExecEventMap.makeMap(new Object[][]{
                {"simpleThree", 4000L},
                {"objectThree", new SupportBean_B("B1")},
        });

        Map<String, Object> levelTwo = ExecEventMap.makeMap(new Object[][]{
                {"simpleTwo", 300},
                {"objectTwo", SupportBeanCombinedProps.makeDefaultBean()},
                {"nodefmapTwo", ExecEventMap.makeMap(new Object[][]{{"key3", "val3"}})},
                {"mapTwo", levelThree},
        });

        Map<String, Object> levelOne = ExecEventMap.makeMap(new Object[][]{
                {"simpleOne", 10},
                {"objectOne", SupportBeanComplexProps.makeDefaultBean()},
                {"nodefmapOne", ExecEventMap.makeMap(new Object[][]{{"key2", "val2"}})},
                {"mapOne", levelTwo}
        });

        Map<String, Object> levelZero = ExecEventMap.makeMap(new Object[][]{
                {"simple", "abc"},
                {"object", new SupportBean_A("A1")},
                {"nodefmap", ExecEventMap.makeMap(new Object[][]{{"key1", "val1"}})},
                {"map", levelOne}
        });

        return levelZero;
    }

    private Map<String, Object> getTestDataThree() {
        Map<String, Object> levelThree = ExecEventMap.makeMap(new Object[][]{
                {"simpleThree", 4000L},
        });

        Map<String, Object> levelTwo = ExecEventMap.makeMap(new Object[][]{
                {"objectTwo", SupportBeanCombinedProps.makeDefaultBean()},
                {"nodefmapTwo", ExecEventMap.makeMap(new Object[][]{{"key3", "val3"}})},
                {"mapTwo", levelThree},
        });

        Map<String, Object> levelOne = ExecEventMap.makeMap(new Object[][]{
                {"simpleOne", null},
                {"objectOne", null},
                {"mapOne", levelTwo}
        });

        Map<String, Object> levelZero = ExecEventMap.makeMap(new Object[][]{
                {"simple", "abc"},
                {"object", new SupportBean_A("A1")},
                {"nodefmap", ExecEventMap.makeMap(new Object[][]{{"key1", "val1"}})},
                {"map", levelOne}
        });

        return levelZero;
    }
}
