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
package com.espertech.esper.regression.expr.datetime;

import com.espertech.esper.client.ConfigurationEventTypeLegacy;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportTimeStartEndA;
import com.espertech.esper.supportregression.bean.SupportTimeStartEndB;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecDTPerfIntervalOps implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationEventTypeLegacy config = new ConfigurationEventTypeLegacy();
        config.setStartTimestampPropertyName("longdateStart");
        config.setEndTimestampPropertyName("longdateEnd");
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportTimeStartEndA.class.getName(), config);
        epService.getEPAdministrator().getConfiguration().addEventType("B", SupportTimeStartEndB.class.getName(), config);

        epService.getEPAdministrator().createEPL("create window AWindow#keepall as A");
        epService.getEPAdministrator().createEPL("insert into AWindow select * from A");

        // preload
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(SupportTimeStartEndA.make("A" + i, "2002-05-30T09:00:00.000", 100));
        }
        epService.getEPRuntime().sendEvent(SupportTimeStartEndA.make("AEarlier", "2002-05-30T08:00:00.000", 100));
        epService.getEPRuntime().sendEvent(SupportTimeStartEndA.make("ALater", "2002-05-30T10:00:00.000", 100));

        // assert BEFORE
        String eplBefore = "select a.key as c0 from AWindow as a, B b unidirectional where a.before(b)";
        runAssertion(epService, eplBefore, "2002-05-30T09:00:00.000", 0, "AEarlier");

        String eplBeforeMSec = "select a.key as c0 from AWindow as a, B b unidirectional where a.longdateEnd.before(b.longdateStart)";
        runAssertion(epService, eplBeforeMSec, "2002-05-30T09:00:00.000", 0, "AEarlier");

        String eplBeforeMSecMix1 = "select a.key as c0 from AWindow as a, B b unidirectional where a.longdateEnd.before(b)";
        runAssertion(epService, eplBeforeMSecMix1, "2002-05-30T09:00:00.000", 0, "AEarlier");

        String eplBeforeMSecMix2 = "select a.key as c0 from AWindow as a, B b unidirectional where a.before(b.longdateStart)";
        runAssertion(epService, eplBeforeMSecMix2, "2002-05-30T09:00:00.000", 0, "AEarlier");

        // assert AFTER
        String eplAfter = "select a.key as c0 from AWindow as a, B b unidirectional where a.after(b)";
        runAssertion(epService, eplAfter, "2002-05-30T09:00:00.000", 0, "ALater");

        // assert COINCIDES
        String eplCoincides = "select a.key as c0 from AWindow as a, B b unidirectional where a.coincides(b)";
        runAssertion(epService, eplCoincides, "2002-05-30T08:00:00.000", 100, "AEarlier");

        // assert DURING
        String eplDuring = "select a.key as c0 from AWindow as a, B b unidirectional where a.during(b)";
        runAssertion(epService, eplDuring, "2002-05-30T07:59:59.000", 2000, "AEarlier");

        // assert FINISHES
        String eplFinishes = "select a.key as c0 from AWindow as a, B b unidirectional where a.finishes(b)";
        runAssertion(epService, eplFinishes, "2002-05-30T07:59:59.950", 150, "AEarlier");

        // assert FINISHED-BY
        String eplFinishedBy = "select a.key as c0 from AWindow as a, B b unidirectional where a.finishedBy(b)";
        runAssertion(epService, eplFinishedBy, "2002-05-30T08:00:00.050", 50, "AEarlier");

        // assert INCLUDES
        String eplIncludes = "select a.key as c0 from AWindow as a, B b unidirectional where a.includes(b)";
        runAssertion(epService, eplIncludes, "2002-05-30T08:00:00.050", 20, "AEarlier");

        // assert MEETS
        String eplMeets = "select a.key as c0 from AWindow as a, B b unidirectional where a.meets(b)";
        runAssertion(epService, eplMeets, "2002-05-30T08:00:00.100", 0, "AEarlier");

        // assert METBY
        String eplMetBy = "select a.key as c0 from AWindow as a, B b unidirectional where a.metBy(b)";
        runAssertion(epService, eplMetBy, "2002-05-30T07:59:59.950", 50, "AEarlier");

        // assert OVERLAPS
        String eplOverlaps = "select a.key as c0 from AWindow as a, B b unidirectional where a.overlaps(b)";
        runAssertion(epService, eplOverlaps, "2002-05-30T08:00:00.050", 100, "AEarlier");

        // assert OVERLAPPEDY
        String eplOverlappedBy = "select a.key as c0 from AWindow as a, B b unidirectional where a.overlappedBy(b)";
        runAssertion(epService, eplOverlappedBy, "2002-05-30T09:59:59.950", 100, "ALater");
        runAssertion(epService, eplOverlappedBy, "2002-05-30T07:59:59.950", 100, "AEarlier");

        // assert STARTS
        String eplStarts = "select a.key as c0 from AWindow as a, B b unidirectional where a.starts(b)";
        runAssertion(epService, eplStarts, "2002-05-30T08:00:00.000", 150, "AEarlier");

        // assert STARTEDBY
        String eplEnds = "select a.key as c0 from AWindow as a, B b unidirectional where a.startedBy(b)";
        runAssertion(epService, eplEnds, "2002-05-30T08:00:00.000", 50, "AEarlier");
    }

    private void runAssertion(EPServiceProvider epService, String epl, String timestampB, long durationB, String expectedAKey) {

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // query
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(SupportTimeStartEndB.make("B", timestampB, durationB));
            assertEquals(expectedAKey, listener.assertOneGetNewAndReset().get("c0"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("Delta=" + delta / 1000d, delta < 500);

        stmt.destroy();
    }
}
