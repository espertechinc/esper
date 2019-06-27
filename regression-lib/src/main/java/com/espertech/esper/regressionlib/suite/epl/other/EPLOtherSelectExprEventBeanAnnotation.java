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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.junit.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertTrue;

public class EPLOtherSelectExprEventBeanAnnotation {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherSelectExprEventBeanAnnoSimple());
        execs.add(new EPLOtherSelectExprEventBeanAnnoWSubquery());
        return execs;
    }

    private static class EPLOtherSelectExprEventBeanAnnoSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                runAssertionEventBeanAnnotation(env, rep);
            }
        }
    }

    private static class EPLOtherSelectExprEventBeanAnnoWSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test non-named-window
            RegressionPath path = new RegressionPath();
            env.compileDeployWBusPublicType("create objectarray schema MyEvent(col1 string, col2 string)", path);

            String eplInsert = "@name('insert') insert into DStream select " +
                "(select * from MyEvent#keepall) @eventbean as c0 " +
                "from SupportBean";
            env.compileDeploy(eplInsert, path);

            for (String prop : "c0".split(",")) {
                assertFragment(prop, env.statement("insert").getEventType(), "MyEvent", true);
            }

            // test consuming statement
            String[] fields = "f0,f1".split(",");
            env.compileDeploy("@name('s0') select " +
                "c0 as f0, " +
                "c0.lastOf().col1 as f1 " +
                "from DStream", path).addListener("s0");

            Object[] eventOne = new Object[]{"E1", null};
            env.sendEventObjectArray(eventOne, "MyEvent");
            env.sendEventBean(new SupportBean());
            EventBean out = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(out, fields, new Object[]{new Object[]{eventOne}, "E1"});

            Object[] eventTwo = new Object[]{"E2", null};
            env.sendEventObjectArray(eventTwo, "MyEvent");
            env.sendEventBean(new SupportBean());
            out = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(out, fields, new Object[]{new Object[]{eventOne, eventTwo}, "E2"});

            env.undeployAll();
        }
    }

    private static void runAssertionEventBeanAnnotation(RegressionEnvironment env, EventRepresentationChoice rep) {
        RegressionPath path = new RegressionPath();
        env.compileDeployWBusPublicType(rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEvent.class) + "@name('schema') create schema MyEvent(col1 string)", path);

        String eplInsert = "@name('insert') insert into DStream select " +
            "last(*) @eventbean as c0, " +
            "window(*) @eventbean as c1, " +
            "prevwindow(s0) @eventbean as c2 " +
            "from MyEvent#length(2) as s0";
        env.compileDeploy(eplInsert, path).addListener("insert");

        for (String prop : "c0,c1,c2".split(",")) {
            assertFragment(prop, env.statement("insert").getEventType(), "MyEvent", prop.equals("c1") || prop.equals("c2"));
        }

        // test consuming statement
        String[] fields = "f0,f1,f2,f3,f4,f5".split(",");
        env.compileDeploy("@name('s0') select " +
            "c0 as f0, " +
            "c0.col1 as f1, " +
            "c1 as f2, " +
            "c1.lastOf().col1 as f3, " +
            "c1 as f4, " +
            "c1.lastOf().col1 as f5 " +
            "from DStream", path).addListener("s0");
        env.compileDeploy("@name('s1') select * from MyEvent", path).addListener("s1");

        Object eventOne = sendEvent(env, rep, "E1");
        if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
            eventOne = env.listener("s1").assertOneGetNewAndReset().getUnderlying();
        }
        assertTrue(((Map) env.listener("insert").assertOneGetNewAndReset().getUnderlying()).get("c0") instanceof EventBean);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{eventOne, "E1", new Object[]{eventOne}, "E1", new Object[]{eventOne}, "E1"});

        Object eventTwo = sendEvent(env, rep, "E2");
        if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
            eventTwo = env.listener("s1").assertOneGetNewAndReset().getUnderlying();
        }
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{eventTwo, "E2", new Object[]{eventOne, eventTwo}, "E2", new Object[]{eventOne, eventTwo}, "E2"});

        // test SODA
        env.eplToModelCompileDeploy(eplInsert, path);

        // test invalid
        tryInvalidCompile(env, path, "@name('s0') select last(*) @xxx from MyEvent",
            "Failed to recognize select-expression annotation 'xxx', expected 'eventbean' in text 'last(*) @xxx'");

        env.undeployAll();
    }

    private static void assertFragment(String prop, EventType eventType, String fragmentTypeName, boolean indexed) {
        EventPropertyDescriptor desc = eventType.getPropertyDescriptor(prop);
        Assert.assertEquals(true, desc.isFragment());
        FragmentEventType fragment = eventType.getFragmentType(prop);
        Assert.assertEquals(fragmentTypeName, fragment.getFragmentType().getName());
        Assert.assertEquals(false, fragment.isNative());
        Assert.assertEquals(indexed, fragment.isIndexed());
    }

    private static Object sendEvent(RegressionEnvironment env, EventRepresentationChoice rep, String value) {
        Object eventOne;
        if (rep.isMapEvent()) {
            Map<String, Object> event = Collections.singletonMap("col1", value);
            env.sendEventMap(event, "MyEvent");
            eventOne = event;
        } else if (rep.isObjectArrayEvent()) {
            Object[] event = new Object[]{value};
            env.sendEventObjectArray(event, "MyEvent");
            eventOne = event;
        } else if (rep.isAvroEvent()) {
            Schema schema = SupportAvroUtil.getAvroSchema(env.statement("schema").getEventType());
            GenericData.Record event = new GenericData.Record(schema);
            event.put("col1", value);
            env.sendEventAvro(event, "MyEvent");
            eventOne = event;
        } else if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
            JsonObject object = new JsonObject().add("col1", value);
            env.sendEventJson(object.toString(), "MyEvent");
            eventOne = object.toString();
        } else {
            throw new IllegalStateException();
        }
        return eventOne;
    }

    public static class MyLocalJsonProvidedMyEvent implements Serializable {
        public String col1;
    }
}
