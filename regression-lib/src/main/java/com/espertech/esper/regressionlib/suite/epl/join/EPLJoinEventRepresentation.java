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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import org.apache.avro.generic.GenericData;

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
            AtomicInteger milestone = new AtomicInteger();

            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                String s0Type = "S0_" + rep.getUndName();
                String s1Type = "S1_" + rep.getUndName();
                String eplOne = "select S0.id as S0_id, S1.id as S1_id, S0.p00 as S0_p00, S1.p00 as S1_p00 from " + s0Type + "#keepall as S0, " +
                    s1Type + "#keepall as S1 where S0.id = S1.id";
                tryJoinAssertion(env, eplOne, rep, "S0_id,S1_id,S0_p00,S1_p00", milestone);
            }

            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                String s0Type = "S0_" + rep.getUndName();
                String s1Type = "S1_" + rep.getUndName();
                String eplTwo = "select * from " + s0Type + "#keepall as S0, " + s1Type + "#keepall as S1 where S0.id = S1.id";
                tryJoinAssertion(env, eplTwo, rep, "S0.id,S1.id,S0.p00,S1.p00", milestone);
            }
        }

        private static void tryJoinAssertion(RegressionEnvironment env, String epl, EventRepresentationChoice rep, String columnNames, AtomicInteger milestone) {
            env.compileDeployAddListenerMile("@name('s0')" + rep.getAnnotationText() + epl, "s0", milestone.getAndIncrement());

            String s0Name = "S0_" + rep.getUndName();
            String s1Name = "S1_" + rep.getUndName();

            sendRepEvent(env, rep, s0Name, "a", 1);
            assertFalse(env.listener("s0").isInvoked());

            sendRepEvent(env, rep, s1Name, "a", 2);
            EventBean output = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(output, columnNames.split(","), new Object[]{"a", "a", 1, 2});
            assertTrue(rep.matchesClass(output.getUnderlying().getClass()));

            sendRepEvent(env, rep, s1Name, "b", 3);
            sendRepEvent(env, rep, s0Name, "c", 4);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
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
        } else {
            fail();
        }
    }
}
