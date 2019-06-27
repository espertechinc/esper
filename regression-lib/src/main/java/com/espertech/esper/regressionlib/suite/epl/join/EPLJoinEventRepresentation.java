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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EPLJoinEventRepresentation {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinJoinEventRepresentations());
        execs.add(new EPLJoinJoinMapEventNotUnique());
        execs.add(new EPLJoinJoinWrapperEventNotUnique());
        return execs;
    }

    private static class EPLJoinJoinEventRepresentations implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String jsonSchemas = "@public @buseventtype create json schema S0_JSON(id String, p00 int);\n" +
                "@public @buseventtype create json schema S1_JSON(id String, p00 int);\n" +
                "@public @buseventtype @JsonSchema(className='" + MyLocalJsonProvidedS0.class.getName() + "') create json schema S0_JSONCLASSPROVIDED();\n" +
                "@public @buseventtype @JsonSchema(className='" + MyLocalJsonProvidedS1.class.getName() + "') create json schema S1_JSONCLASSPROVIDED();\n";
            env.compileDeploy(jsonSchemas, path);
            AtomicInteger milestone = new AtomicInteger();

            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                String s0Type = "S0_" + rep.getName();
                String s1Type = "S1_" + rep.getName();
                String eplOne = "select S0.id as s0id, S1.id as s1id, S0.p00 as s0p00, S1.p00 as s1p00 from " + s0Type + "#keepall as S0, " +
                    s1Type + "#keepall as S1 where S0.id = S1.id";
                tryJoinAssertion(env, eplOne, rep, "s0id,s1id,s0p00,s1p00", milestone, path, MyLocalJsonProvidedWFields.class);
            }

            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                String s0Type = "S0_" + rep.getName();
                String s1Type = "S1_" + rep.getName();
                String eplTwo = "select * from " + s0Type + "#keepall as s0, " + s1Type + "#keepall as s1 where s0.id = s1.id";
                tryJoinAssertion(env, eplTwo, rep, "s0.id,s1.id,s0.p00,s1.p00", milestone, path, MyLocalJsonProvidedWildcard.class);
            }

            env.undeployAll();
        }

        private static void tryJoinAssertion(RegressionEnvironment env, String epl, EventRepresentationChoice rep, String columnNames, AtomicInteger milestone, RegressionPath path, Class jsonClass) {
            env.compileDeploy("@name('s0')" + rep.getAnnotationTextWJsonProvided(jsonClass) + epl, path).addListener("s0").milestoneInc(milestone);

            String s0Name = "S0_" + rep.getName();
            String s1Name = "S1_" + rep.getName();

            sendRepEvent(env, rep, s0Name, "a", 1);
            assertFalse(env.listener("s0").isInvoked());

            sendRepEvent(env, rep, s1Name, "a", 2);
            EventBean output = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(output, columnNames.split(","), new Object[]{"a", "a", 1, 2});
            assertTrue(rep.matchesClass(output.getUnderlying().getClass()));

            sendRepEvent(env, rep, s1Name, "b", 3);
            sendRepEvent(env, rep, s0Name, "c", 4);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployModuleContaining("s0");
        }
    }

    private static class EPLJoinJoinMapEventNotUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Test for Esper-122
            String joinStatement = "@name('s0') select S0.id, S1.id, S0.p00, S1.p00 from MapS0#keepall as S0, MapS1#keepall as S1" +
                " where S0.id = S1.id";
            env.compileDeployAddListenerMileZero(joinStatement, "s0");

            for (int i = 0; i < 100; i++) {
                if (i % 2 == 1) {
                    sendMapEvent(env, "MapS0", "a", 1);
                } else {
                    sendMapEvent(env, "MapS1", "a", 1);
                }
            }

            env.undeployAll();
        }
    }

    private static class EPLJoinJoinWrapperEventNotUnique implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Test for Esper-122
            String epl = "insert into S0Stream select 's0' as streamone, * from SupportBean;\n" +
                "insert into S1Stream select 's1' as streamtwo, * from SupportBean;\n" +
                "@name('s0') select * from S0Stream#keepall as a, S1Stream#keepall as b where a.intBoxed = b.intBoxed";
            env.compileDeployAddListenerMileZero(epl, "s0");

            for (int i = 0; i < 100; i++) {
                env.sendEventBean(new SupportBean());
            }

            env.undeployAll();
        }
    }

    private static void sendMapEvent(RegressionEnvironment env, String name, String id, int p00) {
        Map<String, Object> theEvent = new HashMap<String, Object>();
        theEvent.put("id", id);
        theEvent.put("p00", p00);
        env.sendEventMap(theEvent, name);
    }

    private static void sendRepEvent(RegressionEnvironment env, EventRepresentationChoice rep, String name, String id, int p00) {
        if (rep.isMapEvent()) {
            Map<String, Object> theEvent = new HashMap<>();
            theEvent.put("id", id);
            theEvent.put("p00", p00);
            env.sendEventMap(theEvent, name);
        } else if (rep.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{id, p00}, name);
        } else if (rep.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(name)));
            theEvent.put("id", id);
            theEvent.put("p00", p00);
            env.sendEventAvro(theEvent, name);
        } else if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
            String json = "{\"id\": \"" + id + "\", \"p00\": " + p00 + "}";
            env.eventService().sendEventJson(json, name);
        } else {
            fail();
        }
    }

    public static class MyLocalJsonProvidedS0 implements Serializable {
        public String id;
        public int p00;
    }

    public static class MyLocalJsonProvidedS1 implements Serializable {
        public String id;
        public int p00;
    }

    public static class MyLocalJsonProvidedWFields implements Serializable {
        public String s0id;
        public String s1id;
        public int s0p00;
        public int s1p00;
    }

    public static class MyLocalJsonProvidedWildcard implements Serializable {
        public MyLocalJsonProvidedS0 s0;
        public MyLocalJsonProvidedS1 s1;
    }
}
