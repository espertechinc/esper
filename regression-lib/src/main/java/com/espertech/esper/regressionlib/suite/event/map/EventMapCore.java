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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;

import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.*;

public class EventMapCore {
    private static Map<String, Object> map;

    static {
        map = new HashMap<String, Object>();
        map.put("myInt", 3);
        map.put("myString", "some string");
        map.put("beanA", SupportBeanComplexProps.makeDefaultBean());
    }

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventMapCoreMapNestedEventType());
        execs.add(new EventMapCoreMetadata());
        execs.add(new EventMapCoreNestedObjects());
        execs.add(new EventMapCoreQueryFields());
        execs.add(new EventMapCoreInvalidStatement());
        return execs;
    }

    private static class EventMapCoreMapNestedEventType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            assertNotNull(env.runtime().getEventTypeService().getEventTypePreconfigured("MyMap"));
            env.compileDeploy("@name('s0') select lev0name.lev1name.sb.theString as val from MyMap").addListener("s0");

            Map<String, Object> lev2data = new HashMap<String, Object>();
            lev2data.put("sb", new SupportBean("E1", 0));
            Map<String, Object> lev1data = new HashMap<String, Object>();
            lev1data.put("lev1name", lev2data);
            Map<String, Object> lev0data = new HashMap<String, Object>();
            lev0data.put("lev0name", lev1data);

            env.sendEventMap(lev0data, "MyMap");
            assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("val"));

            try {
                env.sendEventObjectArray(new Object[0], "MyMap");
                fail();
            } catch (EPException ex) {
                assertEquals("Event type named 'MyMap' has not been defined or is not a Object-array event type, the name 'MyMap' refers to a java.util.Map event type", ex.getMessage());
            }
            env.undeployAll();
        }
    }

    private static class EventMapCoreMetadata implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventType type = env.runtime().getEventTypeService().getEventTypePreconfigured("myMapEvent");
            assertEquals(EventTypeApplicationType.MAP, type.getMetadata().getApplicationType());
            assertEquals("myMapEvent", type.getMetadata().getName());

            EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("myInt", Integer.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("myString", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("beanA", SupportBeanComplexProps.class, null, false, false, false, false, true),
                new EventPropertyDescriptor("myStringArray", String[].class, String.class, false, false, true, false, false),
            }, type.getPropertyDescriptors());
        }
    }

    private static class EventMapCoreNestedObjects implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select beanA.simpleProperty as simple," +
                "beanA.nested.nestedValue as nested," +
                "beanA.indexed[1] as indexed," +
                "beanA.nested.nestedNested.nestedNestedValue as nestednested " +
                "from myMapEvent#length(5)";
            env.compileDeploy(statementText).addListener("s0");

            env.sendEventMap(map, "myMapEvent");
            assertEquals("nestedValue", env.listener("s0").getLastNewData()[0].get("nested"));
            assertEquals(2, env.listener("s0").getLastNewData()[0].get("indexed"));
            assertEquals("nestedNestedValue", env.listener("s0").getLastNewData()[0].get("nestednested"));

            env.undeployAll();
        }
    }

    private static class EventMapCoreQueryFields implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select myInt as intVal, myString as stringVal from myMapEvent#length(5)";
            env.compileDeploy(statementText).addListener("s0");


            // send Map<String, Object> event
            env.sendEventMap(map, "myMapEvent");
            assertEquals(3, env.listener("s0").getLastNewData()[0].get("intVal"));
            assertEquals("some string", env.listener("s0").getLastNewData()[0].get("stringVal"));

            // send Map base event
            Map mapNoType = new HashMap();
            mapNoType.put("myInt", 4);
            mapNoType.put("myString", "string2");
            env.sendEventMap(mapNoType, "myMapEvent");
            assertEquals(4, env.listener("s0").getLastNewData()[0].get("intVal"));
            assertEquals("string2", env.listener("s0").getLastNewData()[0].get("stringVal"));

            env.undeployAll();
        }
    }

    private static class EventMapCoreInvalidStatement implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "select XXX from myMapEvent#length(5)", "skip");
            tryInvalidCompile(env, "select myString * 2 from myMapEvent#length(5)", "skip");
            tryInvalidCompile(env, "select String.trim(myInt) from myMapEvent#length(5)", "skip");
        }
    }

    public static Map<String, Object> makeMap(String nameValuePairs) {
        Map result = new HashMap<String, Object>();
        String[] elements = nameValuePairs.split(",");
        for (int i = 0; i < elements.length; i++) {
            String[] pair = elements[i].split("=");
            if (pair.length == 2) {
                result.put(pair[0], pair[1]);
            }
        }
        return result;
    }

    public static Map<String, Object> makeMap(Object[][] entries) {
        Map result = new HashMap<String, Object>();
        if (entries == null) {
            return result;
        }
        for (int i = 0; i < entries.length; i++) {
            result.put(entries[i][0], entries[i][1]);
        }
        return result;
    }

    public static Properties makeProperties(Object[][] entries) {
        Properties result = new Properties();
        for (int i = 0; i < entries.length; i++) {
            Class clazz = (Class) entries[i][1];
            result.put(entries[i][0], clazz.getName());
        }
        return result;
    }

    public static Object getNestedKeyMap(Map<String, Object> root, String keyOne, String keyTwo) {
        Map map = (Map) root.get(keyOne);
        return map.get(keyTwo);
    }

    protected static Object getNestedKeyMap(Map<String, Object> root, String keyOne, String keyTwo, String keyThree) {
        Map map = (Map) root.get(keyOne);
        map = (Map) map.get(keyTwo);
        return map.get(keyThree);
    }

}
