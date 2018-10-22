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

import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.bean.SupportBean_C;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class EPLJoinSingleOp3Stream {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinSingleOp3StreamUniquePerId());
        execs.add(new EPLJoinSingleOp3StreamUniquePerIdOM());
        execs.add(new EPLJoinSingleOp3StreamUniquePerIdCompile());
        return execs;
    }

    private static class EPLJoinSingleOp3StreamUniquePerId implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from " +
                "SupportBean_A#length(3) as streamA," +
                "SupportBean_B#length(3) as streamB," +
                "SupportBean_C#length(3) as streamC" +
                " where (streamA.id = streamB.id) " +
                "   and (streamB.id = streamC.id)" +
                "   and (streamA.id = streamC.id)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            runJoinUniquePerId(env);

            env.undeployAll();
        }
    }

    private static class EPLJoinSingleOp3StreamUniquePerIdOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard());
            FromClause fromClause = FromClause.create(
                FilterStream.create("SupportBean_A", "streamA").addView(View.create("length", Expressions.constant(3))),
                FilterStream.create("SupportBean_B", "streamB").addView(View.create("length", Expressions.constant(3))),
                FilterStream.create("SupportBean_C", "streamC").addView(View.create("length", Expressions.constant(3))));
            model.setFromClause(fromClause);
            model.setWhereClause(Expressions.and(
                Expressions.eqProperty("streamA.id", "streamB.id"),
                Expressions.eqProperty("streamB.id", "streamC.id"),
                Expressions.eqProperty("streamA.id", "streamC.id")));
            model = SerializableObjectCopier.copyMayFail(model);

            String epl = "select * from " +
                "SupportBean_A#length(3) as streamA, " +
                "SupportBean_B#length(3) as streamB, " +
                "SupportBean_C#length(3) as streamC " +
                "where streamA.id=streamB.id " +
                "and streamB.id=streamC.id " +
                "and streamA.id=streamC.id";
            Assert.assertEquals(epl, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0").milestone(0);

            runJoinUniquePerId(env);

            env.undeployAll();
        }
    }

    private static class EPLJoinSingleOp3StreamUniquePerIdCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from " +
                "SupportBean_A#length(3) as streamA, " +
                "SupportBean_B#length(3) as streamB, " +
                "SupportBean_C#length(3) as streamC " +
                "where streamA.id=streamB.id " +
                "and streamB.id=streamC.id " +
                "and streamA.id=streamC.id";
            env.eplToModelCompileDeploy(epl).addListener("s0");

            runJoinUniquePerId(env);

            env.undeployAll();
        }
    }

    private static void runJoinUniquePerId(RegressionEnvironment env) {
        SupportBean_A[] eventsA = new SupportBean_A[10];
        SupportBean_B[] eventsB = new SupportBean_B[10];
        SupportBean_C[] eventsC = new SupportBean_C[10];
        for (int i = 0; i < eventsA.length; i++) {
            eventsA[i] = new SupportBean_A(Integer.toString(i));
            eventsB[i] = new SupportBean_B(Integer.toString(i));
            eventsC[i] = new SupportBean_C(Integer.toString(i));
        }

        // Test sending a C event
        sendEvent(env, eventsA[0]);
        sendEvent(env, eventsB[0]);
        assertNull(env.listener("s0").getLastNewData());
        sendEvent(env, eventsC[0]);
        assertEventsReceived(env, eventsA[0], eventsB[0], eventsC[0]);

        // Test sending a B event
        sendEvent(env, new Object[]{eventsA[1], eventsB[2], eventsC[3]});
        sendEvent(env, eventsC[1]);
        assertNull(env.listener("s0").getLastNewData());
        sendEvent(env, eventsB[1]);
        assertEventsReceived(env, eventsA[1], eventsB[1], eventsC[1]);

        // Test sending a C event
        sendEvent(env, new Object[]{eventsA[4], eventsA[5], eventsB[4], eventsB[3]});
        assertNull(env.listener("s0").getLastNewData());
        sendEvent(env, eventsC[4]);
        assertEventsReceived(env, eventsA[4], eventsB[4], eventsC[4]);
    }

    private static void assertEventsReceived(RegressionEnvironment env, SupportBean_A eventA, SupportBean_B eventB, SupportBean_C eventC) {
        SupportListener updateListener = env.listener("s0");
        Assert.assertEquals(1, updateListener.getLastNewData().length);
        assertSame(eventA, updateListener.getLastNewData()[0].get("streamA"));
        assertSame(eventB, updateListener.getLastNewData()[0].get("streamB"));
        assertSame(eventC, updateListener.getLastNewData()[0].get("streamC"));
        updateListener.reset();
    }

    private static void sendEvent(RegressionEnvironment env, Object theEvent) {
        env.sendEventBean(theEvent);
    }

    private static void sendEvent(RegressionEnvironment env, Object[] events) {
        for (int i = 0; i < events.length; i++) {
            env.sendEventBean(events[i]);
        }
    }
}
