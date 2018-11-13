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
package com.espertech.esper.regressionlib.suite.expr.datetime;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportTimeStartEndA;
import com.espertech.esper.regressionlib.support.bean.SupportTimeStartEndB;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExprDTPerfIntervalOps implements RegressionExecution {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('create') create window AWindow#keepall as SupportTimeStartEndA", path);
        env.compileDeploy("insert into AWindow select * from SupportTimeStartEndA", path);

        EventType eventTypeNW = env.statement("create").getEventType();
        assertEquals("longdateStart", eventTypeNW.getStartTimestampPropertyName());
        assertEquals("longdateEnd", eventTypeNW.getEndTimestampPropertyName());

        // preload
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(SupportTimeStartEndA.make("A" + i, "2002-05-30T09:00:00.000", 100));
        }
        env.sendEventBean(SupportTimeStartEndA.make("AEarlier", "2002-05-30T08:00:00.000", 100));
        env.sendEventBean(SupportTimeStartEndA.make("ALater", "2002-05-30T10:00:00.000", 100));

        // assert BEFORE
        String eplBefore = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.before(b)";
        runAssertion(env, path, eplBefore, "2002-05-30T09:00:00.000", 0, "AEarlier");

        String eplBeforeMSec = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.longdateEnd.before(b.longdateStart)";
        runAssertion(env, path, eplBeforeMSec, "2002-05-30T09:00:00.000", 0, "AEarlier");

        String eplBeforeMSecMix1 = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.longdateEnd.before(b)";
        runAssertion(env, path, eplBeforeMSecMix1, "2002-05-30T09:00:00.000", 0, "AEarlier");

        String eplBeforeMSecMix2 = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.before(b.longdateStart)";
        runAssertion(env, path, eplBeforeMSecMix2, "2002-05-30T09:00:00.000", 0, "AEarlier");

        // assert AFTER
        String eplAfter = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.after(b)";
        runAssertion(env, path, eplAfter, "2002-05-30T09:00:00.000", 0, "ALater");

        // assert COINCIDES
        String eplCoincides = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.coincides(b)";
        runAssertion(env, path, eplCoincides, "2002-05-30T08:00:00.000", 100, "AEarlier");

        // assert DURING
        String eplDuring = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.during(b)";
        runAssertion(env, path, eplDuring, "2002-05-30T07:59:59.000", 2000, "AEarlier");

        // assert FINISHES
        String eplFinishes = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.finishes(b)";
        runAssertion(env, path, eplFinishes, "2002-05-30T07:59:59.950", 150, "AEarlier");

        // assert FINISHED-BY
        String eplFinishedBy = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.finishedBy(b)";
        runAssertion(env, path, eplFinishedBy, "2002-05-30T08:00:00.050", 50, "AEarlier");

        // assert INCLUDES
        String eplIncludes = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.includes(b)";
        runAssertion(env, path, eplIncludes, "2002-05-30T08:00:00.050", 20, "AEarlier");

        // assert MEETS
        String eplMeets = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.meets(b)";
        runAssertion(env, path, eplMeets, "2002-05-30T08:00:00.100", 0, "AEarlier");

        // assert METBY
        String eplMetBy = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.metBy(b)";
        runAssertion(env, path, eplMetBy, "2002-05-30T07:59:59.950", 50, "AEarlier");

        // assert OVERLAPS
        String eplOverlaps = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.overlaps(b)";
        runAssertion(env, path, eplOverlaps, "2002-05-30T08:00:00.050", 100, "AEarlier");

        // assert OVERLAPPEDY
        String eplOverlappedBy = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.overlappedBy(b)";
        runAssertion(env, path, eplOverlappedBy, "2002-05-30T09:59:59.950", 100, "ALater");
        runAssertion(env, path, eplOverlappedBy, "2002-05-30T07:59:59.950", 100, "AEarlier");

        // assert STARTS
        String eplStarts = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.starts(b)";
        runAssertion(env, path, eplStarts, "2002-05-30T08:00:00.000", 150, "AEarlier");

        // assert STARTEDBY
        String eplEnds = "select a.key as c0 from AWindow as a, SupportTimeStartEndB b unidirectional where a.startedBy(b)";
        runAssertion(env, path, eplEnds, "2002-05-30T08:00:00.000", 50, "AEarlier");

        env.undeployAll();
    }

    private void runAssertion(RegressionEnvironment env, RegressionPath path, String epl, String timestampB, long durationB, String expectedAKey) {

        env.compileDeploy("@name('s0') " + epl, path).addListener("s0");

        // query
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            env.sendEventBean(SupportTimeStartEndB.make("B", timestampB, durationB));
            assertEquals(expectedAKey, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("Delta=" + delta / 1000d, delta < 500);

        env.undeployModuleContaining("s0");
    }
}
