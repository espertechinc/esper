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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.EPStatement;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Collections;
import java.util.Map;

import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.*;


public class EPLInsertIntoPopulateCreateStream implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionCreateStream(env, rep);
        }

        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionCreateStreamTwo(env, rep);
        }

        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertPopulateFromNamedWindow(env, rep);
        }

        runAssertionObjectArrPropertyReorder(env);
    }

    private static void runAssertionObjectArrPropertyReorder(RegressionEnvironment env) {
        String epl = "create objectarray schema MyInner (p_inner string);\n" +
            "create objectarray schema MyOATarget (unfilled string, p0 string, p1 string, i0 MyInner);\n" +
            "create objectarray schema MyOASource (p0 string, p1 string, i0 MyInner);\n" +
            "insert into MyOATarget select p0, p1, i0, null as unfilled from MyOASource;\n" +
            "@name('s0') select * from MyOATarget;\n";
        env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

        env.sendEventObjectArray(new Object[]{"p0value", "p1value", new Object[]{"i"}}, "MyOASource");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "p0,p1".split(","), new Object[]{"p0value", "p1value"});

        env.undeployAll();
    }

    private static void runAssertPopulateFromNamedWindow(RegressionEnvironment env, EventRepresentationChoice type) {
        RegressionPath path = new RegressionPath();
        String schemaEPL = "create " + type.getOutputTypeCreateSchemaName() + " schema Node(nid string)";
        env.compileDeployWBusPublicType(schemaEPL, path);

        env.compileDeploy("create window NodeWindow#unique(nid) as Node", path);
        env.compileDeploy("insert into NodeWindow select * from Node", path);
        env.compileDeploy("create " + type.getOutputTypeCreateSchemaName() + " schema NodePlus(npid string, node Node)", path);
        env.compileDeploy("@name('s0') insert into NodePlus select 'E1' as npid, n1 as node from NodeWindow n1", path).addListener("s0");

        if (type.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{"n1"}, "Node");
        } else if (type.isMapEvent()) {
            env.sendEventMap(Collections.singletonMap("nid", "n1"), "Node");
        } else if (type.isAvroEvent()) {
            GenericData.Record genericRecord = new GenericData.Record(record("name").fields().requiredString("nid").endRecord());
            genericRecord.put("nid", "n1");
            env.sendEventAvro(genericRecord, "Node");
        } else {
            fail();
        }
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("E1", event.get("npid"));
        assertEquals("n1", event.get("node.nid"));
        EventBean fragment = (EventBean) event.getFragment("node");
        assertEquals("Node", fragment.getEventType().getName());

        env.undeployAll();
    }

    private static void runAssertionCreateStream(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        String epl = eventRepresentationEnum.getAnnotationText() + " create schema MyEvent(myId int);\n" +
            eventRepresentationEnum.getAnnotationText() + " create schema CompositeEvent(c1 MyEvent, c2 MyEvent, rule string);\n" +
            "insert into MyStream select c, 'additionalValue' as value from MyEvent c;\n" +
            "insert into CompositeEvent select e1.c as c1, e2.c as c2, '4' as rule " +
            "  from pattern [e1=MyStream -> e2=MyStream];\n" +
            eventRepresentationEnum.getAnnotationText() + " @Name('Target') select * from CompositeEvent;\n";
        env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("Target");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(makeEvent(10).values().toArray(), "MyEvent");
            env.sendEventObjectArray(makeEvent(11).values().toArray(), "MyEvent");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(makeEvent(10), "MyEvent");
            env.sendEventMap(makeEvent(11), "MyEvent");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            env.sendEventAvro(makeEventAvro(10), "MyEvent");
            env.sendEventAvro(makeEventAvro(11), "MyEvent");
        } else {
            fail();
        }
        EventBean theEvent = env.listener("Target").assertOneGetNewAndReset();
        assertEquals(10, theEvent.get("c1.myId"));
        assertEquals(11, theEvent.get("c2.myId"));
        assertEquals("4", theEvent.get("rule"));

        env.undeployAll();
    }

    private static void runAssertionCreateStreamTwo(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        RegressionPath path = new RegressionPath();
        String epl = eventRepresentationEnum.getAnnotationText() + " create schema MyEvent(myId int)\n;" +
            eventRepresentationEnum.getAnnotationText() + " create schema AllMyEvent as (myEvent MyEvent, class String, reverse boolean);\n" +
            eventRepresentationEnum.getAnnotationText() + " create schema SuspectMyEvent as (myEvent MyEvent, class String);\n";
        env.compileDeployWBusPublicType(epl, path);

        env.compileDeploy("@name('s0') insert into AllMyEvent " +
            "select c as myEvent, 'test' as class, false as reverse " +
            "from MyEvent(myId=1) c", path).addListener("s0");

        assertTrue(eventRepresentationEnum.matchesClass(env.statement("s0").getEventType().getUnderlyingType()));

        env.compileDeploy("@name('s1') insert into SuspectMyEvent " +
            "select c.myEvent as myEvent, class " +
            "from AllMyEvent(not reverse) c", path).addListener("s1");

        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(makeEvent(1).values().toArray(), "MyEvent");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(makeEvent(1), "MyEvent");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            env.sendEventAvro(makeEventAvro(1), "MyEvent");
        } else {
            fail();
        }

        assertCreateStreamTwo(eventRepresentationEnum, env.listener("s0").assertOneGetNewAndReset(), env.statement("s0"));
        assertCreateStreamTwo(eventRepresentationEnum, env.listener("s1").assertOneGetNewAndReset(), env.statement("s1"));

        env.undeployAll();
    }

    private static void assertCreateStreamTwo(EventRepresentationChoice eventRepresentationEnum, EventBean eventBean, EPStatement statement) {
        if (eventRepresentationEnum.isAvroEvent()) {
            assertEquals(1, eventBean.get("myEvent.myId"));
        } else {
            assertTrue(eventBean.get("myEvent") instanceof EventBean);
            assertEquals(1, ((EventBean) eventBean.get("myEvent")).get("myId"));
        }
        assertNotNull(statement.getEventType().getFragmentType("myEvent"));
    }

    private static Map<String, Object> makeEvent(int myId) {
        return Collections.<String, Object>singletonMap("myId", myId);
    }

    private static GenericData.Record makeEventAvro(int myId) {
        Schema schema = record("schema").fields().requiredInt("myId").endRecord();
        GenericData.Record record = new GenericData.Record(schema);
        record.put("myId", myId);
        return record;
    }
}
