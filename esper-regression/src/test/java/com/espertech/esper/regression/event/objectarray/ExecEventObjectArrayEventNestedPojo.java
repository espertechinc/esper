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
package com.espertech.esper.regression.event.objectarray;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.regression.event.map.TestSuiteEventMap;
import com.espertech.esper.supportregression.bean.SupportBeanCombinedProps;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.regression.event.map.ExecEventMap.makeMap;
import static com.espertech.esper.regression.event.objectarray.ExecEventObjectArray.getNestedKeyOA;
import static org.junit.Assert.*;

public class ExecEventObjectArrayEventNestedPojo implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        Pair<String[], Object[]> pair = getTestDef();
        configuration.addEventType("NestedObjectArr", pair.getFirst(), pair.getSecond());
    }

    public void run(EPServiceProvider epService) throws Exception {
        String statementText = "select " +
                "simple, object, nodefmap, map, " +
                "object.id as a1, nodefmap.key1? as a2, nodefmap.key2? as a3, nodefmap.key3?.key4 as a4, " +
                "map.objectOne as b1, map.simpleOne as b2, map.nodefmapOne.key2? as b3, map.mapOne.simpleTwo? as b4, " +
                "map.objectOne.indexed[1] as c1, map.objectOne.nested.nestedValue as c2," +
                "map.mapOne.simpleTwo as d1, map.mapOne.objectTwo as d2, map.mapOne.nodefmapTwo as d3, " +
                "map.mapOne.mapTwo as e1, map.mapOne.mapTwo.simpleThree as e2, map.mapOne.mapTwo.objectThree as e3, " +
                "map.mapOne.objectTwo.array[1].mapped('1ma').value as f1, map.mapOne.mapTwo.objectThree.id as f2" +
                " from NestedObjectArr";
        EPStatement statement = epService.getEPAdministrator().createEPL(statementText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Object[] testdata = getTestData();
        epService.getEPRuntime().sendEvent(testdata, "NestedObjectArr");

        // test all properties exist
        EventBean received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, "simple,object,nodefmap,map".split(","),
                new Object[]{"abc", new SupportBean_A("A1"), testdata[2], testdata[3]});
        EPAssertionUtil.assertProps(received, "a1,a2,a3,a4".split(","),
                new Object[]{"A1", "val1", null, null});
        EPAssertionUtil.assertProps(received, "b1,b2,b3,b4".split(","),
                new Object[]{getNestedKeyOA(testdata, 3, "objectOne"), 10, "val2", 300});
        EPAssertionUtil.assertProps(received, "c1,c2".split(","), new Object[]{2, "nestedValue"});
        EPAssertionUtil.assertProps(received, "d1,d2,d3".split(","),
                new Object[]{300, getNestedKeyOA(testdata, 3, "mapOne", "objectTwo"), getNestedKeyOA(testdata, 3, "mapOne", "nodefmapTwo")});
        EPAssertionUtil.assertProps(received, "e1,e2,e3".split(","),
                new Object[]{getNestedKeyOA(testdata, 3, "mapOne", "mapTwo"), 4000L, new SupportBean_B("B1")});
        EPAssertionUtil.assertProps(received, "f1,f2".split(","),
                new Object[]{"1ma0", "B1"});

        // assert type info
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from NestedObjectArr");
        EventType eventType = stmt.getEventType();

        String[] propertiesReceived = eventType.getPropertyNames();
        String[] propertiesExpected = new String[]{"simple", "object", "nodefmap", "map"};
        EPAssertionUtil.assertEqualsAnyOrder(propertiesReceived, propertiesExpected);
        assertEquals(String.class, eventType.getPropertyType("simple"));
        assertEquals(Map.class, eventType.getPropertyType("map"));
        assertEquals(Map.class, eventType.getPropertyType("nodefmap"));
        assertEquals(SupportBean_A.class, eventType.getPropertyType("object"));

        assertNull(eventType.getPropertyType("map.mapOne.simpleOne"));

        // nested POJO with generic return type
        listener.reset();
        epService.getEPAdministrator().getConfiguration().addEventType("MyNested", new String[]{"bean"}, new Object[]{MyNested.class});
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select * from MyNested(bean.insides.anyOf(i=>id = 'A'))");
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[]{new MyNested(Arrays.asList(new MyInside[]{new MyInside("A")}))}, "MyNested");
        assertTrue(listener.isInvoked());
    }

    private Object[] getTestData() {
        Map<String, Object> levelThree = makeMap(new Object[][]{
                {"simpleThree", 4000L},
                {"objectThree", new SupportBean_B("B1")},
        });

        Map<String, Object> levelTwo = makeMap(new Object[][]{
                {"simpleTwo", 300},
                {"objectTwo", SupportBeanCombinedProps.makeDefaultBean()},
                {"nodefmapTwo", makeMap(new Object[][]{{"key3", "val3"}})},
                {"mapTwo", levelThree},
        });

        Map<String, Object> levelOne = makeMap(new Object[][]{
                {"simpleOne", 10},
                {"objectOne", SupportBeanComplexProps.makeDefaultBean()},
                {"nodefmapOne", makeMap(new Object[][]{{"key2", "val2"}})},
                {"mapOne", levelTwo}
        });

        Object[] levelZero = {"abc", new SupportBean_A("A1"), makeMap(new Object[][]{{"key1", "val1"}}), levelOne};
        return levelZero;
    }

    private Pair<String[], Object[]> getTestDef() {
        Map<String, Object> levelThree = makeMap(new Object[][]{
                {"simpleThree", Long.class},
                {"objectThree", SupportBean_B.class},
        });

        Map<String, Object> levelTwo = makeMap(new Object[][]{
                {"simpleTwo", Integer.class},
                {"objectTwo", SupportBeanCombinedProps.class},
                {"nodefmapTwo", Map.class},
                {"mapTwo", levelThree},
        });

        Map<String, Object> levelOne = makeMap(new Object[][]{
                {"simpleOne", Integer.class},
                {"objectOne", SupportBeanComplexProps.class},
                {"nodefmapOne", Map.class},
                {"mapOne", levelTwo}
        });

        String[] levelZeroProps = {"simple", "object", "nodefmap", "map"};
        Object[] levelZeroTypes = {String.class, SupportBean_A.class, Map.class, levelOne};
        return new Pair<String[], Object[]>(levelZeroProps, levelZeroTypes);
    }

    public static class MyNested {
        private final List<MyInside> insides;

        private MyNested(List<MyInside> insides) {
            this.insides = insides;
        }

        public List<MyInside> getInsides() {
            return insides;
        }
    }

    public static class MyInside {
        private final String id;

        private MyInside(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    private final static Logger log = LoggerFactory.getLogger(TestSuiteEventMap.class);
}
