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
package com.espertech.esper.regressionlib.suite.event.map;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanCombinedProps;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventMapNested {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventMapNestedInsertInto());
        execs.add(new EventMapNestedEventType());
        execs.add(new EventMapNestedNestedPojo());
        execs.add(new EventMapNestedIsExists());
        return execs;
    }

    private static class EventMapNestedInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String statementText = "insert into MyStream select map.mapOne as val1 from NestedMap#length(5)";
            env.compileDeploy(statementText, path);

            statementText = "@name('s0') select val1 as a from MyStream";
            env.compileDeploy(statementText, path).addListener("s0");

            Map<String, Object> testdata = getTestData();
            env.sendEventMap(testdata, "NestedMap");

            // test all properties exist
            String[] fields = "a".split(",");
            EventBean received = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(received, fields, new Object[]{EventMapCore.getNestedKeyMap(testdata, "map", "mapOne")});

            env.undeployAll();
        }
    }

    private static class EventMapNestedEventType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from NestedMap");
            EventType eventType = env.statement("s0").getEventType();

            String[] propertiesReceived = eventType.getPropertyNames();
            String[] propertiesExpected = new String[]{"simple", "object", "nodefmap", "map"};
            EPAssertionUtil.assertEqualsAnyOrder(propertiesReceived, propertiesExpected);
            assertEquals(String.class, eventType.getPropertyType("simple"));
            assertEquals(Map.class, eventType.getPropertyType("map"));
            assertEquals(Map.class, eventType.getPropertyType("nodefmap"));
            assertEquals(SupportBean_A.class, eventType.getPropertyType("object"));

            assertNull(eventType.getPropertyType("map.mapOne.simpleOne"));

            env.undeployAll();
        }
    }

    private static class EventMapNestedNestedPojo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select " +
                "simple, object, nodefmap, map, " +
                "object.id as a1, nodefmap.key1? as a2, nodefmap.key2? as a3, nodefmap.key3?.key4 as a4, " +
                "map.objectOne as b1, map.simpleOne as b2, map.nodefmapOne.key2? as b3, map.mapOne.simpleTwo? as b4, " +
                "map.objectOne.indexed[1] as c1, map.objectOne.nested.nestedValue as c2," +
                "map.mapOne.simpleTwo as d1, map.mapOne.objectTwo as d2, map.mapOne.nodefmapTwo as d3, " +
                "map.mapOne.mapTwo as e1, map.mapOne.mapTwo.simpleThree as e2, map.mapOne.mapTwo.objectThree as e3, " +
                "map.mapOne.objectTwo.array[1].mapped('1ma').value as f1, map.mapOne.mapTwo.objectThree.id as f2" +
                " from NestedMap#length(5)";
            env.compileDeploy(statementText).addListener("s0");

            Map<String, Object> testdata = getTestData();
            env.sendEventMap(testdata, "NestedMap");

            // test all properties exist
            EventBean received = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(received, "simple,object,nodefmap,map".split(","),
                new Object[]{"abc", new SupportBean_A("A1"), testdata.get("nodefmap"), testdata.get("map")});
            EPAssertionUtil.assertProps(received, "a1,a2,a3,a4".split(","),
                new Object[]{"A1", "val1", null, null});
            EPAssertionUtil.assertProps(received, "b1,b2,b3,b4".split(","),
                new Object[]{EventMapCore.getNestedKeyMap(testdata, "map", "objectOne"), 10, "val2", 300});
            EPAssertionUtil.assertProps(received, "c1,c2".split(","), new Object[]{2, "nestedValue"});
            EPAssertionUtil.assertProps(received, "d1,d2,d3".split(","),
                new Object[]{300, EventMapCore.getNestedKeyMap(testdata, "map", "mapOne", "objectTwo"), EventMapCore.getNestedKeyMap(testdata, "map", "mapOne", "nodefmapTwo")});
            EPAssertionUtil.assertProps(received, "e1,e2,e3".split(","),
                new Object[]{EventMapCore.getNestedKeyMap(testdata, "map", "mapOne", "mapTwo"), 4000L, new SupportBean_B("B1")});
            EPAssertionUtil.assertProps(received, "f1,f2".split(","),
                new Object[]{"1ma0", "B1"});

            // test partial properties exist
            testdata = getTestDataThree();
            env.sendEventMap(testdata, "NestedMap");

            received = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(received, "simple,object,nodefmap,map".split(","),
                new Object[]{"abc", new SupportBean_A("A1"), testdata.get("nodefmap"), testdata.get("map")});
            EPAssertionUtil.assertProps(received, "a1,a2,a3,a4".split(","),
                new Object[]{"A1", "val1", null, null});
            EPAssertionUtil.assertProps(received, "b1,b2,b3,b4".split(","),
                new Object[]{EventMapCore.getNestedKeyMap(testdata, "map", "objectOne"), null, null, null});
            EPAssertionUtil.assertProps(received, "c1,c2".split(","), new Object[]{null, null});
            EPAssertionUtil.assertProps(received, "d1,d2,d3".split(","),
                new Object[]{null, EventMapCore.getNestedKeyMap(testdata, "map", "mapOne", "objectTwo"), EventMapCore.getNestedKeyMap(testdata, "map", "mapOne", "nodefmapTwo")});
            EPAssertionUtil.assertProps(received, "e1,e2,e3".split(","),
                new Object[]{EventMapCore.getNestedKeyMap(testdata, "map", "mapOne", "mapTwo"), 4000L, null});
            EPAssertionUtil.assertProps(received, "f1,f2".split(","),
                new Object[]{"1ma0", null});

            env.undeployAll();
        }
    }

    private static class EventMapNestedIsExists implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select " +
                "exists(map.mapOne?) as a," +
                "exists(map.mapOne?.simpleOne) as b," +
                "exists(map.mapOne?.simpleTwo) as c," +
                "exists(map.mapOne?.mapTwo) as d," +
                "exists(map.mapOne.mapTwo?) as e," +
                "exists(map.mapOne.mapTwo.simpleThree?) as f," +
                "exists(map.mapOne.mapTwo.objectThree?) as g " +
                " from NestedMap#length(5)";
            env.compileDeploy(statementText).addListener("s0");

            Map<String, Object> testdata = getTestData();
            env.sendEventMap(testdata, "NestedMap");

            // test all properties exist
            String[] fields = "a,b,c,d,e,f,g".split(",");
            EventBean received = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(received, fields,
                new Object[]{true, false, true, true, true, true, true});

            // test partial properties exist
            testdata = getTestDataThree();
            env.sendEventMap(testdata, "NestedMap");

            received = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(received, fields,
                new Object[]{true, false, false, true, true, true, false});

            env.undeployAll();
        }
    }

    private static Map<String, Object> getTestData() {
        Map<String, Object> levelThree = EventMapCore.makeMap(new Object[][]{
            {"simpleThree", 4000L},
            {"objectThree", new SupportBean_B("B1")},
        });

        Map<String, Object> levelTwo = EventMapCore.makeMap(new Object[][]{
            {"simpleTwo", 300},
            {"objectTwo", SupportBeanCombinedProps.makeDefaultBean()},
            {"nodefmapTwo", EventMapCore.makeMap(new Object[][]{{"key3", "val3"}})},
            {"mapTwo", levelThree},
        });

        Map<String, Object> levelOne = EventMapCore.makeMap(new Object[][]{
            {"simpleOne", 10},
            {"objectOne", SupportBeanComplexProps.makeDefaultBean()},
            {"nodefmapOne", EventMapCore.makeMap(new Object[][]{{"key2", "val2"}})},
            {"mapOne", levelTwo}
        });

        Map<String, Object> levelZero = EventMapCore.makeMap(new Object[][]{
            {"simple", "abc"},
            {"object", new SupportBean_A("A1")},
            {"nodefmap", EventMapCore.makeMap(new Object[][]{{"key1", "val1"}})},
            {"map", levelOne}
        });

        return levelZero;
    }

    private static Map<String, Object> getTestDataThree() {
        Map<String, Object> levelThree = EventMapCore.makeMap(new Object[][]{
            {"simpleThree", 4000L},
        });

        Map<String, Object> levelTwo = EventMapCore.makeMap(new Object[][]{
            {"objectTwo", SupportBeanCombinedProps.makeDefaultBean()},
            {"nodefmapTwo", EventMapCore.makeMap(new Object[][]{{"key3", "val3"}})},
            {"mapTwo", levelThree},
        });

        Map<String, Object> levelOne = EventMapCore.makeMap(new Object[][]{
            {"simpleOne", null},
            {"objectOne", null},
            {"mapOne", levelTwo}
        });

        Map<String, Object> levelZero = EventMapCore.makeMap(new Object[][]{
            {"simple", "abc"},
            {"object", new SupportBean_A("A1")},
            {"nodefmap", EventMapCore.makeMap(new Object[][]{{"key1", "val1"}})},
            {"map", levelOne}
        });

        return levelZero;
    }
}