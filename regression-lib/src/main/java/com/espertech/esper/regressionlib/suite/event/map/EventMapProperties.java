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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EventMapProperties {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventMapArrayProperty());
        execs.add(new EventMapMappedProperty());
        execs.add(new EventMapMapNamePropertyNested());
        execs.add(new EventMapMapNameProperty());
        return execs;
    }

    private static class EventMapArrayProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select p0[0] as a, p0[1] as b, p1[0].intPrimitive as c, p1[1] as d, p0 as e from MyArrayMap");
            env.addListener("s0");

            int[] p0 = new int[]{1, 2, 3};
            SupportBean[] beans = new SupportBean[]{new SupportBean("e1", 5), new SupportBean("e2", 6)};
            Map<String, Object> theEvent = EventMapCore.makeMap(new Object[][]{{"p0", p0}, {"p1", beans}});
            env.sendEventMap(theEvent, "MyArrayMap");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 5, beans[1], p0});
            EventType eventType = env.statement("s0").getEventType();
            assertEquals(Integer.class, eventType.getPropertyType("a"));
            assertEquals(Integer.class, eventType.getPropertyType("b"));
            assertEquals(Integer.class, eventType.getPropertyType("c"));
            assertEquals(SupportBean.class, eventType.getPropertyType("d"));
            assertEquals(int[].class, eventType.getPropertyType("e"));
            env.undeployAll();

            env.compileDeploy("@name('s0') select outer.p0[0] as a, outer.p0[1] as b, outer.p1[0].intPrimitive as c, outer.p1[1] as d, outer.p0 as e from MyArrayMapOuter");
            env.addListener("s0");

            Map<String, Object> eventOuter = EventMapCore.makeMap(new Object[][]{{"outer", theEvent}});
            env.sendEventMap(eventOuter, "MyArrayMapOuter");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a,b,c,d".split(","), new Object[]{1, 2, 5, beans[1]});
            eventType = env.statement("s0").getEventType();
            assertEquals(Integer.class, eventType.getPropertyType("a"));
            assertEquals(Integer.class, eventType.getPropertyType("b"));
            assertEquals(Integer.class, eventType.getPropertyType("c"));
            assertEquals(SupportBean.class, eventType.getPropertyType("d"));
            assertEquals(int[].class, eventType.getPropertyType("e"));

            env.undeployAll();
        }
    }

    private static class EventMapMappedProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select p0('k1') as a from MyMappedPropertyMap");
            env.addListener("s0");

            Map<String, Object> eventVal = new HashMap<String, Object>();
            eventVal.put("k1", "v1");
            Map<String, Object> theEvent = EventMapCore.makeMap(new Object[][]{{"p0", eventVal}});
            env.sendEventMap(theEvent, "MyMappedPropertyMap");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a".split(","), new Object[]{"v1"});
            assertEquals(Object.class, env.statement("s0").getEventType().getPropertyType("a"));
            env.undeployAll();

            env.compileDeploy("@name('s0') select outer.p0('k1') as a from MyMappedPropertyMapOuter");
            env.addListener("s0");

            Map<String, Object> eventOuter = EventMapCore.makeMap(new Object[][]{{"outer", theEvent}});
            env.sendEventMap(eventOuter, "MyMappedPropertyMapOuter");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a".split(","), new Object[]{"v1"});
            assertEquals(Object.class, env.statement("s0").getEventType().getPropertyType("a"));
            env.undeployModuleContaining("s0");

            // test map that contains a bean which has a map property
            env.compileDeploy("@name('s0') select outerTwo.mapProperty('xOne') as a from MyMappedPropertyMapOuterTwo");
            env.addListener("s0");

            Map<String, Object> eventOuterTwo = EventMapCore.makeMap(new Object[][]{{"outerTwo", SupportBeanComplexProps.makeDefaultBean()}});
            env.sendEventMap(eventOuterTwo, "MyMappedPropertyMapOuterTwo");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a".split(","), new Object[]{"yOne"});
            assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("a"));

            env.undeployAll();
        }
    }

    private static class EventMapMapNamePropertyNested implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select outer.p0.n0 as a, outer.p1[0].n0 as b, outer.p1[1].n0 as c, outer.p0 as d, outer.p1 as e from MyArrayMapTwo");
            env.addListener("s0");

            Map<String, Object> n0Bean1 = EventMapCore.makeMap(new Object[][]{{"n0", 1}});
            Map<String, Object> n0Bean21 = EventMapCore.makeMap(new Object[][]{{"n0", 2}});
            Map<String, Object> n0Bean22 = EventMapCore.makeMap(new Object[][]{{"n0", 3}});
            Map[] n0Bean2 = new Map[]{n0Bean21, n0Bean22};
            Map<String, Object> theEvent = EventMapCore.makeMap(new Object[][]{{"p0", n0Bean1}, {"p1", n0Bean2}});
            Map<String, Object> eventOuter = EventMapCore.makeMap(new Object[][]{{"outer", theEvent}});
            env.sendEventMap(eventOuter, "MyArrayMapTwo");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 3, n0Bean1, n0Bean2});
            EventType eventType = env.statement("s0").getEventType();
            assertEquals(Integer.class, eventType.getPropertyType("a"));
            assertEquals(Integer.class, eventType.getPropertyType("b"));
            assertEquals(Integer.class, eventType.getPropertyType("c"));
            assertEquals(Map.class, eventType.getPropertyType("d"));
            assertEquals(Map[].class, eventType.getPropertyType("e"));

            env.undeployAll();
            env.compileDeploy("@name('s0') select outer.p0.n0? as a, outer.p1[0].n0? as b, outer.p1[1]?.n0 as c, outer.p0? as d, outer.p1? as e from MyArrayMapTwo");
            env.addListener("s0");

            env.sendEventMap(eventOuter, "MyArrayMapTwo");

            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a,b,c,d,e".split(","), new Object[]{1, 2, 3, n0Bean1, n0Bean2});
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("a"));

            env.undeployAll();
        }
    }

    private static class EventMapMapNameProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select p0.n0 as a, p1[0].n0 as b, p1[1].n0 as c, p0 as d, p1 as e from MyMapWithAMap");
            env.addListener("s0");

            Map<String, Object> n0Bean1 = EventMapCore.makeMap(new Object[][]{{"n0", 1}});
            Map<String, Object> n0Bean21 = EventMapCore.makeMap(new Object[][]{{"n0", 2}});
            Map<String, Object> n0Bean22 = EventMapCore.makeMap(new Object[][]{{"n0", 3}});
            Map[] n0Bean2 = new Map[]{n0Bean21, n0Bean22};
            Map<String, Object> theEvent = EventMapCore.makeMap(new Object[][]{{"p0", n0Bean1}, {"p1", n0Bean2}});
            env.sendEventMap(theEvent, "MyMapWithAMap");

            EventBean eventResult = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(eventResult, "a,b,c,d".split(","), new Object[]{1, 2, 3, n0Bean1});
            Map[] valueE = (Map[]) eventResult.get("e");
            assertEquals(valueE[0], n0Bean2[0]);
            assertEquals(valueE[1], n0Bean2[1]);

            EventType eventType = env.statement("s0").getEventType();
            assertEquals(Integer.class, eventType.getPropertyType("a"));
            assertEquals(Integer.class, eventType.getPropertyType("b"));
            assertEquals(Integer.class, eventType.getPropertyType("c"));
            assertEquals(Map.class, eventType.getPropertyType("d"));
            assertEquals(Map[].class, eventType.getPropertyType("e"));

            env.undeployAll();
        }
    }
}
