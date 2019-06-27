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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EPLInsertIntoPopulateUndStreamSelect {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoNamedWindowInheritsMap());
        execs.add(new EPLInsertIntoNamedWindowRep());
        execs.add(new EPLInsertIntoStreamInsertWWidenOA());
        execs.add(new EPLInsertIntoInvalid());
        return execs;
    }

    private static class EPLInsertIntoNamedWindowInheritsMap implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create objectarray schema Event();\n" +
                "create objectarray schema ChildEvent(id string, action string) inherits Event;\n" +
                "create objectarray schema Incident(name string, event Event);\n" +
                "@Name('window') create window IncidentWindow#keepall as Incident;\n" +
                "\n" +
                "on ChildEvent e\n" +
                "    merge IncidentWindow w\n" +
                "    where e.id = cast(w.event.id? as string)\n" +
                "    when not matched\n" +
                "        then insert (name, event) select 'ChildIncident', e \n" +
                "            where e.action = 'INSERT'\n" +
                "    when matched\n" +
                "        then update set w.event = e \n" +
                "            where e.action = 'INSERT'\n" +
                "        then delete\n" +
                "            where e.action = 'CLEAR';";
            env.compileDeployWBusPublicType(epl, new RegressionPath());

            env.sendEventObjectArray(new Object[]{"ID1", "INSERT"}, "ChildEvent");
            EventBean event = env.statement("window").iterator().next();
            Object[] underlying = (Object[]) event.getUnderlying();
            assertEquals("ChildIncident", underlying[0]);
            Object[] underlyingInner = (Object[]) ((EventBean) underlying[1]).getUnderlying();
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"ID1", "INSERT"}, underlyingInner);

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoNamedWindowRep implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                if (rep.isJsonProvidedClassEvent()) { // assertion uses inheritance of types
                    continue;
                }
                tryAssertionNamedWindow(env, rep);
            }
        }
    }

    private static class EPLInsertIntoStreamInsertWWidenOA implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionStreamInsertWWidenMap(env, rep);
            }
        }
    }

    private static class EPLInsertIntoInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionInvalid(env, rep);
            }
        }
    }

    private static void tryAssertionNamedWindow(RegressionEnvironment env, EventRepresentationChoice rep) {
        RegressionPath path = new RegressionPath();
        String schema = rep.getAnnotationText() + "@name('schema') create schema A as (myint int, mystr string);\n" +
            rep.getAnnotationText() + "create schema C as (addprop int) inherits A;\n";
        env.compileDeployWBusPublicType(schema, path);

        env.compileDeploy("create window MyWindow#time(5 days) as C", path);
        env.compileDeploy("@name('s0') select * from MyWindow", path).addListener("s0");

        // select underlying
        env.compileDeploy("@name('insert') insert into MyWindow select mya.* from A as mya", path);
        if (rep.isMapEvent()) {
            env.sendEventMap(makeMap(123, "abc"), "A");
        } else if (rep.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{123, "abc"}, "A");
        } else if (rep.isAvroEvent()) {
            env.sendEventAvro(makeAvro(env, 123, "abc"), "A");
        } else if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
            JsonObject object = new JsonObject();
            object.add("myint", 123);
            object.add("mystr", "abc");
            env.sendEventJson(object.toString(), "A");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "myint,mystr,addprop".split(","), new Object[]{123, "abc", null});
        env.undeployModuleContaining("insert");

        // select underlying plus property
        env.compileDeploy("insert into MyWindow select mya.*, 1 as addprop from A as mya", path);
        if (rep.isMapEvent()) {
            env.sendEventMap(makeMap(456, "def"), "A");
        } else if (rep.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{456, "def"}, "A");
        } else if (rep.isAvroEvent()) {
            env.sendEventAvro(makeAvro(env, 456, "def"), "A");
        } else if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
            JsonObject object = new JsonObject();
            object.add("myint", 456);
            object.add("mystr", "def");
            env.sendEventJson(object.toString(), "A");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "myint,mystr,addprop".split(","), new Object[]{456, "def", 1});

        env.undeployAll();
    }

    private static void tryAssertionStreamInsertWWidenMap(RegressionEnvironment env, EventRepresentationChoice rep) {

        RegressionPath path = new RegressionPath();
        String schemaSrc = rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedSrc.class) + "@name('schema') create schema Src as (myint int, mystr string)";
        env.compileDeployWBusPublicType(schemaSrc, path);

        env.compileDeploy(rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedD1.class) + "create schema D1 as (myint int, mystr string, addprop long)", path);
        String eplOne = "insert into D1 select 1 as addprop, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(env, path, rep, eplOne, "myint,mystr,addprop", new Object[]{123, "abc", 1L});

        env.compileDeploy(rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedD2.class) + "create schema D2 as (mystr string, myint int, addprop double)", path);
        String eplTwo = "insert into D2 select 1 as addprop, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(env, path, rep, eplTwo, "myint,mystr,addprop", new Object[]{123, "abc", 1d});

        env.compileDeploy(rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedD3.class) + "create schema D3 as (mystr string, addprop int)", path);
        String eplThree = "insert into D3 select 1 as addprop, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(env, path, rep, eplThree, "mystr,addprop", new Object[]{"abc", 1});

        env.compileDeploy(rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedD4.class) +  "create schema D4 as (myint int, mystr string)", path);
        String eplFour = "insert into D4 select mysrc.* from Src as mysrc";
        runStreamInsertAssertion(env, path, rep, eplFour, "myint,mystr", new Object[]{123, "abc"});

        String eplFive = "insert into D4 select mysrc.*, 999 as myint, 'xxx' as mystr from Src as mysrc";
        runStreamInsertAssertion(env, path, rep, eplFive, "myint,mystr", new Object[]{999, "xxx"});
        String eplSix = "insert into D4 select 999 as myint, 'xxx' as mystr, mysrc.* from Src as mysrc";
        runStreamInsertAssertion(env, path, rep, eplSix, "myint,mystr", new Object[]{999, "xxx"});

        env.undeployAll();
    }

    private static void tryAssertionInvalid(RegressionEnvironment env, EventRepresentationChoice rep) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy(rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedSrc.class) + "create schema Src as (myint int, mystr string)", path);

        // mismatch in type
        env.compileDeploy(rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedE1.class) + "create schema E1 as (myint long)", path);
        String message = !rep.isAvroEvent() ?
            "Type by name 'E1' in property 'myint' expected java.lang.Integer but receives java.lang.Long" :
            "Type by name 'E1' in property 'myint' expected schema '\"long\"' but received schema '\"int\"'";
        SupportMessageAssertUtil.tryInvalidCompile(env, path, "insert into E1 select mysrc.* from Src as mysrc", message);

        // mismatch in column name
        env.compileDeploy(rep.getAnnotationTextWJsonProvided(MyLocalJsonProvidedE2.class) + "create schema E2 as (someprop long)", path);
        SupportMessageAssertUtil.tryInvalidCompile(env, path, "insert into E2 select mysrc.*, 1 as otherprop from Src as mysrc",
            "Failed to find column 'otherprop' in target type 'E2' [insert into E2 select mysrc.*, 1 as otherprop from Src as mysrc]");

        env.undeployAll();
    }

    private static void runStreamInsertAssertion(RegressionEnvironment env, RegressionPath path, EventRepresentationChoice rep, String epl, String fields, Object[] expected) {
        env.compileDeploy("@name('s0') " + epl, path).addListener("s0");

        if (rep.isMapEvent()) {
            env.sendEventMap(makeMap(123, "abc"), "Src");
        } else if (rep.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{123, "abc"}, "Src");
        } else if (rep.isAvroEvent()) {
            EventType eventType = env.runtime().getEventTypeService().getEventType(env.deploymentId("schema"), "Src");
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(eventType));
            event.put("myint", 123);
            event.put("mystr", "abc");
            env.sendEventAvro(event, "Src");
        } else if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
            JsonObject object = new JsonObject();
            object.add("myint", 123);
            object.add("mystr", "abc");
            env.sendEventJson(object.toString(), "Src");
        } else {
            fail();
        }
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields.split(","), expected);
        env.undeployModuleContaining("s0");
    }

    private static Map<String, Object> makeMap(int myint, String mystr) {
        Map<String, Object> event = new HashMap<String, Object>();
        event.put("myint", myint);
        event.put("mystr", mystr);
        return event;
    }

    private static GenericData.Record makeAvro(RegressionEnvironment env, int myint, String mystr) {
        EventType eventType = env.runtime().getEventTypeService().getEventType(env.deploymentId("schema"), "A");
        GenericData.Record record = new GenericData.Record(SupportAvroUtil.getAvroSchema(eventType));
        record.put("myint", myint);
        record.put("mystr", mystr);
        return record;
    }

    public static class MyLocalJsonProvidedSrc implements Serializable {
        public int myint;
        public String mystr;
    }

    public static class MyLocalJsonProvidedD1 implements Serializable {
        public int myint;
        public String mystr;
        public long addprop;
    }

    public static class MyLocalJsonProvidedD2 implements Serializable {
        public int myint;
        public String mystr;
        public double addprop;
    }

    public static class MyLocalJsonProvidedD3 implements Serializable {
        public String mystr;
        public int addprop;
    }

    public static class MyLocalJsonProvidedD4 implements Serializable {
        public int myint;
        public String mystr;
    }

    public static class MyLocalJsonProvidedE1 implements Serializable {
        public long myint;
    }

    public static class MyLocalJsonProvidedE2 implements Serializable {
        public long someprop;
    }
}
